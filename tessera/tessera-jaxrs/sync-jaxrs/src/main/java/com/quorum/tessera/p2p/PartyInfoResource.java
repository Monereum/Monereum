package com.quorum.tessera.p2p;

import com.quorum.tessera.partyinfo.PartyInfoParser;
import com.quorum.tessera.partyinfo.PartyInfoService;
import com.quorum.tessera.partyinfo.model.PartyInfo;
import com.quorum.tessera.partyinfo.model.Recipient;
import com.quorum.tessera.enclave.Enclave;
import com.quorum.tessera.enclave.EncodedPayload;
import com.quorum.tessera.enclave.PayloadEncoder;
import com.quorum.tessera.encryption.PublicKey;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;

/** Defines endpoints for requesting node discovery (partyinfo) information */
@Path("/partyinfo")
public class PartyInfoResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(PartyInfoResource.class);

    private final PartyInfoParser partyInfoParser;

    private final PartyInfoService partyInfoService;

    private final Client restClient;

    private final Enclave enclave;

    private final PayloadEncoder payloadEncoder;

    private final boolean enableKeyValidation;

    public PartyInfoResource(
            final PartyInfoService partyInfoService,
            final PartyInfoParser partyInfoParser,
            final Client restClient,
            final Enclave enclave,
            final PayloadEncoder payloadEncoder,
            final boolean enableKeyValidation) {
        this.partyInfoService = requireNonNull(partyInfoService, "partyInfoService must not be null");
        this.partyInfoParser = requireNonNull(partyInfoParser, "partyInfoParser must not be null");
        this.restClient = requireNonNull(restClient);
        this.enclave = requireNonNull(enclave);
        this.payloadEncoder = requireNonNull(payloadEncoder);
        this.enableKeyValidation = enableKeyValidation;
    }

    public PartyInfoResource(
            final PartyInfoService partyInfoService,
            final PartyInfoParser partyInfoParser,
            final Client restClient,
            final Enclave enclave,
            final boolean enableKeyValidation) {
        this(partyInfoService, partyInfoParser, restClient, enclave, PayloadEncoder.create(), enableKeyValidation);
    }

    /**
     * Allows node information to be retrieved in a specific encoded form including other node URLS and public key to
     * URL mappings
     *
     * @param payload The encoded node information from the requester
     * @return the merged node information from this node, which may contain new information
     */
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @ApiOperation(value = "Request public key/url of other nodes", produces = "public keylist/url")
    @ApiResponses({@ApiResponse(code = 200, message = "Encoded PartyInfo Data", response = byte[].class)})
    public Response partyInfo(@ApiParam(required = true) final byte[] payload) {

        final PartyInfo partyInfo = partyInfoParser.from(payload);

        if (!enableKeyValidation) {
            partyInfoService.updatePartyInfo(partyInfo);

            // create an empty party info object with our URL to send back
            // this is used by older versions (before 0.10.0), but we don't want to give any info back
            final PartyInfo emptyInfo = new PartyInfo(partyInfoService.getPartyInfo().getUrl(), emptySet(), emptySet());
            final byte[] returnData = partyInfoParser.to(emptyInfo);
            return Response.ok(returnData).build();
        }

        // Start validation stuff
        final PublicKey sender = enclave.defaultPublicKey();

        final String url = partyInfo.getUrl();

        final String dataToEncrypt = UUID.randomUUID().toString();

        final Predicate<Recipient> isValidRecipientKey =
                r -> {
                    try {

                        final PublicKey key = r.getKey();
                        final EncodedPayload encodedPayload =
                                enclave.encryptPayload(dataToEncrypt.getBytes(), sender, Arrays.asList(key));

                        final byte[] encodedPayloadData = payloadEncoder.encode(encodedPayload);

                        Response response =
                                restClient
                                        .target(r.getUrl())
                                        .path("partyinfo")
                                        .path("validate")
                                        .request()
                                        .post(Entity.entity(encodedPayloadData, MediaType.APPLICATION_OCTET_STREAM));

                        String unencodedValidationData = response.readEntity(String.class);

                        boolean isValid = Objects.equals(unencodedValidationData, dataToEncrypt);
                        if (!isValid) {
                            LOGGER.warn("Invalid key found {} recipient will be ignored.", r.getUrl());
                        }

                        return isValid;
                        // Assume any all exceptions to mean invalid. enclave bubbles up nacl array out of
                        // bounds when calculating shared key from invalid data
                    } catch (Exception ex) {
                        LOGGER.debug(null, ex);
                        return false;
                    }
                };

        final Predicate<Recipient> isSendingUrl = r -> r.getUrl().equalsIgnoreCase(url);

        // Validate caller and treat no valid certs as security issue.
        final Set<Recipient> recipients =
                partyInfo.getRecipients().stream()
                        .filter(isSendingUrl.and(isValidRecipientKey))
                        .collect(Collectors.toSet());

        if (recipients.isEmpty()) {
            throw new SecurityException("No key found for url " + url);
        }

        final PartyInfo modifiedPartyInfo = new PartyInfo(url, recipients, partyInfo.getParties());

        // End validation stuff
        partyInfoService.updatePartyInfo(modifiedPartyInfo);

        return Response.ok().build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Fetch network/peer information", produces = "public list of peers/publickey mappings")
    @ApiResponses({@ApiResponse(code = 200, message = "Peer/Network information", response = PartyInfo.class)})
    public Response getPartyInfo() {

        final PartyInfo current = this.partyInfoService.getPartyInfo();

        // TODO: remove the filter when URIs don't need to end with a /
        final JsonArrayBuilder peersBuilder = Json.createArrayBuilder();
        current.getParties().stream()
                .filter(p -> p.getUrl().endsWith("/"))
                .map(
                        party -> {
                            final JsonObjectBuilder builder = Json.createObjectBuilder();
                            builder.add("url", party.getUrl());
                            if (party.getLastContacted() != null) {
                                builder.add("lastContact", party.getLastContacted().toString());
                            } else {
                                builder.addNull("lastContact");
                            }
                            return builder.build();
                        })
                .forEach(peersBuilder::add);

        final JsonArrayBuilder recipientBuilder = Json.createArrayBuilder();
        current.getRecipients().stream()
                .map(
                        recipient ->
                                Json.createObjectBuilder()
                                        .add("key", recipient.getKey().encodeToBase64())
                                        .add("url", recipient.getUrl())
                                        .build())
                .forEach(recipientBuilder::add);

        final String output =
                Json.createObjectBuilder()
                        .add("url", current.getUrl())
                        .add("peers", peersBuilder.build())
                        .add("keys", recipientBuilder.build())
                        .build()
                        .toString();

        return Response.status(Response.Status.OK).entity(output).build();
    }

    @POST
    @Path("validate")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.TEXT_PLAIN)
    public Response validate(byte[] payloadData) {
        final EncodedPayload payload = payloadEncoder.decode(payloadData);

        final PublicKey mykey = payload.getRecipientKeys().iterator().next();

        final byte[] result = enclave.unencryptTransaction(payload, mykey);

        return Response.ok(new String(result)).build();
    }
}
