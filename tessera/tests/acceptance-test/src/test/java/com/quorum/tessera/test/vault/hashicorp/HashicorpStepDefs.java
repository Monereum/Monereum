package com.quorum.tessera.test.vault.hashicorp;

import com.quorum.tessera.config.Config;
import com.quorum.tessera.config.HashicorpKeyVaultConfig;
import com.quorum.tessera.config.keypairs.HashicorpVaultKeyPair;
import com.quorum.tessera.config.util.JaxbUtil;
import com.quorum.tessera.test.util.ElUtil;
import cucumber.api.java8.En;
import exec.NodeExecManager;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.net.ssl.HttpsURLConnection;
import javax.ws.rs.core.UriBuilder;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static com.quorum.tessera.config.util.EnvironmentVariables.*;
import static org.assertj.core.api.Assertions.assertThat;

public class HashicorpStepDefs implements En {

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    private String vaultToken;

    private String unsealKey;

    private final String secretEngineName = "kv";

    private String approleRoleId;

    private String approleSecretId;

    private Path tempTesseraConfig;

    public HashicorpStepDefs() {
        final AtomicReference<Process> vaultServerProcess = new AtomicReference<>();
        final AtomicReference<Process> tesseraProcess = new AtomicReference<>();

        Before(
                () -> {
                    // only needed when running outside of maven build process
                    //            System.setProperty("application.jar",
                    // "/Users/yourname/jpmc-tessera/tessera-app/target/tessera-app-0.11-SNAPSHOT-app.jar");

                    tempTesseraConfig = null;
                });

        Given(
                "^the vault server has been started with TLS-enabled$",
                () -> {
                    Path vaultDir =
                            Files.createDirectories(
                                    Paths.get("target/temp/" + UUID.randomUUID().toString() + "/vault"));

                    Map<String, Object> params = new HashMap<>();
                    params.put("vaultPath", vaultDir.toString());
                    params.put("vaultCert", getServerTlsCert());
                    params.put("vaultKey", getServerTlsKey());
                    params.put("clientCert", getClientCaTlsCert());

                    Path configFile =
                            ElUtil.createTempFileFromTemplate(
                                    getClass().getResource("/vault/hashicorp-tls-config.hcl"), params);

                    List<String> args = Arrays.asList("vault", "server", "-config=" + configFile.toString());
                    System.out.println(String.join(" ", args));

                    ProcessBuilder vaultServerProcessBuilder = new ProcessBuilder(args);

                    vaultServerProcess.set(vaultServerProcessBuilder.redirectErrorStream(true).start());

                    AtomicBoolean isAddressAlreadyInUse = new AtomicBoolean(false);

                    executorService.submit(
                            () -> {
                                try (BufferedReader reader =
                                        Stream.of(vaultServerProcess.get().getInputStream())
                                                .map(InputStreamReader::new)
                                                .map(BufferedReader::new)
                                                .findAny()
                                                .get()) {

                                    String line;
                                    while ((line = reader.readLine()) != null) {
                                        System.out.println(line);
                                        if (line.matches("^Error.+address already in use")) {
                                            isAddressAlreadyInUse.set(true);
                                        }
                                    }

                                } catch (IOException ex) {
                                    throw new UncheckedIOException(ex);
                                }
                            });

                    // wait so that assertion is not evaluated before output is checked
                    CountDownLatch startUpLatch = new CountDownLatch(1);
                    startUpLatch.await(5, TimeUnit.SECONDS);

                    assertThat(isAddressAlreadyInUse).isFalse();

                    setKeyStoreProperties();

                    // Initialise the vault
                    final URL initUrl =
                            UriBuilder.fromUri("https://localhost:8200").path("v1/sys/init").build().toURL();
                    HttpsURLConnection initUrlConnection = (HttpsURLConnection) initUrl.openConnection();

                    initUrlConnection.setDoOutput(true);
                    initUrlConnection.setRequestMethod("PUT");

                    String initData = "{\"secret_shares\": 1, \"secret_threshold\": 1}";

                    try (OutputStreamWriter writer = new OutputStreamWriter(initUrlConnection.getOutputStream())) {
                        writer.write(initData);
                    }

                    initUrlConnection.connect();
                    assertThat(initUrlConnection.getResponseCode()).isEqualTo(HttpsURLConnection.HTTP_OK);

                    JsonReader initResponseReader = Json.createReader(initUrlConnection.getInputStream());

                    JsonObject initResponse = initResponseReader.readObject();

                    assertThat(initResponse.getString("root_token")).isNotEmpty();
                    vaultToken = initResponse.getString("root_token");

                    assertThat(initResponse.getJsonArray("keys_base64").size()).isEqualTo(1);
                    assertThat(initResponse.getJsonArray("keys_base64").get(0).toString()).isNotEmpty();
                    String quotedUnsealKey = initResponse.getJsonArray("keys_base64").get(0).toString();

                    if ('\"' == quotedUnsealKey.charAt(0)
                            && '\"' == quotedUnsealKey.charAt(quotedUnsealKey.length() - 1)) {
                        unsealKey = quotedUnsealKey.substring(1, quotedUnsealKey.length() - 1);
                    }

                    // Unseal the vault
                    final URL unsealUrl =
                            UriBuilder.fromUri("https://localhost:8200").path("v1/sys/unseal").build().toURL();
                    HttpsURLConnection unsealUrlConnection = (HttpsURLConnection) unsealUrl.openConnection();

                    unsealUrlConnection.setDoOutput(true);
                    unsealUrlConnection.setRequestMethod("PUT");

                    String unsealData = "{\"key\": \"" + unsealKey + "\"}";

                    try (OutputStreamWriter writer = new OutputStreamWriter(unsealUrlConnection.getOutputStream())) {
                        writer.write(unsealData);
                    }

                    unsealUrlConnection.connect();
                    assertThat(unsealUrlConnection.getResponseCode()).isEqualTo(HttpsURLConnection.HTTP_OK);
                });

        Given(
                "the vault is initialised and unsealed",
                () -> {
                    final URL initUrl =
                            UriBuilder.fromUri("https://localhost:8200").path("v1/sys/health").build().toURL();
                    HttpsURLConnection initUrlConnection = (HttpsURLConnection) initUrl.openConnection();
                    initUrlConnection.connect();

                    // See https://www.vaultproject.io/api/system/health.html for info on response codes for this
                    // endpoint
                    assertThat(initUrlConnection.getResponseCode())
                            .as("check vault is initialized")
                            .isNotEqualTo(HttpsURLConnection.HTTP_NOT_IMPLEMENTED);
                    assertThat(initUrlConnection.getResponseCode()).as("check vault is unsealed").isNotEqualTo(503);
                    assertThat(initUrlConnection.getResponseCode()).isEqualTo(HttpsURLConnection.HTTP_OK);
                });

        Given(
                "the vault has a v2 kv secret engine",
                () -> {
                    setKeyStoreProperties();

                    // Create new v2 kv secret engine
                    final String mountPath = String.format("v1/sys/mounts/%s", secretEngineName);
                    final URL createSecretEngineUrl =
                            UriBuilder.fromUri("https://localhost:8200").path(mountPath).build().toURL();
                    HttpsURLConnection createSecretEngineUrlConnection =
                            (HttpsURLConnection) createSecretEngineUrl.openConnection();

                    createSecretEngineUrlConnection.setDoOutput(true);
                    createSecretEngineUrlConnection.setRequestMethod("POST");
                    createSecretEngineUrlConnection.setRequestProperty("X-Vault-Token", vaultToken);

                    final String createSecretEngineData = "{\"type\": \"kv\", \"options\": {\"version\": \"2\"}}";

                    try (OutputStreamWriter writer =
                            new OutputStreamWriter(createSecretEngineUrlConnection.getOutputStream())) {
                        writer.write(createSecretEngineData);
                    }

                    createSecretEngineUrlConnection.connect();
                    assertThat(createSecretEngineUrlConnection.getResponseCode())
                            .isEqualTo(HttpsURLConnection.HTTP_NO_CONTENT);
                });

        Given(
                "^the AppRole auth method is enabled at (?:the|a) (default|custom) path$",
                (String approleType) -> {
                    setKeyStoreProperties();

                    String approlePath;

                    if ("default".equals(approleType)) {
                        approlePath = "approle";
                    } else {
                        approlePath = "different-approle";
                    }

                    // Enable approle authentication
                    final URL enableApproleUrl =
                            UriBuilder.fromUri("https://localhost:8200")
                                    .path("v1/sys/auth/" + approlePath)
                                    .build()
                                    .toURL();
                    HttpsURLConnection enableApproleUrlConnection =
                            (HttpsURLConnection) enableApproleUrl.openConnection();

                    enableApproleUrlConnection.setDoOutput(true);
                    enableApproleUrlConnection.setRequestMethod("POST");
                    enableApproleUrlConnection.setRequestProperty("X-Vault-Token", vaultToken);

                    String enableApproleData = "{\"type\": \"approle\"}";

                    try (OutputStreamWriter writer =
                            new OutputStreamWriter(enableApproleUrlConnection.getOutputStream())) {
                        writer.write(enableApproleData);
                    }

                    enableApproleUrlConnection.connect();
                    assertThat(enableApproleUrlConnection.getResponseCode())
                            .isEqualTo(HttpsURLConnection.HTTP_NO_CONTENT);

                    // Create a policy and assign to a new approle
                    final URL createPolicyUrl =
                            UriBuilder.fromUri("https://localhost:8200")
                                    .path("v1/sys/policy/simple-policy")
                                    .build()
                                    .toURL();
                    HttpsURLConnection createPolicyUrlConnection =
                            (HttpsURLConnection) createPolicyUrl.openConnection();

                    createPolicyUrlConnection.setDoOutput(true);
                    createPolicyUrlConnection.setRequestMethod("POST");
                    createPolicyUrlConnection.setRequestProperty("X-Vault-Token", vaultToken);

                    final String createPolicyData =
                            String.format(
                                    "{ \"policy\": \"path \\\"%s/data/tessera*\\\" { capabilities = [\\\"create\\\", \\\"update\\\", \\\"read\\\"]}\" }",
                                    secretEngineName);

                    try (OutputStreamWriter writer =
                            new OutputStreamWriter(createPolicyUrlConnection.getOutputStream())) {
                        writer.write(createPolicyData);
                    }

                    createPolicyUrlConnection.connect();
                    assertThat(createPolicyUrlConnection.getResponseCode())
                            .isEqualTo(HttpsURLConnection.HTTP_NO_CONTENT);

                    final URL createApproleUrl =
                            UriBuilder.fromUri("https://localhost:8200")
                                    .path("v1/auth/" + approlePath + "/role/simple-role")
                                    .build()
                                    .toURL();
                    HttpsURLConnection createApproleUrlConnection =
                            (HttpsURLConnection) createApproleUrl.openConnection();

                    createApproleUrlConnection.setDoOutput(true);
                    createApproleUrlConnection.setRequestMethod("POST");
                    createApproleUrlConnection.setRequestProperty("X-Vault-Token", vaultToken);

                    final String createApproleData = "{ \"policies\": [\"simple-policy\"] }";

                    try (OutputStreamWriter writer =
                            new OutputStreamWriter(createApproleUrlConnection.getOutputStream())) {
                        writer.write(createApproleData);
                    }

                    createApproleUrlConnection.connect();
                    assertThat(createApproleUrlConnection.getResponseCode())
                            .isEqualTo(HttpsURLConnection.HTTP_NO_CONTENT);

                    // Retrieve approle credentials
                    final URL getRoleIdUrl =
                            UriBuilder.fromUri("https://localhost:8200")
                                    .path("v1/auth/" + approlePath + "/role/simple-role/role-id")
                                    .build()
                                    .toURL();
                    HttpsURLConnection getRoleIdUrlConnection = (HttpsURLConnection) getRoleIdUrl.openConnection();

                    getRoleIdUrlConnection.setRequestProperty("X-Vault-Token", vaultToken);

                    getRoleIdUrlConnection.connect();
                    assertThat(getRoleIdUrlConnection.getResponseCode()).isEqualTo(HttpsURLConnection.HTTP_OK);

                    JsonReader jsonReader = Json.createReader(getRoleIdUrlConnection.getInputStream());
                    JsonObject getRoleIdObject = jsonReader.readObject().getJsonObject("data");

                    assertThat(getRoleIdObject.getString("role_id")).isNotEmpty();
                    approleRoleId = getRoleIdObject.getString("role_id");

                    final URL createSecretIdUrl =
                            UriBuilder.fromUri("https://localhost:8200")
                                    .path("v1/auth/" + approlePath + "/role/simple-role/secret-id")
                                    .build()
                                    .toURL();
                    HttpsURLConnection createSecretIdUrlConnection =
                            (HttpsURLConnection) createSecretIdUrl.openConnection();

                    createSecretIdUrlConnection.setRequestMethod("POST");
                    createSecretIdUrlConnection.setRequestProperty("X-Vault-Token", vaultToken);

                    createSecretIdUrlConnection.connect();
                    assertThat(createSecretIdUrlConnection.getResponseCode()).isEqualTo(HttpsURLConnection.HTTP_OK);

                    JsonReader anotherJsonReader = Json.createReader(createSecretIdUrlConnection.getInputStream());
                    JsonObject createSecretIdObject = anotherJsonReader.readObject().getJsonObject("data");

                    assertThat(createSecretIdObject.getString("secret_id")).isNotEmpty();
                    approleSecretId = createSecretIdObject.getString("secret_id");

                    createTempTesseraConfigWithApprole(approlePath);
                });

        Given(
                "the vault contains a key pair",
                () -> {
                    Objects.requireNonNull(vaultToken);

                    setKeyStoreProperties();

                    // Set secret data
                    final String setPath = String.format("v1/%s/data/tessera", secretEngineName);
                    final URL setSecretUrl = UriBuilder.fromUri("https://localhost:8200").path(setPath).build().toURL();
                    HttpsURLConnection setSecretUrlConnection = (HttpsURLConnection) setSecretUrl.openConnection();

                    setSecretUrlConnection.setDoOutput(true);
                    setSecretUrlConnection.setRequestMethod("POST");
                    setSecretUrlConnection.setRequestProperty("X-Vault-Token", vaultToken);

                    String setSecretData =
                            "{\"data\": {\"publicKey\": \"/+UuD63zItL1EbjxkKUljMgG8Z1w0AJ8pNOR4iq2yQc=\", \"privateKey\": \"yAWAJjwPqUtNVlqGjSrBmr1/iIkghuOh1803Yzx9jLM=\"}}";

                    try (OutputStreamWriter writer = new OutputStreamWriter(setSecretUrlConnection.getOutputStream())) {
                        writer.write(setSecretData);
                    }

                    setSecretUrlConnection.connect();
                    assertThat(setSecretUrlConnection.getResponseCode()).isEqualTo(HttpsURLConnection.HTTP_OK);

                    final String getPath = String.format("v1/%s/data/tessera", secretEngineName);
                    final URL getSecretUrl = UriBuilder.fromUri("https://localhost:8200").path(getPath).build().toURL();
                    HttpsURLConnection getSecretUrlConnection = (HttpsURLConnection) getSecretUrl.openConnection();
                    getSecretUrlConnection.setRequestProperty("X-Vault-Token", vaultToken);

                    getSecretUrlConnection.connect();
                    assertThat(getSecretUrlConnection.getResponseCode()).isEqualTo(HttpsURLConnection.HTTP_OK);

                    JsonReader jsonReader = Json.createReader(getSecretUrlConnection.getInputStream());

                    JsonObject getSecretObject = jsonReader.readObject();
                    JsonObject keyDataObject = getSecretObject.getJsonObject("data").getJsonObject("data");
                    assertThat(keyDataObject.getString("publicKey"))
                            .isEqualTo("/+UuD63zItL1EbjxkKUljMgG8Z1w0AJ8pNOR4iq2yQc=");
                    assertThat(keyDataObject.getString("privateKey"))
                            .isEqualTo("yAWAJjwPqUtNVlqGjSrBmr1/iIkghuOh1803Yzx9jLM=");
                });

        Given(
                "^the configfile contains the correct vault configuration(| and custom approle configuration)",
                (String isCustomApprole) -> {
                    createTempTesseraConfig();

                    final Config config = JaxbUtil.unmarshal(Files.newInputStream(tempTesseraConfig), Config.class);

                    HashicorpKeyVaultConfig expectedVaultConfig = new HashicorpKeyVaultConfig();
                    expectedVaultConfig.setUrl("https://localhost:8200");
                    expectedVaultConfig.setTlsKeyStorePath(Paths.get(getClientTlsKeystore()));
                    expectedVaultConfig.setTlsTrustStorePath(Paths.get(getClientTlsTruststore()));

                    if (!isCustomApprole.isEmpty()) {
                        expectedVaultConfig.setApprolePath("different-approle");
                    }

                    assertThat(config.getKeys().getHashicorpKeyVaultConfig())
                            .isEqualToComparingFieldByField(expectedVaultConfig);
                });

        Given(
                "the configfile contains the correct key data",
                () -> {
                    createTempTesseraConfig();

                    final Config config = JaxbUtil.unmarshal(Files.newInputStream(tempTesseraConfig), Config.class);

                    final HashicorpVaultKeyPair expectedKeyData =
                            new HashicorpVaultKeyPair("publicKey", "privateKey", secretEngineName, "tessera", null);

                    assertThat(config.getKeys().getKeyData().size()).isEqualTo(1);
                    assertThat(config.getKeys().getKeyData().get(0)).isEqualToComparingFieldByField(expectedKeyData);
                });

        When(
                "^Tessera is started with the following CLI args and (token|approle) environment variables*$",
                (String authMethod, String cliArgs) -> {
                    final String jarfile = System.getProperty("application.jar");

                    final URL logbackConfigFile = NodeExecManager.class.getResource("/logback-node.xml");
                    Path pid = Paths.get(System.getProperty("java.io.tmpdir"), "pidA.pid");

                    String formattedArgs =
                            String.format(cliArgs, tempTesseraConfig.toString(), pid.toAbsolutePath().toString());

                    List<String> args = new ArrayList<>();
                    args.addAll(
                            Arrays.asList(
                                    "java",
                                    "-Dspring.profiles.active=disable-unixsocket",
                                    "-Dlogback.configurationFile=" + logbackConfigFile.getFile(),
                                    "-Ddebug=true",
                                    "-jar",
                                    jarfile));
                    args.addAll(Arrays.asList(formattedArgs.split(" ")));
                    System.out.println(String.join(" ", args));

                    ProcessBuilder tesseraProcessBuilder = new ProcessBuilder(args);

                    Map<String, String> tesseraEnvironment = tesseraProcessBuilder.environment();
                    tesseraEnvironment.put(HASHICORP_CLIENT_KEYSTORE_PWD, "testtest");
                    tesseraEnvironment.put(HASHICORP_CLIENT_TRUSTSTORE_PWD, "testtest");

                    if ("token".equals(authMethod)) {

                        Objects.requireNonNull(vaultToken);
                        tesseraEnvironment.put(HASHICORP_TOKEN, vaultToken);

                    } else {

                        Objects.requireNonNull(approleRoleId);
                        Objects.requireNonNull(approleSecretId);
                        tesseraEnvironment.put(HASHICORP_ROLE_ID, approleRoleId);
                        tesseraEnvironment.put(HASHICORP_SECRET_ID, approleSecretId);
                    }

                    try {
                        tesseraProcess.set(tesseraProcessBuilder.redirectErrorStream(true).start());
                    } catch (NullPointerException ex) {
                        throw new NullPointerException("Check that application.jar property has been set");
                    }

                    executorService.submit(
                            () -> {
                                try (BufferedReader reader =
                                        Stream.of(tesseraProcess.get().getInputStream())
                                                .map(InputStreamReader::new)
                                                .map(BufferedReader::new)
                                                .findAny()
                                                .get()) {

                                    String line;
                                    while ((line = reader.readLine()) != null) {
                                        System.out.println(line);
                                    }

                                } catch (IOException ex) {
                                    throw new UncheckedIOException(ex);
                                }
                            });

                    final Config config = JaxbUtil.unmarshal(Files.newInputStream(tempTesseraConfig), Config.class);

                    final URL bindingUrl =
                            UriBuilder.fromUri(config.getP2PServerConfig().getBindingUri())
                                    .path("upcheck")
                                    .build()
                                    .toURL();

                    CountDownLatch startUpLatch = new CountDownLatch(1);

                    executorService.submit(
                            () -> {
                                while (true) {
                                    try {
                                        HttpURLConnection conn = (HttpURLConnection) bindingUrl.openConnection();
                                        conn.connect();

                                        System.out.println(bindingUrl + " started." + conn.getResponseCode());

                                        startUpLatch.countDown();
                                        return;
                                    } catch (IOException ex) {
                                        try {
                                            TimeUnit.MILLISECONDS.sleep(200L);
                                        } catch (InterruptedException ex1) {
                                        }
                                    }
                                }
                            });

                    boolean started = startUpLatch.await(30, TimeUnit.SECONDS);

                    if (!started) {
                        System.err.println(bindingUrl + " Not started. ");
                    }

                    executorService.submit(
                            () -> {
                                try {
                                    int exitCode = tesseraProcess.get().waitFor();
                                    if (0 != exitCode) {
                                        System.err.println("Tessera node exited with code " + exitCode);
                                    }
                                } catch (InterruptedException ex) {
                                    ex.printStackTrace();
                                }
                            });

                    startUpLatch.await(30, TimeUnit.SECONDS);
                });

        When(
                "^Tessera keygen is run with the following CLI args and (token|approle) environment variables*$",
                (String authMethod, String cliArgs) -> {
                    final String jarfile = System.getProperty("application.jar");
                    final URL logbackConfigFile = NodeExecManager.class.getResource("/logback-node.xml");

                    String formattedArgs = String.format(cliArgs, getClientTlsKeystore(), getClientTlsTruststore());

                    List<String> args = new ArrayList<>();
                    args.addAll(
                            Arrays.asList(
                                    "java",
                                    "-Dspring.profiles.active=disable-unixsocket",
                                    "-Dlogback.configurationFile=" + logbackConfigFile.getFile(),
                                    "-Ddebug=true",
                                    "-jar",
                                    jarfile));
                    args.addAll(Arrays.asList(formattedArgs.split(" ")));
                    System.out.println(String.join(" ", args));

                    ProcessBuilder tesseraProcessBuilder = new ProcessBuilder(args);

                    Map<String, String> tesseraEnvironment = tesseraProcessBuilder.environment();
                    tesseraEnvironment.put(HASHICORP_CLIENT_KEYSTORE_PWD, "testtest");
                    tesseraEnvironment.put(HASHICORP_CLIENT_TRUSTSTORE_PWD, "testtest");

                    if ("token".equals(authMethod)) {

                        Objects.requireNonNull(vaultToken);
                        tesseraEnvironment.put(HASHICORP_TOKEN, vaultToken);

                    } else {

                        Objects.requireNonNull(approleRoleId);
                        Objects.requireNonNull(approleSecretId);
                        tesseraEnvironment.put(HASHICORP_ROLE_ID, approleRoleId);
                        tesseraEnvironment.put(HASHICORP_SECRET_ID, approleSecretId);
                    }

                    try {
                        tesseraProcess.set(tesseraProcessBuilder.redirectErrorStream(true).start());
                    } catch (NullPointerException ex) {
                        throw new NullPointerException("Check that application.jar property has been set");
                    }

                    executorService.submit(
                            () -> {
                                try (BufferedReader reader =
                                        Stream.of(tesseraProcess.get().getInputStream())
                                                .map(InputStreamReader::new)
                                                .map(BufferedReader::new)
                                                .findAny()
                                                .get()) {

                                    String line;
                                    while ((line = reader.readLine()) != null) {
                                        System.out.println(line);
                                    }

                                } catch (IOException ex) {
                                    throw new UncheckedIOException(ex);
                                }
                            });

                    CountDownLatch startUpLatch = new CountDownLatch(1);
                    startUpLatch.await(10, TimeUnit.SECONDS);
                });

        Then(
                "Tessera will retrieve the key pair from the vault",
                () -> {
                    final URL partyInfoUrl =
                            UriBuilder.fromUri("http://localhost").port(8080).path("partyinfo").build().toURL();

                    HttpURLConnection partyInfoUrlConnection = (HttpURLConnection) partyInfoUrl.openConnection();
                    partyInfoUrlConnection.connect();

                    int partyInfoResponseCode = partyInfoUrlConnection.getResponseCode();
                    assertThat(partyInfoResponseCode).isEqualTo(HttpURLConnection.HTTP_OK);

                    JsonReader jsonReader = Json.createReader(partyInfoUrlConnection.getInputStream());

                    JsonObject partyInfoObject = jsonReader.readObject();

                    assertThat(partyInfoObject).isNotNull();
                    assertThat(partyInfoObject.getJsonArray("keys")).hasSize(1);
                    assertThat(partyInfoObject.getJsonArray("keys").getJsonObject(0).getString("key"))
                            .isEqualTo("/+UuD63zItL1EbjxkKUljMgG8Z1w0AJ8pNOR4iq2yQc=");
                });

        Then(
                "^a new key pair (.+) will have been added to the vault$",
                (String secretName) -> {
                    Objects.requireNonNull(vaultToken);

                    setKeyStoreProperties();

                    final String getPath = String.format("v1/%s/data/%s", secretEngineName, secretName);
                    final URL getSecretUrl = UriBuilder.fromUri("https://localhost:8200").path(getPath).build().toURL();

                    HttpsURLConnection getSecretUrlConnection = (HttpsURLConnection) getSecretUrl.openConnection();
                    getSecretUrlConnection.setRequestProperty("X-Vault-Token", vaultToken);
                    getSecretUrlConnection.connect();

                    int getSecretResponseCode = getSecretUrlConnection.getResponseCode();
                    assertThat(getSecretResponseCode).isEqualTo(HttpURLConnection.HTTP_OK);

                    JsonReader jsonReader = Json.createReader(getSecretUrlConnection.getInputStream());

                    JsonObject getSecretObject = jsonReader.readObject();
                    JsonObject keyDataObject = getSecretObject.getJsonObject("data").getJsonObject("data");

                    assertThat(keyDataObject.size()).isEqualTo(2);
                    assertThat(keyDataObject.getString("publicKey")).isNotBlank();
                    assertThat(keyDataObject.getString("privateKey")).isNotBlank();
                });

        After(
                () -> {
                    if (vaultServerProcess.get() != null && vaultServerProcess.get().isAlive()) {
                        vaultServerProcess.get().destroy();
                    }

                    if (tesseraProcess.get() != null && tesseraProcess.get().isAlive()) {
                        tesseraProcess.get().destroy();
                    }
                });
    }

