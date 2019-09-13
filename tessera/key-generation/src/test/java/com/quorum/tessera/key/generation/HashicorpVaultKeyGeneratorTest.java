package com.quorum.tessera.key.generation;

import com.quorum.tessera.config.keypairs.HashicorpVaultKeyPair;
import com.quorum.tessera.config.vault.data.HashicorpSetSecretData;
import com.quorum.tessera.encryption.KeyPair;
import com.quorum.tessera.encryption.PrivateKey;
import com.quorum.tessera.encryption.PublicKey;
import com.quorum.tessera.key.vault.KeyVaultService;
import com.quorum.tessera.nacl.NaclFacade;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class HashicorpVaultKeyGeneratorTest {

    private final String pubStr = "public";
    private final String privStr = "private";
    private final PublicKey pub = PublicKey.from(pubStr.getBytes());
    private final PrivateKey priv = PrivateKey.from(privStr.getBytes());

    private NaclFacade naclFacade;
    private KeyVaultService keyVaultService;
    private HashicorpVaultKeyGenerator hashicorpVaultKeyGenerator;

    @Before
    public void setUp() {
        this.naclFacade = mock(NaclFacade.class);
        this.keyVaultService = mock(KeyVaultService.class);

        final KeyPair keyPair = new KeyPair(pub, priv);
        when(naclFacade.generateNewKeys()).thenReturn(keyPair);

        this.hashicorpVaultKeyGenerator = new HashicorpVaultKeyGenerator(naclFacade, keyVaultService);

    }

    @Test(expected = NullPointerException.class)
    public void nullFilenameThrowsException() {
        KeyVaultOptions keyVaultOptions = mock(KeyVaultOptions.class);
        when(keyVaultOptions.getSecretEngineName()).thenReturn("secretEngine");

        hashicorpVaultKeyGenerator.generate(null, null, keyVaultOptions);
    }

    @Test(expected = NullPointerException.class)
    public void nullKeyVaultOptionsThrowsException() {
        hashicorpVaultKeyGenerator.generate("filename", null, null);
    }

    @Test(expected = NullPointerException.class)
    public void nullSecretEngineNameThrowsException() {
        KeyVaultOptions keyVaultOptions = mock(KeyVaultOptions.class);
        when(keyVaultOptions.getSecretEngineName()).thenReturn(null);

        hashicorpVaultKeyGenerator.generate("filename", null, keyVaultOptions);
    }

    @Test
    public void generatedKeyPairIsSavedToSpecifiedPathInVaultWithIds() {
        String secretEngine = "secretEngine";
        String filename = "secretName";

        KeyVaultOptions keyVaultOptions = mock(KeyVaultOptions.class);
        when(keyVaultOptions.getSecretEngineName()).thenReturn(secretEngine);

        HashicorpVaultKeyPair result = hashicorpVaultKeyGenerator.generate(filename, null, keyVaultOptions);

        HashicorpVaultKeyPair expected = new HashicorpVaultKeyPair("publicKey", "privateKey", secretEngine, filename, null);
        assertThat(result).isEqualToComparingFieldByField(expected);

        final ArgumentCaptor<HashicorpSetSecretData> captor = ArgumentCaptor.forClass(HashicorpSetSecretData.class);
        verify(keyVaultService).setSecret(captor.capture());

        assertThat(captor.getAllValues()).hasSize(1);
        HashicorpSetSecretData capturedArg = captor.getValue();

        Map<String, Object> expectedNameValuePairs = new HashMap<>();
        expectedNameValuePairs.put("publicKey", pub.encodeToBase64());
        expectedNameValuePairs.put("privateKey", priv.encodeToBase64());

        HashicorpSetSecretData expectedData = new HashicorpSetSecretData(secretEngine, filename, expectedNameValuePairs);

        assertThat(capturedArg).isEqualToComparingFieldByFieldRecursively(expectedData);

        verifyNoMoreInteractions(keyVaultService);

    }

}
