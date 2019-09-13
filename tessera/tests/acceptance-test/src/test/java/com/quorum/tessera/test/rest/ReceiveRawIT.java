package com.quorum.tessera.test.rest;

import com.quorum.tessera.api.model.SendRequest;
import com.quorum.tessera.api.model.SendResponse;
import com.quorum.tessera.test.Party;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import static org.assertj.core.api.Assertions.assertThat;
import com.quorum.tessera.test.PartyHelper;
import javax.ws.rs.client.ClientBuilder;

public class ReceiveRawIT {

    private static final String RECEIVE_PATH = "/receiveraw";

    private static final String C11N_TO = "c11n-to";

    private static final String C11N_KEY = "c11n-key";

    private static final byte[] PAYLOAD = "TXN_DATA".getBytes();
    
    private PartyHelper partyHelper = PartyHelper.create();

    private Client client = ClientBuilder.newClient();

    private String hash;

    private Party partyOne;
    
    private Party partyTwo;
    
    //Persist a single transaction that can be used later
    @Before
    public void init() {
        
        this.partyOne = partyHelper.findByAlias("A");
        this.partyTwo = partyHelper.findByAlias("B");
        
        SendRequest sendRequest = new SendRequest();
        sendRequest.setPayload(PAYLOAD);
        sendRequest.setTo(partyTwo.getPublicKey());
        sendRequest.setFrom(partyOne.getPublicKey());

        final Response response = client.target(partyOne.getQ2TUri())
            .path("/send")
            .request()
            .post(Entity.entity(sendRequest, MediaType.APPLICATION_JSON));

        final SendResponse result = response.readEntity(SendResponse.class);

        this.hash = result.getKey();

    }

    @Test
    public void fetchExistingTransactionUsingOwnKey() {

        final Response response = client.target(partyOne.getQ2TUri())
            .path(RECEIVE_PATH)
            .request()
            .header(C11N_KEY, this.hash)
            .header(C11N_TO, partyOne.getPublicKey())
            .buildGet()
            .invoke();

        //validate result

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);

        final byte[] result = response.readEntity(byte[].class);

        assertThat(result).isEqualTo(PAYLOAD);

    }

    @Test
    public void fetchExistingTransactionNotUsingKeyOnSender() {

        final Response response = client.target(partyOne.getQ2TUri())
            .path(RECEIVE_PATH)
            .request()
            .header(C11N_KEY, this.hash)
            .buildGet()
            .invoke();

        //validate result

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);

        final byte[] result = response.readEntity(byte[].class);

        assertThat(result).isEqualTo(PAYLOAD);

    }

    @Test
    public void fetchExistingTransactionNotUsingKeyOnRecipient() {

        Party sender = partyHelper.findByAlias("A");
        
        byte[] transactionPayload = new RestUtils().createTransactionData();

        SendRequest sendRequest = new SendRequest();
        sendRequest.setPayload(transactionPayload);
        sendRequest.setFrom(sender.getPublicKey());
        sendRequest.setTo(partyHelper.findByAlias("B").getPublicKey());
        

        final Response r = client.target(sender.getQ2TUri())
            .path("/send")
            .request()
            .post(Entity.entity(sendRequest, MediaType.APPLICATION_JSON));

        final SendResponse sendResponse = r.readEntity(SendResponse.class);

        final Response response = client.target(partyHelper.findByAlias("B").getQ2TUri())
            .path(RECEIVE_PATH)
            .request()
            .header(C11N_KEY, sendResponse.getKey())
            .buildGet()
            .invoke();

        //validate result

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);

        final byte[] result = response.readEntity(byte[].class);

        assertThat(result).isEqualTo(transactionPayload);

    }

    @Test
    public void fetchExistingTransactionUsingRecipientKey() {

        final Response response = client.target(partyTwo.getQ2TUri())
            .path(RECEIVE_PATH)
            .request()
            .header(C11N_KEY, this.hash)
            .header(C11N_TO, partyTwo.getPublicKey())
            .buildGet()
            .invoke();

        //validate result

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);

        final byte[] result = response.readEntity(byte[].class);

        assertThat(result).isEqualTo(PAYLOAD);

    }

    @Test
    public void fetchNonexistentTransactionFails() {

        final Response response = client.target(partyOne.getQ2TUri())
            .path(RECEIVE_PATH)
            .request()
            .header(C11N_KEY, "invalidhashvalue")
            .buildGet()
            .invoke();

        //validate result

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(404);

        final String result = response.readEntity(String.class);
        assertThat(result).isEqualTo("Message with hash invalidhashvalue was not found");
  
    }

}
