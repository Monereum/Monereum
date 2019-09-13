package com.quorum.tessera.config.keypairs;

import com.quorum.tessera.config.constraints.ValidAzureVaultKeyPair;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlElement;

@ValidAzureVaultKeyPair
public class AzureVaultKeyPair implements ConfigKeyPair {

    @NotNull
    @XmlElement
    @Pattern(regexp = "^[0-9a-zA-Z\\-]*$",
            message = "Azure Key Vault key IDs can only contain alphanumeric characters and dashes (-)")
    private String publicKeyId;

    @NotNull
    @XmlElement
    @Pattern(regexp = "^[0-9a-zA-Z\\-]*$",
            message = "Azure Key Vault key IDs can only contain alphanumeric characters and dashes (-)")
    private String privateKeyId;

    @XmlElement
    @Size(min = 32, max = 32, message = "length must be 32 characters")
    private String publicKeyVersion;

    @XmlElement
    @Size(min = 32, max = 32, message = "length must be 32 characters")
    private String privateKeyVersion;

    public AzureVaultKeyPair(String publicKeyId, String privateKeyId, String publicKeyVersion, String privateKeyVersion) {
        this.publicKeyId = publicKeyId;
        this.privateKeyId = privateKeyId;
        this.publicKeyVersion = publicKeyVersion;
        this.privateKeyVersion = privateKeyVersion;
    }

    public String getPublicKeyId() {
        return this.publicKeyId;
    }

    public String getPrivateKeyId() {
        return this.privateKeyId;
    }

    public String getPublicKeyVersion() {
        return publicKeyVersion;
    }

    public String getPrivateKeyVersion() {
        return privateKeyVersion;
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
