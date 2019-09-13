package com.quorum.tessera.key.vault.azure;

import org.junit.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class AzureKeyVaultClientFactoryTest {

    private AzureKeyVaultClientFactory keyVaultClientFactory;

    @Test
    public void injectedCredentialsUsedToGetClient() {
        AzureKeyVaultClientCredentials clientCredentials = mock(AzureKeyVaultClientCredentials.class);

        keyVaultClientFactory = new AzureKeyVaultClientFactory(clientCredentials);
        keyVaultClientFactory.getAuthenticatedClient();

        verify(clientCredentials).applyCredentialsFilter(any());
    }
}
