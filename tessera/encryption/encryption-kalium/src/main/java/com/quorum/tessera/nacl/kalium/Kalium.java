package com.quorum.tessera.nacl.kalium;

import com.quorum.tessera.encryption.PrivateKey;
import com.quorum.tessera.encryption.PublicKey;
import com.quorum.tessera.encryption.SharedKey;
import com.quorum.tessera.encryption.KeyPair;
import com.quorum.tessera.nacl.NaclException;
import com.quorum.tessera.nacl.NaclFacade;
import com.quorum.tessera.nacl.Nonce;
import org.abstractj.kalium.NaCl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Objects;

import static org.abstractj.kalium.NaCl.Sodium.*;

/**
 * An implementation of the {@link NaclFacade} using the Kalium and libsodium binding
 */
public class Kalium implements NaclFacade {

    private static final Logger LOGGER = LoggerFactory.getLogger(Kalium.class);

    private static final String REDACTED = "REDACTED";

    private final NaCl.Sodium sodium;

    public Kalium(final NaCl.Sodium sodium) {
        this.sodium = Objects.requireNonNull(sodium, "Kalium sodium implementation was null");

        LOGGER.info("Initialising Sodium...");
        this.sodium.sodium_init();
        LOGGER.info("Sodium initialised");
    }

    @Override
    public SharedKey computeSharedKey(final PublicKey publicKey, final PrivateKey privateKey) {
        final byte[] output = new byte[CRYPTO_BOX_CURVE25519XSALSA20POLY1305_BEFORENMBYTES];

        LOGGER.info("Computing the shared key for public key {} and private key {}", publicKey, REDACTED);
        LOGGER.debug("Computing the shared key for public key {} and private key {}", publicKey, privateKey);
        final int sodiumResult = this.sodium.crypto_box_curve25519xsalsa20poly1305_beforenm(
            output, publicKey.getKeyBytes(), privateKey.getKeyBytes()
        );

        if (sodiumResult == -1) {
            LOGGER.warn("Could not compute the shared key for pub {} and priv {}", publicKey, REDACTED);
            LOGGER.debug("Could not compute the shared key for pub {} and priv {}", publicKey, privateKey);
            throw new NaclException("Kalium could not compute the shared key");
        }

        final SharedKey sharedKey = SharedKey.from(output);

        LOGGER.info("Computed shared key {} for pub {} and priv {}", sharedKey, publicKey, REDACTED);
        LOGGER.debug("Computed shared key {} for pub {} and priv {}", sharedKey, publicKey, privateKey);

        return sharedKey;
    }

    @Override
    public byte[] seal(final byte[] message, final Nonce nonce, final PublicKey publicKey, final PrivateKey privateKey) {
        /*
         * The Kalium library uses the C API
         * which expects the first CRYPTO_BOX_CURVE25519XSALSA20POLY1305_ZEROBYTES bytes to be zero
         */
        final byte[] paddedMessage = pad(message, CRYPTO_BOX_CURVE25519XSALSA20POLY1305_ZEROBYTES);
        final byte[] output = new byte[paddedMessage.length];

        LOGGER.info("Sealing message using public key {}", publicKey);
        LOGGER.debug(
            "Sealing message {} using nonce {}, public key {} and private key {}",
            Arrays.toString(message), nonce, publicKey, privateKey
        );

        final int sodiumResult = sodium.crypto_box_curve25519xsalsa20poly1305(
            output, paddedMessage, paddedMessage.length, nonce.getNonceBytes(), publicKey.getKeyBytes(), privateKey.getKeyBytes()
        );

        if (sodiumResult == -1) {
            LOGGER.warn("Could not create sealed payload using public key {} and private key {}", publicKey, REDACTED);
            LOGGER.debug("Could not create sealed payload using public key {} and private key {}", publicKey, privateKey);
            throw new NaclException("Kalium could not seal the payload using the provided keys directly");
        }

        LOGGER.info("Created sealed payload for public key {}", publicKey);
        LOGGER.debug(
            "Created sealed payload {} using nonce {}, public key {} and private key {}",
            Arrays.toString(output), nonce, publicKey, privateKey
        );

        /*
         * NaCL C API states that first crypto_secretbox_BOXZEROBYTES must be zero
         * but these are not part of the message, and must be stripped out
         */
        return extract(output, CRYPTO_BOX_CURVE25519XSALSA20POLY1305_BOXZEROBYTES);
    }

