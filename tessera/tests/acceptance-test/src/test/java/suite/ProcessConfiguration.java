package suite;

import com.quorum.tessera.config.CommunicationType;
import com.quorum.tessera.test.DBType;

/**
 * Sets the runtime properties of the nodes used during the test. This allows the same tests to run with different node
 * configurations. Similar to {@link ProcessConfig}, but can be instantiated for use with parameterized tests.
 */
public class ProcessConfiguration {

    private DBType dbType;

    private CommunicationType communicationType;

    private SocketType socketType;

    private EnclaveType enclaveType = EnclaveType.LOCAL;

    private boolean admin = false;

    private String prefix = "";

    public ProcessConfiguration(
            final DBType dbType,
            final CommunicationType communicationType,
            final SocketType socketType,
            final EnclaveType enclaveType,
            final boolean admin,
            final String prefix) {
        this.dbType = dbType;
        this.communicationType = communicationType;
        this.socketType = socketType;
        this.enclaveType = enclaveType;
        this.admin = admin;
        this.prefix = prefix;
    }

    public ProcessConfiguration() {}

    public DBType getDbType() {
        return dbType;
    }

    public void setDbType(final DBType dbType) {
        this.dbType = dbType;
    }

    public CommunicationType getCommunicationType() {
        return communicationType;
    }

    public void setCommunicationType(final CommunicationType communicationType) {
        this.communicationType = communicationType;
    }

    public SocketType getSocketType() {
        return socketType;
    }

    public void setSocketType(final SocketType socketType) {
        this.socketType = socketType;
    }

    public EnclaveType getEnclaveType() {
        return enclaveType;
    }

    public void setEnclaveType(final EnclaveType enclaveType) {
        this.enclaveType = enclaveType;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(final boolean admin) {
        this.admin = admin;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(final String prefix) {
        this.prefix = prefix;
    }
}
