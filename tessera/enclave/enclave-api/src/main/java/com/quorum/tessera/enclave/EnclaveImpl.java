package com.quorum.tessera.enclave;

import com.quorum.tessera.encryption.*;
import com.quorum.tessera.nacl.NaclFacade;
import com.quorum.tessera.nacl.Nonce;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;

public class EnclaveImpl implements Enclave {

    private final NaclFacade nacl;

    private final KeyManager keyManager;

    public EnclaveImpl(NaclFacade nacl, KeyManager keyManager) {
        this.nacl = Objects.requireNonNull(nacl);
        this.keyManager = Objects.requireNonNull(keyManager);
    }

    @Override
    public EncodedPayload encryptPayload(final byte[] message,
                                         final PublicKey senderPublicKey,
                                         final List<PublicKey> recipientPublicKeys) {
        final MasterKey masterKey = nacl.createMasterKey();
        final Nonce nonce = nacl.randomNonce();
        final Nonce recipientNonce = nacl.randomNonce();

        final byte[] cipherText = nacl.sealAfterPrecomputation(message, nonce, masterKey);

        final List<byte[]> encryptedMasterKeys = buildRecipientMasterKeys(senderPublicKey, recipientPublicKeys, recipientNonce, masterKey);

        return new EncodedPayload(
            senderPublicKey, cipherText, nonce, encryptedMasterKeys, recipientNonce, recipientPublicKeys
        );
    }

    @Override
    public byte[] createNewRecipientBox(final EncodedPayload payload, final PublicKey publicKey) {

        if (payload.getRecipientKeys().isEmpty() || payload.getRecipientBoxes().isEmpty()) {
            throw new RuntimeException("No key or recipient-box to use");
        }

        final MasterKey master = this.getMasterKey(
            payload.getRecipientKeys().get(0), payload.getSenderKey(),
            payload.getRecipientNonce(), payload.getRecipientBoxes().get(0)
        );

        final List<byte[]> sealedMasterKeyList = this.buildRecipientMasterKeys(
            payload.getSenderKey(), singletonList(publicKey), payload.getRecipientNonce(), master
        );

        return sealedMasterKeyList.get(0);
    }

    @Override
    public EncodedPayload encryptPayload(final RawTransaction rawTransaction,
                                         final List<PublicKey> recipientPublicKeys) {
        final MasterKey masterKey = this.getMasterKey(
            rawTransaction.getFrom(), rawTransaction.getFrom(),
            rawTransaction.getNonce(), rawTransaction.getEncryptedKey()
        );

        final Nonce recipientNonce = nacl.randomNonce();
        final List<byte[]> encryptedMasterKeys
            = buildRecipientMasterKeys(rawTransaction.getFrom(), recipientPublicKeys, recipientNonce, masterKey);

        return new EncodedPayload(
            rawTransaction.getFrom(), rawTransaction.getEncryptedPayload(),
            rawTransaction.getNonce(), encryptedMasterKeys, recipientNonce, recipientPublicKeys
        );
    }

    private List<byte[]> buildRecipientMasterKeys(final PublicKey senderPublicKey,
                                                  final List<PublicKey> recipientPublicKeys,
                                                  final Nonce recipientNonce,
                                                  final MasterKey masterKey){
        final PrivateKey privateKey = keyManager.getPrivateKeyForPublicKey(senderPublicKey);

        return recipientPublicKeys
            .stream()
            .map(publicKey -> nacl.computeSharedKey(publicKey, privateKey))
            .map(sharedKey -> nacl.sealAfterPrecomputation(masterKey.getKeyBytes(), recipientNonce, sharedKey))
            .collect(Collectors.toList());
    }

    @Override
    public RawTransaction encryptRawPayload(byte[] message, PublicKey sender) {
        final MasterKey masterKey = nacl.createMasterKey();
        final Nonce nonce = nacl.randomNonce();

        final byte[] cipherText = nacl.sealAfterPrecomputation(message, nonce, masterKey);

        final PrivateKey privateKey = keyManager.getPrivateKeyForPublicKey(sender);

        // TODO NL - check if it makes sense to compute a shared key from the public and private parts of the same key
        SharedKey sharedKey = nacl.computeSharedKey(sender, privateKey);
        final byte[] encryptedMasterKey = nacl.sealAfterPrecomputation(masterKey.getKeyBytes(), nonce, sharedKey);

        return new RawTransaction(cipherText, encryptedMasterKey, nonce, sender);
    }

    @Override
    public byte[] unencryptTransaction(EncodedPayload payload, final PublicKey providedSenderKey) {

        final PublicKey senderPubKey;

        final PublicKey recipientPubKey;

        if (!this.getPublicKeys().contains(payload.getSenderKey())) {
            // This is a payload originally sent to us by another node
            senderPubKey = providedSenderKey;
            recipientPubKey = payload.getSenderKey();
        } else {
            // This is a payload that originated from us
            senderPubKey = payload.getSenderKey();
            recipientPubKey = payload.getRecipientKeys().get(0);
        }

        final PrivateKey senderPrivKey = keyManager.getPrivateKeyForPublicKey(senderPubKey);

        final SharedKey sharedKey = nacl.computeSharedKey(recipientPubKey, senderPrivKey);

        final byte[] recipientBox = payload.getRecipientBoxes().iterator().next();

        final Nonce recipientNonce = payload.getRecipientNonce();

        final byte[] masterKeyBytes = nacl.openAfterPrecomputation(recipientBox, recipientNonce, sharedKey);

        final MasterKey masterKey = MasterKey.from(masterKeyBytes);

        final byte[] cipherText = payload.getCipherText();
        final Nonce cipherTextNonce = payload.getCipherTextNonce();

        return nacl.openAfterPrecomputation(cipherText, cipherTextNonce, masterKey);

    }

    private MasterKey getMasterKey(PublicKey recipient, PublicKey sender, Nonce nonce, byte[] encryptedKey) {

        final SharedKey sharedKey = nacl.computeSharedKey(recipient, keyManager.getPrivateKeyForPublicKey(sender));

        final byte[] masterKeyBytes = nacl.openAfterPrecomputation(encryptedKey, nonce, sharedKey);

        return MasterKey.from(masterKeyBytes);
    }

    @Override
    public PublicKey defaultPublicKey() {
        return keyManager.defaultPublicKey();
    }

    @Override
    public Set<PublicKey> getForwardingKeys() {
        return keyManager.getForwardingKeys();
    }

    @Override
    public Set<PublicKey> getPublicKeys() {
        return keyManager.getPublicKeys();
    }

    @Override
    public Status status() {
        return Status.STARTED;
    }
}
