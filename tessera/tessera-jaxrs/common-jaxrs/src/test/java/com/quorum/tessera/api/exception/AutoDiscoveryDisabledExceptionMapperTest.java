package com.quorum.tessera.api.exception;

import com.quorum.tessera.partyinfo.AutoDiscoveryDisabledException;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;

public class AutoDiscoveryDisabledExceptionMapperTest {

    private AutoDiscoveryDisabledExceptionMapper mapper = new AutoDiscoveryDisabledExceptionMapper();

    @Test
    public void handleAutoDiscoveryDisabledException() {
        final String message = ".. all outta gum";
        final AutoDiscoveryDisabledException exception = new AutoDiscoveryDisabledException(message);

        final Response result = mapper.toResponse(exception);

        assertThat(result.getStatus()).isEqualTo(403);
        assertThat(result.getEntity()).isEqualTo(message);
    }
}
