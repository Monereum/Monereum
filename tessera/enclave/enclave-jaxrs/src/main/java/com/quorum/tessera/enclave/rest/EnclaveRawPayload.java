package com.quorum.tessera.enclave.rest;

import java.util.List;
import javax.xml.bind.annotation.XmlMimeType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class EnclaveRawPayload {

    @XmlMimeType("base64Binary")
    private byte[] encryptedPayload;

    @XmlMimeType("base64Binary")
    private byte[] encryptedKey;

    @XmlMimeType("base64Binary")
    private byte[] nonce;

    @XmlMimeType("base64Binary")
    private byte[] from;

    @XmlMimeType("base64Binary")
    private List<byte[]> recipientPublicKeys;

    public byte[] getEncryptedPayload() {
        return encryptedPayload;
    }

    public void setEncryptedPayload(byte[] encryptedPayload) {
        this.encryptedPayload = encryptedPayload;
    }

    public byte[] getEncryptedKey() {
        return encryptedKey;
    }

    public void setEncryptedKey(byte[] encryptedKey) {
        this.encryptedKey = encryptedKey;
    }

    public byte[] getNonce() {
        return nonce;
    }

    public void setNonce(byte[] nonce) {
        this.nonce = nonce;
    }

    public byte[] getFrom() {
        return from;
    }

    public void setFrom(byte[] from) {
        this.from = from;
    }

    public List<byte[]> getRecipientPublicKeys() {
        return recipientPublicKeys;
    }

    public void setRecipientPublicKeys(List<byte[]> recipientPublicKeys) {
        this.recipientPublicKeys = recipientPublicKeys;
    }

}
