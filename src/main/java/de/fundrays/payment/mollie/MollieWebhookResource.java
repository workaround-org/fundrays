package de.fundrays.payment.mollie;

import com.mollie.mollie.models.components.PaymentResponseStatus;
import de.fundrays.donation.service.DonationProviderRefNotFoundException;
import de.fundrays.donation.service.DonationService;
import de.fundrays.donation.service.DonationStateTransitionException;
import de.fundrays.donation.service.PaymentMethodUnavailableException;
import de.fundrays.shared.ErrorResponse;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/webhooks/mollie")
@PermitAll
@Consumes(MediaType.WILDCARD)
public class MollieWebhookResource
{
	@Inject
	MollieApiAdapter mollieApiAdapter;

	@Inject
	DonationService donationService;

	@POST
	public Response receive(@Context ContainerRequestContext requestContext)
	{
		String paymentId = (String)requestContext.getProperty(MollieWebhookCsrfBypassFilter.PAYMENT_ID_KEY);
		if (paymentId == null || paymentId.isBlank())
		{
			return error(Response.Status.BAD_REQUEST, "Mollie webhook missing payment id");
		}

		PaymentResponseStatus status;
		try
		{
			status = mollieApiAdapter.fetchStatus(paymentId);
		}
		catch (MollieGatewayException e)
		{
			// Return 5xx so Mollie retries the webhook delivery
			return Response.serverError()
				.entity(new ErrorResponse("Failed to fetch payment status from Mollie"))
				.type(MediaType.APPLICATION_JSON)
				.build();
		}

		try
		{
			if (status == PaymentResponseStatus.PAID)
			{
				donationService.confirmByProviderRef(paymentId);
			}
			else if (status == PaymentResponseStatus.CANCELED
				|| status == PaymentResponseStatus.EXPIRED
				|| status == PaymentResponseStatus.FAILED)
			{
				donationService.failByProviderRef(paymentId);
			}
			// OPEN / PENDING / AUTHORIZED: payment still in progress, nothing
			// to do yet
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
