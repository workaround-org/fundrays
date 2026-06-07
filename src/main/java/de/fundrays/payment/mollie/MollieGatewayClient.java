package de.fundrays.payment.mollie;

import de.fundrays.donation.domain.Donation;
import io.smallrye.faulttolerance.api.ExponentialBackoff;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.faulttolerance.Retry;

import java.net.URI;
import java.time.temporal.ChronoUnit;

@ApplicationScoped
public class MollieGatewayClient implements MollieGateway
{
	@Inject
	MollieApiAdapter mollieApiAdapter;

	@Override
	@Retry(maxRetries = 2, delay = 200, delayUnit = ChronoUnit.MILLIS, retryOn = MollieTransientGatewayException.class)
	@ExponentialBackoff(factor = 2)
	public PaymentInitiation initiate(Donation donation, URI returnUrl, URI webhookUrl)
	{
		if (donation.id == null)
		{
			throw new MollieGatewayException("Donation must be persisted before initiating payment");
		}
		return mollieApiAdapter.createPayment(donation, returnUrl, webhookUrl);
	}
}
