package com.quorum.tessera.nacl;

import com.quorum.tessera.encryption.KeyPair;
import com.quorum.tessera.encryption.MasterKey;
import com.quorum.tessera.encryption.PrivateKey;
import com.quorum.tessera.encryption.PublicKey;
import com.quorum.tessera.encryption.SharedKey;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.mock;

public class NaclFacadeTest {

    private NaclFacade naclFacade;

    @Before
    public void onSetUp() {
        naclFacade = new MockNaclFacade();
    }

    @Test
    public void sealAfterPrecomputationWithMasterKey() {
        byte[] message = "MESSAGE".getBytes();
        Nonce nonce = mock(Nonce.class);
        MasterKey masterKey = MasterKey.from("".getBytes());

        byte[] outcome = "sealAfterPrecomputationWithSharedKey".getBytes();

        byte[] result = naclFacade.sealAfterPrecomputation(message, nonce, masterKey);

        assertThat(result).isEqualTo(outcome);
    }

    @Test
    public void openAfterPrecomputationWithMasterKey() {
        byte[] message = "MESSAGE".getBytes();
        Nonce nonce = mock(Nonce.class);
        MasterKey masterKey = MasterKey.from("".getBytes());

        byte[] outcome = "openAfterPrecomputationWithSharedKey".getBytes();

        byte[] result = naclFacade.openAfterPrecomputation(message, nonce, masterKey);

        assertThat(result).isEqualTo(outcome);
    }

    @Test
    public void createMasterKey() {

        MasterKey result = naclFacade.createMasterKey();
        assertThat(result.getKeyBytes()).isEqualTo("createSingleKey".getBytes());
    }

    static class MockNaclFacade implements NaclFacade {

        @Override
        public SharedKey computeSharedKey(PublicKey publicKey, PrivateKey privateKey) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public byte[] seal(byte[] message, Nonce nonce, PublicKey publicKey, PrivateKey privateKey) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public byte[] open(byte[] cipherText, Nonce nonce, PublicKey publicKey, PrivateKey privateKey) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public byte[] sealAfterPrecomputation(byte[] message, Nonce nonce, SharedKey sharedKey) {
            return "sealAfterPrecomputationWithSharedKey".getBytes();
        }

        @Override
        public byte[] openAfterPrecomputation(byte[] cipherText, Nonce nonce, SharedKey sharedKey) {
            return "openAfterPrecomputationWithSharedKey".getBytes();
        }

        @Override
        public Nonce randomNonce() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public KeyPair generateNewKeys() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public SharedKey createSingleKey() {
            return SharedKey.from("createSingleKey".getBytes());
        }

    }

}
