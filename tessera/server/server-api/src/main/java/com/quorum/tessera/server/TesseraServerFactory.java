package com.quorum.tessera.server;

import com.quorum.tessera.ServiceLoaderUtil;
import com.quorum.tessera.config.CommunicationType;
import com.quorum.tessera.config.ServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public interface TesseraServerFactory<T> {

    Logger LOGGER = LoggerFactory.getLogger(TesseraServerFactory.class);

    TesseraServer createServer(ServerConfig config, Set<T> services);

    CommunicationType communicationType();

    static TesseraServerFactory create(CommunicationType communicationType) {
        return ServiceLoaderUtil.loadAll(TesseraServerFactory.class)
                .filter(f -> f.communicationType() == communicationType)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No server factory found for " + communicationType));
    }
}
