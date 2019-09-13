package com.quorum.tessera.transaction;

import com.quorum.tessera.partyinfo.ResendResponse;
import com.quorum.tessera.partyinfo.ResendRequest;
import com.quorum.tessera.data.MessageHash;
import com.quorum.tessera.api.model.*;

public interface TransactionManager {

    SendResponse send(SendRequest sendRequest);

    SendResponse sendSignedTransaction(SendSignedRequest sendRequest);

    void delete(DeleteRequest request);

    ResendResponse resend(ResendRequest request);

    MessageHash storePayload(byte[] toByteArray);

    ReceiveResponse receive(ReceiveRequest request);

    StoreRawResponse store(StoreRawRequest storeRequest);
}
