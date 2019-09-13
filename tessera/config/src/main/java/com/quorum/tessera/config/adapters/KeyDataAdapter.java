package com.quorum.tessera.config.adapters;

import com.quorum.tessera.config.KeyData;
import com.quorum.tessera.config.keypairs.*;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.Objects;

public class KeyDataAdapter extends XmlAdapter<KeyData, ConfigKeyPair> {

    public static final String NACL_FAILURE_TOKEN = "NACL_FAILURE";

    @Override
    public ConfigKeyPair unmarshal(final KeyData keyData) {

        //case 1, the keys are provided inline
        if (Objects.nonNull(keyData.getPrivateKey()) && Objects.nonNull(keyData.getPublicKey())) {
            return new DirectKeyPair(keyData.getPublicKey(), keyData.getPrivateKey());
        }

        //case 2, the config is provided inline
        if (keyData.getPublicKey() != null && keyData.getConfig() != null) {
            return new InlineKeypair(keyData.getPublicKey(), keyData.getConfig());
        }

        //case 3, the Azure Key Vault data is provided
        if(keyData.getAzureVaultPublicKeyId() != null && keyData.getAzureVaultPrivateKeyId() != null) {
            return new AzureVaultKeyPair(keyData.getAzureVaultPublicKeyId(), keyData.getAzureVaultPrivateKeyId(), keyData.getAzureVaultPublicKeyVersion(), keyData.getAzureVaultPrivateKeyVersion());
        }

        //case 4, the Hashicorp Vault data is provided
        if(keyData.getHashicorpVaultPublicKeyId() != null && keyData.getHashicorpVaultPrivateKeyId() != null
            && keyData.getHashicorpVaultSecretEngineName() != null && keyData.getHashicorpVaultSecretName() != null) {
            return new HashicorpVaultKeyPair(keyData.getHashicorpVaultPublicKeyId(), keyData.getHashicorpVaultPrivateKeyId(), keyData.getHashicorpVaultSecretEngineName(), keyData.getHashicorpVaultSecretName(), keyData.getHashicorpVaultSecretVersion());
        }

        //case 5, the keys are provided inside a file
        if(keyData.getPublicKeyPath() != null && keyData.getPrivateKeyPath() != null) {
            return new FilesystemKeyPair(keyData.getPublicKeyPath(), keyData.getPrivateKeyPath());
        }

        //case 6, the key config specified is invalid
        return new UnsupportedKeyPair(
            keyData.getConfig(),
            keyData.getPrivateKey(),
            keyData.getPublicKey(),
            keyData.getPrivateKeyPath(),
            keyData.getPublicKeyPath(),
            keyData.getAzureVaultPublicKeyId(),
            keyData.getAzureVaultPrivateKeyId(),
            keyData.getAzureVaultPublicKeyVersion(),
            keyData.getAzureVaultPrivateKeyVersion(),
            keyData.getHashicorpVaultPublicKeyId(),
            keyData.getHashicorpVaultPrivateKeyId(),
            keyData.getHashicorpVaultSecretEngineName(),
            keyData.getHashicorpVaultSecretName(),
            keyData.getHashicorpVaultSecretVersion()
        );
    }

    @Override
    public KeyData marshal(final ConfigKeyPair keyPair) {

        KeyData keyData = new KeyData();

        if(keyPair instanceof DirectKeyPair) {
            DirectKeyPair kp = (DirectKeyPair) keyPair;

            keyData.setPublicKey(kp.getPublicKey());
            keyData.setPrivateKey(kp.getPrivateKey());
            return keyData;
        }

        if(keyPair instanceof InlineKeypair) {
            InlineKeypair kp = (InlineKeypair) keyPair;

            keyData.setPublicKey(kp.getPublicKey());
            keyData.setConfig(kp.getPrivateKeyConfig());
            return keyData;
        }

        if(keyPair instanceof AzureVaultKeyPair) {
            AzureVaultKeyPair kp = (AzureVaultKeyPair) keyPair;

            keyData.setAzureVaultPublicKeyId(kp.getPublicKeyId());
            keyData.setAzureVaultPrivateKeyId(kp.getPrivateKeyId());
            keyData.setAzureVaultPublicKeyVersion(kp.getPublicKeyVersion());
            keyData.setAzureVaultPrivateKeyVersion(kp.getPrivateKeyVersion());
            return keyData;
        }

        if(keyPair instanceof HashicorpVaultKeyPair) {
            HashicorpVaultKeyPair kp = (HashicorpVaultKeyPair) keyPair;

            keyData.setHashicorpVaultPublicKeyId(kp.getPublicKeyId());
            keyData.setHashicorpVaultPrivateKeyId(kp.getPrivateKeyId());
            keyData.setHashicorpVaultSecretEngineName(kp.getSecretEngineName());
            keyData.setHashicorpVaultSecretName(kp.getSecretName());
            return keyData;
        }

        if(keyPair instanceof FilesystemKeyPair) {
            FilesystemKeyPair kp = (FilesystemKeyPair) keyPair;

            keyData.setPublicKeyPath(kp.getPublicKeyPath());
            keyData.setPrivateKeyPath(kp.getPrivateKeyPath());
            return keyData;
        }

        if(keyPair instanceof UnsupportedKeyPair) {
            UnsupportedKeyPair kp = (UnsupportedKeyPair) keyPair;
            return new KeyData(
                kp.getConfig(),
                kp.getPrivateKey(),
                kp.getPublicKey(),
                kp.getPrivateKeyPath(),
                kp.getPublicKeyPath(),
                kp.getAzureVaultPrivateKeyId(),
                kp.getAzureVaultPublicKeyId(),
                kp.getAzureVaultPublicKeyVersion(),
                kp.getAzureVaultPrivateKeyVersion(),
                kp.getHashicorpVaultPrivateKeyId(),
                kp.getHashicorpVaultPublicKeyId(),
                kp.getHashicorpVaultSecretEngineName(),
                kp.getHashicorpVaultSecretName(),
                kp.getHashicorpVaultSecretVersion());
        }

        throw new UnsupportedOperationException("The keypair type " + keyPair.getClass() + " is not allowed");
    }
}
