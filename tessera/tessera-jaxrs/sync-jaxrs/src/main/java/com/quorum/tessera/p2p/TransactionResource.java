package com.quorum.tessera.p2p;

import com.quorum.tessera.core.api.ServiceFactory;
import com.quorum.tessera.partyinfo.ResendRequest;
import com.quorum.tessera.partyinfo.ResendResponse;
import com.quorum.tessera.data.MessageHash;
import com.quorum.tessera.transaction.TransactionManager;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Objects;

import static javax.ws.rs.core.MediaType.*;

/**
 * Provides endpoints for dealing with transactions, including:
 *
 * <p>- creating new transactions and distributing them - deleting transactions - fetching transactions - resending old
 * transactions
 */
@Path("/")
public class TransactionResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionResource.class);

    private final TransactionManager delegate;

    public TransactionResource() {
        this(ServiceFactory.create().transactionManager());
    }

    public TransactionResource(TransactionManager delegate) {
        this.delegate = Objects.requireNonNull(delegate);
    }

    @ApiOperation("Resend transactions for given key or message hash/recipient")
    @ApiResponses({
        @ApiResponse(code = 200, message = "Encoded payload when TYPE is INDIVIDUAL", response = String.class),
        @ApiResponse(code = 500, message = "General error")
    })
    @POST
    @Path("resend")
    @Consumes(APPLICATION_JSON)
    @Produces(TEXT_PLAIN)
    public Response resend(
            @ApiParam(name = "resendRequest", required = true) @Valid @NotNull final ResendRequest resendRequest) {

        LOGGER.debug("Received resend request");

        ResendResponse response = delegate.resend(resendRequest);
        Response.ResponseBuilder builder = Response.status(Status.OK);
        response.getPayload().ifPresent(builder::entity);
        return builder.build();
    }

    @ApiOperation(value = "Transmit encrypted payload between P2PRestApp Nodes")
    @ApiResponses({
        @ApiResponse(code = 201, message = "Key created status"),
        @ApiResponse(code = 500, message = "General error")
    })
    @POST
    @Path("push")
    @Consumes(APPLICATION_OCTET_STREAM)
    public Response push(
            @ApiParam(name = "payload", required = true, value = "Key data to be stored.") final byte[] payload) {

        LOGGER.debug("Received push request");

        final MessageHash messageHash = delegate.storePayload(payload);
        LOGGER.debug("Push request generated hash {}", Objects.toString(messageHash));
        // TODO: Return the query url not the string of the messageHAsh
        return Response.status(Response.Status.CREATED).entity(Objects.toString(messageHash)).build();
    }
}
