package com.quorum.tessera.test.rest;

import com.quorum.tessera.api.model.ReceiveResponse;
import com.quorum.tessera.api.model.SendResponse;
import com.quorum.tessera.test.Party;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import com.quorum.tessera.test.PartyHelper;
import java.util.Arrays;


/**
 * This tests that a node that hosts multiple sets of keys can send/receive
 * transactions for both keys
 */
@RunWith(Parameterized.class)
public class MultipleKeyNodeIT {

    private PartyHelper partyHelper = PartyHelper.create();

    private final Client client = ClientBuilder.newClient();

    private final String recipientAlias;

    private String txHash;

    private RestUtils restUtils = new RestUtils();

    public MultipleKeyNodeIT(String recipientAlias) {
        this.recipientAlias = recipientAlias;
    }

    @Before
    public void onSetUp() {

        Party sender = partyHelper.findByAlias("A");
        
        Party recipient = partyHelper.findByAlias(recipientAlias);
        byte[] transactionData = restUtils.createTransactionData();
        final SendResponse result = restUtils.sendRequestAssertSuccess(sender, transactionData, recipient);

        assertThat(result.getKey()).isNotBlank();

        this.txHash = result.getKey();
    }



    @Test
    public void thenTransactionHasBeenPersistedOnOtherNode() throws UnsupportedEncodingException {

        final byte[] transactionData = RestUtils.generateTransactionData();

        Party recipient = partyHelper.findByAlias(recipientAlias);
        //retrieve the transaction
        final Response retrieveResponse = this.client.target(recipient.getQ2TUri())
                .path("transaction")
                .path(URLEncoder.encode(txHash, "UTF-8"))
                .queryParam("to", recipient.getPublicKey())
                .request()
                .get();

        assertThat(retrieveResponse).isNotNull();
        assertThat(retrieveResponse.getStatus())
                .describedAs(txHash + " should be present on other node")
                .isEqualTo(200);

        final ReceiveResponse result = retrieveResponse.readEntity(ReceiveResponse.class);
        //TODO: Verify payload
        assertThat(result).isNotNull();
        

    }

    @Parameterized.Parameters
    public static List<String> recipients() {
        return Arrays.asList("C","D");
        
    }

}
