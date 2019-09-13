package com.quorum.tessera.partyinfo;

import com.quorum.tessera.config.CommunicationType;
import com.quorum.tessera.config.Config;
import com.quorum.tessera.config.ServerConfig;
import java.util.NoSuchElementException;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class P2pClientFactoryTest {

    @Test
    public void newFactory() {

        Config config = mock(Config.class);
        ServerConfig serverConfig = mock(ServerConfig.class);
        when(serverConfig.getCommunicationType()).thenReturn(CommunicationType.REST);
        when(config.getP2PServerConfig()).thenReturn(serverConfig);

        P2pClientFactory factory = P2pClientFactory.newFactory(config);

        assertThat(factory).isExactlyInstanceOf(MockP2pClientFactory.class);
    }

    @Test(expected = NoSuchElementException.class)
    public void newFactoryNullCommuicationType() {

        Config config = mock(Config.class);
        ServerConfig serverConfig = mock(ServerConfig.class);
        when(config.getP2PServerConfig()).thenReturn(serverConfig);

        P2pClientFactory.newFactory(config);
    }
}
