package de.fundrays.payment.wero;

import com.fasterxml.jackson.annotation.JsonAlias;

public record WeroWebhookEvent(
	@JsonAlias("paymentProviderRef") String transactionId,
	String status)
{
}
