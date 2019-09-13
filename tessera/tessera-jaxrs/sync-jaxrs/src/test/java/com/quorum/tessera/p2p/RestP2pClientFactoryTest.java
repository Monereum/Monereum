package com.quorum.tessera.p2p;

import com.quorum.tessera.partyinfo.P2pClient;
import com.quorum.tessera.config.CommunicationType;
import com.quorum.tessera.config.Config;
import com.quorum.tessera.config.ServerConfig;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RestP2pClientFactoryTest {

    @Test
    public void create() {
        RestP2pClientFactory factory = new RestP2pClientFactory();
        assertThat(factory.communicationType()).isEqualTo(CommunicationType.REST);

        Config config = mock(Config.class);
        ServerConfig serverConfig = mock(ServerConfig.class);
        when(serverConfig.isSsl()).thenReturn(Boolean.FALSE);
        when(config.getP2PServerConfig()).thenReturn(serverConfig);
        P2pClient result = factory.create(config);

        assertThat(result).isNotNull();
    }
}
