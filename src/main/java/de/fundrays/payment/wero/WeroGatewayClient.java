package de.fundrays.payment.wero;

import de.fundrays.donation.domain.Donation;
import io.smallrye.faulttolerance.api.ExponentialBackoff;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.time.temporal.ChronoUnit;

@ApplicationScoped
public class WeroGatewayClient implements WeroGateway
{

	@Inject
	@RestClient
	WeroApiClient client;

	@Inject
	WeroConfig config;

	@Override
	@Retry(maxRetries = 2, delay = 200, delayUnit = ChronoUnit.MILLIS, retryOn = WeroTransientGatewayException.class)
	@ExponentialBackoff(factor = 2)
	public WeroPaymentInitiation initiate(Donation donation, URI returnUrl, URI webhookUrl)
	{
		if (config.apiKey() == null || config.apiKey().isBlank())
		{
			throw new WeroGatewayException("Wero API key is not configured");
		}

		WeroCreatePaymentRequest request = new WeroCreatePaymentRequest(
			donation.id.toString(),
			donation.amount,
			donation.currency,
			returnUrl.toString(),
			webhookUrl.toString());

		try (Response response = client.createPayment(
			"Bearer " + config.apiKey(),
			donation.id.toString(),
			request))
		{
			if (response.getStatus() == 429 || response.getStatus() >= 500)
			{
				throw new WeroTransientGatewayException("Wero gateway temporarily unavailable");
			}
			if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL)
			{
				throw new WeroGatewayException("Wero gateway rejected payment request");
			}

			WeroCreatePaymentResponse body = response.readEntity(WeroCreatePaymentResponse.class);
			if (body == null || body.transactionId() == null || body.transactionId().isBlank())
			{
				throw new WeroGatewayException("Wero gateway response is missing transactionId");
			}
			WeroPaymentInitiation initiation = new WeroPaymentInitiation(
				body.transactionId(), body.paymentUrl(), body.deepLink(), body.qrPayload());
			if (initiation.redirectUrl() == null || initiation.redirectUrl().isBlank())
			{
				throw new WeroGatewayException("Wero gateway response is missing paymentUrl or deepLink");
			}
			return initiation;
		}
		catch (ProcessingException e)
		{
			throw new WeroTransientGatewayException("Wero gateway request failed");
		}
	}
}
