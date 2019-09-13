package com.quorum.tessera.key.vault.hashicorp;

import com.quorum.tessera.config.*;
import com.quorum.tessera.config.util.EnvironmentVariableProvider;
import com.quorum.tessera.key.vault.KeyVaultService;
import com.quorum.tessera.key.vault.KeyVaultServiceFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.SessionManager;
import org.springframework.vault.authentication.SimpleSessionManager;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.ClientOptions;
import org.springframework.vault.support.SslConfiguration;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.Optional;

import static com.quorum.tessera.config.util.EnvironmentVariables.*;

public class HashicorpKeyVaultServiceFactory implements KeyVaultServiceFactory {

    @Override
    public KeyVaultService create(Config config, EnvironmentVariableProvider envProvider) {
        Objects.requireNonNull(config);
        Objects.requireNonNull(envProvider);

        HashicorpKeyVaultServiceFactoryUtil util = new HashicorpKeyVaultServiceFactoryUtil();

        return this.create(config, envProvider, util);
    }

    //This method should not be called directly. It has been left package-private to enable injection of util during testing
    KeyVaultService create(Config config, EnvironmentVariableProvider envProvider, HashicorpKeyVaultServiceFactoryUtil util) {
        Objects.requireNonNull(config);
        Objects.requireNonNull(envProvider);
        Objects.requireNonNull(util);

        final String roleId = envProvider.getEnv(HASHICORP_ROLE_ID);
        final String secretId = envProvider.getEnv(HASHICORP_SECRET_ID);
        final String authToken = envProvider.getEnv(HASHICORP_TOKEN);

        if(roleId == null && secretId == null && authToken == null) {
            throw new HashicorpCredentialNotSetException("Environment variables must be set to authenticate with Hashicorp Vault.  Set the " + HASHICORP_ROLE_ID + " and " + HASHICORP_SECRET_ID + " environment variables if using the AppRole authentication method.  Set the " + HASHICORP_TOKEN + " environment variable if using another authentication method.");
        }
        else if(isOnlyOneInputNull(roleId, secretId)) {
            throw new HashicorpCredentialNotSetException("Only one of the " + HASHICORP_ROLE_ID + " and " + HASHICORP_SECRET_ID + " environment variables to authenticate with Hashicorp Vault using the AppRole method has been set");
        }

        HashicorpKeyVaultConfig keyVaultConfig = Optional.ofNullable(config.getKeys())
            .map(KeyConfiguration::getHashicorpKeyVaultConfig)
            .orElseThrow(() -> new ConfigException(new RuntimeException("Trying to create Hashicorp Vault connection but no Vault configuration provided")));

        VaultEndpoint vaultEndpoint;

        try {
            vaultEndpoint = VaultEndpoint.from(new URI(keyVaultConfig.getUrl()));
        } catch (URISyntaxException | IllegalArgumentException e) {
            throw new ConfigException(new RuntimeException("Provided Hashicorp Vault url is incorrectly formatted", e));
        }

        SslConfiguration sslConfiguration = util.configureSsl(keyVaultConfig, envProvider);

        ClientOptions clientOptions = new ClientOptions();

        ClientHttpRequestFactory clientHttpRequestFactory = util.createClientHttpRequestFactory(clientOptions, sslConfiguration);

        ClientAuthentication clientAuthentication = util.configureClientAuthentication(keyVaultConfig, envProvider, clientHttpRequestFactory, vaultEndpoint);

        SessionManager sessionManager = new SimpleSessionManager(clientAuthentication);
        VaultOperations vaultOperations = new VaultTemplate(vaultEndpoint, clientHttpRequestFactory, sessionManager);

        return new HashicorpKeyVaultService(
            new KeyValueOperationsDelegateFactory(vaultOperations)
        );
    }

    @Override
    public KeyVaultType getType() {
        return KeyVaultType.HASHICORP;
    }

    private boolean isOnlyOneInputNull(Object obj1, Object obj2) {
        return Objects.isNull(obj1) ^ Objects.isNull(obj2);
    }
}
