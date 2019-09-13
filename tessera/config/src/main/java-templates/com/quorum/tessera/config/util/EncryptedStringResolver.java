package com.quorum.tessera.config.util;

import org.jasypt.encryption.pbe.PBEStringEncryptor;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.properties.PropertyValueEncryptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EncryptedStringResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(EncryptedStringResolver.class);

    private final PBEStringEncryptor encryptor;

    private boolean isPasswordSet;

    public EncryptedStringResolver() {
        this.encryptor = new StandardPBEStringEncryptor();
    }

    public String resolve(final String textToDecrypt) {

        if (PropertyValueEncryptionUtils.isEncryptedValue(textToDecrypt)) {

            if (!isPasswordSet) {
                encryptor.setPassword(
                        ConfigSecretReader.readSecretFromFile().orElseGet(ConfigSecretReader::readSecretFromConsole));
                isPasswordSet = true;
            }

            return PropertyValueEncryptionUtils.decrypt(textToDecrypt, encryptor);
        }

        LOGGER.warn(
                "Some sensitive values are being given as unencrypted plain text in config. "
                        + "Please note this is NOT recommended for production environment.");

        return textToDecrypt;
    }
}
