package com.quorum.tessera.p2p;

import com.quorum.tessera.partyinfo.P2pClient;
import com.quorum.tessera.partyinfo.P2pClientFactory;
import com.quorum.tessera.config.CommunicationType;
import com.quorum.tessera.config.Config;
import com.quorum.tessera.jaxrs.client.ClientFactory;
import com.quorum.tessera.ssl.context.ClientSSLContextFactory;
import com.quorum.tessera.ssl.context.SSLContextFactory;
import javax.ws.rs.client.Client;

public class RestP2pClientFactory implements P2pClientFactory {

    @Override
    public P2pClient create(Config config) {

        SSLContextFactory clientSSLContextFactory = ClientSSLContextFactory.create();

        ClientFactory clientFactory = new ClientFactory(clientSSLContextFactory);

        Client client = clientFactory.buildFrom(config.getP2PServerConfig());

        return new RestP2pClient(client);
    }

    @Override
    public CommunicationType communicationType() {
        return CommunicationType.REST;
    }
}
