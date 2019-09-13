package com.quorum.tessera.config.keypairs;

import com.quorum.tessera.config.constraints.ValidPositiveInteger;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlElement;

public class HashicorpVaultKeyPair implements ConfigKeyPair {

    @NotNull
    @XmlElement
    private String publicKeyId;

    @NotNull
    @XmlElement
    private String privateKeyId;

    @NotNull
    @XmlElement
    private String secretEngineName;

    @NotNull
    @XmlElement
    private String secretName;

    @ValidPositiveInteger
    @XmlElement
    private String secretVersion;

    public HashicorpVaultKeyPair(String publicKeyId, String privateKeyId, String secretEngineName, String secretName, String secretVersion) {
        this.publicKeyId = publicKeyId;
        this.privateKeyId = privateKeyId;
        this.secretEngineName = secretEngineName;
        this.secretName = secretName;
        this.secretVersion = secretVersion;
    }

    public String getPublicKeyId() {
        return publicKeyId;
    }

    public String getPrivateKeyId() {
        return privateKeyId;
    }

    public String getSecretEngineName() {
        return secretEngineName;
    }

    public String getSecretName() {
        return secretName;
    }

    public String getSecretVersion() {
        return secretVersion;
    }

    public Integer getSecretVersionAsInt() {
        if(secretVersion == null) {
            return 0;
        } else {
            return Integer.parseInt(secretVersion);
        }
    }

    @Override
    public String getPublicKey() {
        //keys are not fetched from vault yet so return null
        return null;
    }

    @Override
    public String getPrivateKey() {
        //keys are not fetched from vault yet so return null
        return null;
    }

    @Override
    public void withPassword(String password) {
        //password not used with vault stored keys
    }

    @Override
    public String getPassword() {
        //no password to return
        return "";
    }
}
