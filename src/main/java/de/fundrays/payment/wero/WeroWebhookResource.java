package de.fundrays.payment.wero;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fundrays.donation.service.DonationProviderRefNotFoundException;
import de.fundrays.donation.service.DonationService;
import de.fundrays.donation.service.DonationStateTransitionException;
import de.fundrays.donation.service.PaymentMethodUnavailableException;
import de.fundrays.shared.ErrorResponse;

import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Locale;

@Path("/webhooks/wero")
@PermitAll
@Consumes(MediaType.APPLICATION_JSON)
public class WeroWebhookResource
{

	@Inject
	WeroWebhookSignatureVerifier signatureVerifier;

	@Inject
	ObjectMapper objectMapper;

	@Inject
	DonationService donationService;

	@POST
	public Response receive(@HeaderParam("X-Wero-Signature") String signature, String payload)
	{
		if (!signatureVerifier.isValid(payload, signature))
		{
			return error(Response.Status.UNAUTHORIZED, "Invalid Wero webhook signature");
		}

		WeroWebhookEvent event;
		try
		{
			event = objectMapper.readValue(payload, WeroWebhookEvent.class);
		}
		catch (JsonProcessingException e)
		{
			return error(Response.Status.BAD_REQUEST, "Invalid Wero webhook payload");
		}
		if (event.transactionId() == null || event.transactionId().isBlank()
			|| event.status() == null || event.status().isBlank())
		{
			return error(Response.Status.BAD_REQUEST, "Wero webhook requires transactionId and status");
		}

		try
		{
			switch (event.status().toUpperCase(Locale.ROOT))
			{
				case "CONFIRMED", "SUCCEEDED", "COMPLETED" -> donationService.confirmByProviderRef(event.transactionId());
				case "FAILED", "DECLINED", "CANCELLED", "CANCELED" -> donationService.failByProviderRef(event.transactionId());
				default -> {
					return error(Response.Status.BAD_REQUEST, "Unsupported Wero webhook status");
				}
			}
			return Response.ok().build();
		}
		catch (DonationProviderRefNotFoundException e)
		{
			return error(Response.Status.NOT_FOUND, e.getMessage());
		}
		catch (DonationStateTransitionException | PaymentMethodUnavailableException e)
		{
			return error(Response.Status.CONFLICT, e.getMessage());
		}
	}

	private static Response error(Response.Status status, String message)
	{
		return Response.status(status)
			.entity(new ErrorResponse(message))
			.type(MediaType.APPLICATION_JSON)
			.build();
	}
}
