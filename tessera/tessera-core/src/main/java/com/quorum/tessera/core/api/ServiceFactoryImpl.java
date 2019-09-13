package com.quorum.tessera.core.api;

import com.quorum.tessera.admin.ConfigService;
import com.quorum.tessera.config.Config;
import com.quorum.tessera.enclave.Enclave;
import com.quorum.tessera.partyinfo.PartyInfoService;
import com.quorum.tessera.service.locator.ServiceLocator;
import com.quorum.tessera.data.EncryptedRawTransactionDAO;
import com.quorum.tessera.data.EncryptedTransactionDAO;
import com.quorum.tessera.partyinfo.PartyInfoServiceFactory;
import com.quorum.tessera.partyinfo.PayloadPublisher;
import com.quorum.tessera.partyinfo.ResendManager;
import com.quorum.tessera.transaction.TransactionManager;

public class ServiceFactoryImpl implements ServiceFactory {

    private final ServiceLocator serviceLocator = ServiceLocator.create();

    private final PartyInfoServiceFactory partyInfoServiceFactory = PartyInfoServiceFactory.create();

    public ServiceFactoryImpl() {}

    @Override
    public PartyInfoService partyInfoService() {
        return partyInfoServiceFactory.partyInfoService();
    }

    @Override
    public Enclave enclave() {
        return find(Enclave.class);
    }

    public <T> T find(Class<T> type) {
        return serviceLocator.getServices().stream()
                .filter(type::isInstance)
                .map(type::cast)
                .findAny()
                .orElseThrow(() -> new IllegalStateException("Unable to find service type :" + type));
    }

    @Override
    public TransactionManager transactionManager() {
        return find(TransactionManager.class);
    }

    @Override
    public Config config() {
        return find(Config.class);
    }

    @Override
    public ConfigService configService() {
        return find(ConfigService.class);
    }

    @Override
    public EncryptedTransactionDAO encryptedTransactionDAO() {
        return find(EncryptedTransactionDAO.class);
    }

    @Override
    public EncryptedRawTransactionDAO encryptedRawTransactionDAO() {
        return find(EncryptedRawTransactionDAO.class);
    }

    @Override
    public ResendManager resendManager() {
        return partyInfoServiceFactory.resendManager();
    }

    @Override
    public PayloadPublisher payloadPublisher() {
        return partyInfoServiceFactory.payloadPublisher();
    }
}
