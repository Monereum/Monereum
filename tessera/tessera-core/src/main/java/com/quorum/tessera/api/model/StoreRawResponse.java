package com.quorum.tessera.api.model;

import io.swagger.annotations.ApiModel;

import javax.xml.bind.annotation.XmlInlineBinaryData;

/**
 * Model representation of a JSON body on incoming HTTP requests
 * <p>
 * A response to a {@link StoreRawRequest} after the raw transaction has been saved
 */
@ApiModel
public class StoreRawResponse {

    @XmlInlineBinaryData
    private byte[] key;

    public StoreRawResponse(byte[] key) {
        this.key = key;
    }

    public StoreRawResponse() {
    }

    public byte[] getKey() {
        return key;
    }

    public void setKey(byte[] key) {
        this.key = key;
    }
}