    private void setKeyStoreProperties() {
        System.setProperty("javax.net.ssl.keyStoreType", "jks");
        System.setProperty("javax.net.ssl.keyStore", getClientTlsKeystore());
        System.setProperty("javax.net.ssl.keyStorePassword", "testtest");
        System.setProperty("javax.net.ssl.trustStoreType", "jks");
        System.setProperty("javax.net.ssl.trustStore", getClientTlsTruststore());
        System.setProperty("javax.net.ssl.trustStorePassword", "testtest");
    }

    private void createTempTesseraConfig() {
        if (tempTesseraConfig == null) {
            Map<String, Object> params = new HashMap<>();
            params.put("clientKeystore", getClientTlsKeystore());
            params.put("clientTruststore", getClientTlsTruststore());

            tempTesseraConfig =
                    ElUtil.createTempFileFromTemplate(
                            getClass().getResource("/vault/tessera-hashicorp-config.json"), params);
            tempTesseraConfig.toFile().deleteOnExit();
        }
    }

    private void createTempTesseraConfigWithApprole(String approlePath) {
        if (tempTesseraConfig == null) {
            Map<String, Object> params = new HashMap<>();
            params.put("clientKeystore", getClientTlsKeystore());
            params.put("clientTruststore", getClientTlsTruststore());
            params.put("approlePath", approlePath);

            tempTesseraConfig =
                    ElUtil.createTempFileFromTemplate(
                            getClass().getResource("/vault/tessera-hashicorp-approle-config.json"), params);
            tempTesseraConfig.toFile().deleteOnExit();
        }
    }

    private String getServerTlsCert() {
        return getClass().getResource("/certificates/localhost-with-san-chain.pem").getFile();
    }

    private String getServerTlsKey() {
        return getClass().getResource("/certificates/localhost-with-san.key").getFile();
    }

    private String getClientCaTlsCert() {
        return getClass().getResource("/certificates/caRoot.pem").getFile();
    }

    private String getClientTlsKeystore() {
        return getClass().getResource("/certificates/quorum-client-keystore.jks").getFile();
    }

    private String getClientTlsTruststore() {
        return getClass().getResource("/certificates/truststore.jks").getFile();
    }
}
