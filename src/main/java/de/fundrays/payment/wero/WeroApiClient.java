package de.fundrays.payment.wero;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/payments")
@RegisterRestClient(configKey = "wero-api")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface WeroApiClient
{

	@POST
	Response createPayment(
		@HeaderParam(HttpHeaders.AUTHORIZATION) String authorization,
		@HeaderParam("Idempotency-Key") String idempotencyKey,
		WeroCreatePaymentRequest request);
}
