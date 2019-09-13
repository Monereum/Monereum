package exec;

import com.quorum.tessera.config.AppType;
import com.quorum.tessera.config.CommunicationType;
import com.quorum.tessera.config.Config;
import com.quorum.tessera.config.KeyConfiguration;
import com.quorum.tessera.config.ServerConfig;
import com.quorum.tessera.config.keypairs.DirectKeyPair;
import com.quorum.tessera.config.util.JaxbUtil;
import config.ConfigDescriptor;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import suite.ExecutionContext;
import suite.ServerStatusCheck;
import suite.ServerStatusCheckExecutor;

public class EnclaveExecManager implements ExecManager {

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    private static final Logger LOGGER = LoggerFactory.getLogger(EnclaveExecManager.class);

    private final ConfigDescriptor configDescriptor;

    private final Path pid;

    private final String nodeId;

    public EnclaveExecManager(ConfigDescriptor configDescriptor) {
        this.configDescriptor = configDescriptor;
        this.pid = Paths.get(System.getProperty("java.io.tmpdir"), "enclave" + configDescriptor.getAlias().name() + ".pid");
        this.nodeId = suite.NodeId.generate(ExecutionContext.currentContext(), configDescriptor.getAlias());
    }

    private final URL logbackConfigFile = EnclaveExecManager.class.getResource("/logback-enclave.xml");

    @Override
    public Process doStart() throws Exception {

        Path enclaveServerJar = Paths.get(System.getProperty("enclave.jaxrs.server.jar", "../../enclave/enclave-jaxrs/target/enclave-jaxrs-0.9-SNAPSHOT-server.jar"));

        ServerConfig serverConfig = configDescriptor.getEnclaveConfig().get().getServerConfigs().get(0);

        List<String> cmd = new ExecArgsBuilder()
                .withPidFile(pid)
                .withJvmArg("-Dnode.number=" + nodeId)
                .withJvmArg("-Dlogback.configurationFile=" + logbackConfigFile)
                .withExecutableJarFile(enclaveServerJar)
                .withConfigFile(configDescriptor.getEnclavePath())
                .build();

        LOGGER.info("Starting enclave {}", configDescriptor.getAlias());

        Process process = ExecUtils.start(cmd,executorService);

        ServerStatusCheckExecutor serverStatusCheckExecutor = new ServerStatusCheckExecutor(ServerStatusCheck.create(serverConfig));

        Future<Boolean> future = executorService.submit(serverStatusCheckExecutor);

        Boolean result = future.get(3, TimeUnit.MINUTES);

        if (!result) {
            throw new IllegalStateException("Enclave server not started");
        }

        LOGGER.info("Started enclave {}", configDescriptor.getAlias());

        return process;
    }

    @Override
    public void doStop() throws Exception {

        String p = Files.lines(pid).findFirst().orElse(null);
        if (p == null) {
            return;
        }
        LOGGER.info("Stopping Enclave : {}, Pid: {}", nodeId, p);
        try {
            ExecUtils.kill(p);
        } finally {
            executorService.shutdown();
        }

    }

    public static void main(String[] args) throws Exception {

        System.setProperty("javax.xml.bind.JAXBContextFactory", "org.eclipse.persistence.jaxb.JAXBContextFactory");
        System.setProperty("javax.xml.bind.context.factory", "org.eclipse.persistence.jaxb.JAXBContextFactory");

        final Config enclaveConfig = new Config();

        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setApp(AppType.ENCLAVE);
        serverConfig.setBindingAddress("http://0.0.0.0:8989");
        serverConfig.setServerAddress("http://localhost:8989");
        serverConfig.setCommunicationType(CommunicationType.REST);

        enclaveConfig.setServerConfigs(Arrays.asList(serverConfig));

        enclaveConfig.setKeys(new KeyConfiguration());
        enclaveConfig.getKeys().setKeyData(Arrays.asList(new DirectKeyPair("/+UuD63zItL1EbjxkKUljMgG8Z1w0AJ8pNOR4iq2yQc=", "yAWAJjwPqUtNVlqGjSrBmr1/iIkghuOh1803Yzx9jLM=")));

        Path configFile = Paths.get("target", UUID.randomUUID().toString() + ".json");

        try (OutputStream out = Files.newOutputStream(configFile)){
            JaxbUtil.marshalWithNoValidation(enclaveConfig, out);
        }

        //EnclaveExecManager enclaveExecManager = new EnclaveExecManager(configFile);
        // enclaveExecManager.start(serverConfig);
    }

}
