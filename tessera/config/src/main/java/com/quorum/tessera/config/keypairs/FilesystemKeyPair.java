package com.quorum.tessera.config.keypairs;

import com.quorum.tessera.config.KeyDataConfig;
import com.quorum.tessera.config.adapters.PathAdapter;
import com.quorum.tessera.config.constraints.ValidBase64;
import com.quorum.tessera.config.constraints.ValidContent;
import com.quorum.tessera.config.constraints.ValidPath;
import com.quorum.tessera.config.util.JaxbUtil;
import com.quorum.tessera.io.IOCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.charset.StandardCharsets.UTF_8;

public class FilesystemKeyPair implements ConfigKeyPair {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilesystemKeyPair.class);

    @ValidContent(minLines = 1, maxLines = 1, message = "file expected to contain a single non empty value")
    @NotNull
    @ValidPath(checkExists = true, message = "File does not exist")
    @XmlElement
    @XmlJavaTypeAdapter(PathAdapter.class)
    private final Path publicKeyPath;

    @ValidContent(minLines = 1, message = "file expected to contain at least one line")
    @NotNull
    @ValidPath(checkExists = true, message = "File does not exist")
    @XmlElement
    @XmlJavaTypeAdapter(PathAdapter.class)
    private final Path privateKeyPath;

    private InlineKeypair inlineKeypair;

    private String password;

    public FilesystemKeyPair(final Path publicKeyPath, final Path privateKeyPath) {
        this.publicKeyPath = publicKeyPath;
        this.privateKeyPath = privateKeyPath;

        try {
            loadKeys();
        } catch (final Exception ex) {
            //silently discard errors as these get picked up by the validator
            LOGGER.debug("Unable to read key files", ex);
        }
    }

    @Override
    @Size(min = 1)
    @ValidBase64(message = "Invalid Base64 key provided")
    public String getPublicKey() {
        if (this.inlineKeypair == null) {
            return null;
        }
        return this.inlineKeypair.getPublicKey();
    }

    @Override
    @Size(min = 1)
    @ValidBase64(message = "Invalid Base64 key provided")
    @Pattern(regexp = "^((?!NACL_FAILURE).)*$", message = "Could not decrypt the private key with the provided password, please double check the passwords provided")
    public String getPrivateKey() {
        if (this.inlineKeypair == null) {
            return null;
        }
        return this.inlineKeypair.getPrivateKey();
    }

    @Override
    public void withPassword(final String password) {
        this.password = password;
        if (this.inlineKeypair != null) {
            this.inlineKeypair.withPassword(this.password);
        }
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    public Path getPublicKeyPath() {
        return publicKeyPath;
    }

    public Path getPrivateKeyPath() {
        return privateKeyPath;
    }

    public InlineKeypair getInlineKeypair() {
        return inlineKeypair;
    }

    private void loadKeys() {
        this.inlineKeypair = new InlineKeypair(
            IOCallback.execute(() -> new String(Files.readAllBytes(this.publicKeyPath), UTF_8)),
            JaxbUtil.unmarshal(
                IOCallback.execute(() -> Files.newInputStream(privateKeyPath)),
                KeyDataConfig.class
            )
        );
    }

}
