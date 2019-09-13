package com.quorum.tessera.data;

import javax.persistence.Embeddable;
import javax.persistence.Lob;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Base64;

@Embeddable
public class MessageHash implements Serializable {

    @Lob private byte[] hashBytes;

    public MessageHash() {}

    public MessageHash(final byte[] hashBytes) {
        this.hashBytes = Arrays.copyOf(hashBytes, hashBytes.length);
    }

    public void setHashBytes(final byte[] hashBytes) {
        this.hashBytes = Arrays.copyOf(hashBytes, hashBytes.length);
    }

    public byte[] getHashBytes() {
        return Arrays.copyOf(hashBytes, hashBytes.length);
    }

    @Override
    public boolean equals(final Object o) {
        return (o instanceof MessageHash) && Arrays.equals(hashBytes, ((MessageHash) o).hashBytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(getHashBytes());
    }

    @Override
    public String toString() {
        return Base64.getEncoder().encodeToString(hashBytes);
    }
}