    @Override
    public byte[] open(final byte[] cipherText, final Nonce nonce, final PublicKey publicKey, final PrivateKey privateKey) {
        LOGGER.info("Opening message using public key {}", publicKey);
        LOGGER.debug(
            "Opening message {} using nonce {}, public key {} and private key {}",
            Arrays.toString(cipherText), nonce, publicKey, privateKey
        );

        /*
         * NaCL C API states that first crypto_secretbox_BOXZEROBYTES must be zero
         * but these are not part of the ciphertext, and must be added in
         */
        final byte[] paddedInput = pad(cipherText, CRYPTO_BOX_CURVE25519XSALSA20POLY1305_BOXZEROBYTES);
        final byte[] paddedOutput = new byte[paddedInput.length];

        final int sodiumResult = sodium.crypto_box_curve25519xsalsa20poly1305_open(
            paddedOutput, paddedInput, paddedInput.length, nonce.getNonceBytes(), publicKey.getKeyBytes(), privateKey.getKeyBytes()
        );

        if (sodiumResult == -1) {
            LOGGER.warn("Could not open sealed payload using public key {} and private key {}", publicKey, REDACTED);
            LOGGER.debug("Could not opern sealed payload using public key {} and private key {}", publicKey, privateKey);
            throw new NaclException("Kalium could not open the payload using the provided keys directly");
        }

        LOGGER.info("Opened sealed payload for public key {}", publicKey);
        LOGGER.debug(
            "Opened payload {} using nonce {}, public key {} and private key {} to get result {}",
            Arrays.toString(cipherText), nonce, publicKey, privateKey, Arrays.toString(paddedOutput)
        );

        return extract(paddedOutput, CRYPTO_BOX_CURVE25519XSALSA20POLY1305_ZEROBYTES);
    }

    @Override
    public byte[] sealAfterPrecomputation(final byte[] message, final Nonce nonce, final SharedKey sharedKey) {
        /*
         * The Kalium library uses the C API
         * which expects the first CRYPTO_BOX_CURVE25519XSALSA20POLY1305_ZEROBYTES bytes to be zero
         */
        final byte[] paddedMessage = pad(message, CRYPTO_BOX_CURVE25519XSALSA20POLY1305_ZEROBYTES);
        final byte[] output = new byte[paddedMessage.length];

        LOGGER.info("Sealing message using public key {}", sharedKey);
        LOGGER.debug(
            "Sealing message {} using nonce {} and shared key {}", Arrays.toString(message), nonce, sharedKey
        );

        final int sodiumResult = this.sodium.crypto_box_curve25519xsalsa20poly1305_afternm(
            output, paddedMessage, paddedMessage.length, nonce.getNonceBytes(), sharedKey.getKeyBytes()
        );

        if (sodiumResult == -1) {
            LOGGER.warn("Could not create sealed payload using shared key {}", sharedKey);
            LOGGER.debug("Could not create sealed payload using shared key {}", sharedKey);
            throw new NaclException("Kalium could not seal the payload using the shared key");
        }

        LOGGER.info("Created sealed payload for shared key {}", sharedKey);
        LOGGER.debug(
            "Created sealed payload {} using nonce {} and shared key {}", Arrays.toString(output), nonce, sharedKey
        );

        /*
         * NaCL C API states that first crypto_secretbox_BOXZEROBYTES must be zero
         * but these are not part of the message, and must be stripped out
         */
        return extract(output, CRYPTO_BOX_CURVE25519XSALSA20POLY1305_BOXZEROBYTES);
    }

