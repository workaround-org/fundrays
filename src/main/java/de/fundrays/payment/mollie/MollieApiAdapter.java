package de.fundrays.payment.mollie;

import com.mollie.mollie.Client;
import com.mollie.mollie.models.components.Amount;
import com.mollie.mollie.models.components.PaymentRequest;
import com.mollie.mollie.models.components.PaymentResponseStatus;
import com.mollie.mollie.models.errors.ErrorResponse;
import com.mollie.mollie.models.operations.GetPaymentRequest;
import de.fundrays.donation.domain.Donation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;

@ApplicationScoped
public class MollieApiAdapter
{
	@Inject
	Client mollieClient;

	public PaymentInitiation createPayment(Donation donation, URI returnUrl, URI webhookUrl)
	{
		String amountValue = BigDecimal.valueOf(donation.amount)
			.divide(BigDecimal.valueOf(100))
			.setScale(2, RoundingMode.HALF_UP)
			.toPlainString();

		try
		{
			var res = mollieClient.payments().create()
				.idempotencyKey(donation.id.toString())
				.paymentRequest(PaymentRequest.builder()
					.description(donation.campaign.title)
					.amount(Amount.builder()
						.currency(donation.currency)
						.value(amountValue)
						.build())
					.redirectUrl(returnUrl.toString())
					.webhookUrl(webhookUrl.toString())
					.build())
				.call();

			var payment = res.paymentResponse()
				.orElseThrow(() -> new MollieGatewayException("Mollie did not return a payment object"));

			String checkoutUrl = payment.links().checkout()
				.map(url -> url.href())
				.orElseThrow(() -> new MollieGatewayException("Mollie payment has no checkout URL"));

			return new PaymentInitiation(payment.id(), checkoutUrl);
		}
		catch (ErrorResponse e)
		{
			if (e.code() == 429 || e.code() >= 500)
			{
				throw new MollieTransientGatewayException("Mollie gateway temporarily unavailable: " + e.code());
			}
			throw new MollieGatewayException("Mollie payment creation failed: " + e.getMessage());
		}
		catch (MollieGatewayException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new MollieTransientGatewayException("Mollie request failed: " + e.getMessage());
		}
	}

	public PaymentResponseStatus fetchStatus(String paymentId)
	{
		try
		{
			var res = mollieClient.payments().get()
				.request(GetPaymentRequest.builder()
					.paymentId(paymentId)
					.build())
				.call();

			return res.paymentResponse()
				.map(p -> p.status())
				.orElseThrow(() -> new MollieGatewayException("Mollie payment not found: " + paymentId));
		}
		catch (ErrorResponse e)
		{
			throw new MollieGatewayException("Mollie payment fetch failed: " + e.getMessage());
		}
		catch (MollieGatewayException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new MollieGatewayException("Mollie request failed: " + e.getMessage());
		}
	}
}