    @Override
    public byte[] openAfterPrecomputation(final byte[] encryptedPayload, final Nonce nonce, final SharedKey sharedKey) {
        LOGGER.info("Opening message using shared key {}", sharedKey);
        LOGGER.debug(
            "Opening message {} using nonce {} and shared key {}", Arrays.toString(encryptedPayload), nonce, sharedKey
        );

        /*
         * NaCL C API states that first crypto_secretbox_BOXZEROBYTES must be zero
         * but these are not part of the ciphertext, and must be added in
         */
        final byte[] paddedInput = pad(encryptedPayload, CRYPTO_BOX_CURVE25519XSALSA20POLY1305_BOXZEROBYTES);
        final byte[] paddedOutput = new byte[paddedInput.length];

        final int sodiumResult = this.sodium.crypto_box_curve25519xsalsa20poly1305_open_afternm(
            paddedOutput, paddedInput, paddedInput.length, nonce.getNonceBytes(), sharedKey.getKeyBytes()
        );

        if (sodiumResult == -1) {
            LOGGER.warn("Could not open sealed payload using shared key {}", sharedKey);
            LOGGER.debug("Could not open sealed payload using shared key {}", sharedKey);
            throw new NaclException("Kalium could not open the payload using the shared key");
        }

        LOGGER.info("Opened sealed payload for shared key {}", sharedKey);
        LOGGER.debug(
            "Opened payload {} using nonce {}, public key {} and private key {} to get result {}",
            Arrays.toString(encryptedPayload), nonce, sharedKey, REDACTED, Arrays.toString(paddedOutput)
        );

        return extract(paddedOutput, CRYPTO_BOX_CURVE25519XSALSA20POLY1305_ZEROBYTES);
    }

    @Override
    public Nonce randomNonce() {

        final byte[] nonceBytes = new byte[CRYPTO_BOX_CURVE25519XSALSA20POLY1305_NONCEBYTES];

        this.sodium.randombytes(nonceBytes, nonceBytes.length);

        final Nonce nonce = new Nonce(nonceBytes);

        LOGGER.debug("Generated random nonce {}", nonce);

        return nonce;
    }

    @Override
    public KeyPair generateNewKeys() {
        final byte[] publicKey = new byte[CRYPTO_BOX_CURVE25519XSALSA20POLY1305_PUBLICKEYBYTES];
        final byte[] privateKey = new byte[CRYPTO_BOX_CURVE25519XSALSA20POLY1305_SECRETKEYBYTES];

        LOGGER.info("Generating new keypair...");

        final int sodiumResult = this.sodium.crypto_box_curve25519xsalsa20poly1305_keypair(publicKey, privateKey);

        if (sodiumResult == -1) {
            LOGGER.warn("Unable to generate a new keypair!");
            throw new NaclException("Kalium could not generate a new public/private keypair");
        }

        final PublicKey pubKey = PublicKey.from(publicKey);
        final PrivateKey privKey = PrivateKey.from(privateKey);

        LOGGER.info("Generated public key {} and private key {}", pubKey, REDACTED);
        LOGGER.debug("Generated public key {} and private key {}", pubKey, privKey);

        return new KeyPair(pubKey, privKey);
    }

    /**
     * Left-pads a given message with padSize amount of zeros
     *
     * @param input   the message to be padded
     * @param padSize the amount of left-padding to apply
     * @return the padded message
     */
    private byte[] pad(final byte[] input, final int padSize) {
        final byte[] paddedMessage = new byte[padSize + input.length];
        System.arraycopy(input, 0, paddedMessage, padSize, input.length);

        return paddedMessage;
    }

    /**
     * Removes left-padding from a given message to tune of padSize
     *
     * @param input   The message from which to remove left-padding
     * @param padSize The amount of left-padding to remove
     * @return The trimmed message
     */
    private byte[] extract(final byte[] input, final int padSize) {
        final byte[] extractedMessage = new byte[input.length - padSize];
        System.arraycopy(input, padSize, extractedMessage, 0, extractedMessage.length);

        return extractedMessage;
    }

    @Override
    public SharedKey createSingleKey() {
        LOGGER.info("Generating random key");

        final byte[] keyBytes = new byte[CRYPTO_BOX_CURVE25519XSALSA20POLY1305_PUBLICKEYBYTES];

        this.sodium.randombytes(keyBytes, keyBytes.length);

        final SharedKey key = SharedKey.from(keyBytes);

        LOGGER.info("Random key generated");
        LOGGER.debug("Generated key with value {}", key);

        return key;
    }

}
