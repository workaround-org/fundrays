package de.fundrays.payment.wero;

public record WeroCreatePaymentRequest(
	String merchantReference,
	long amount,
	String currency,
	String returnUrl,
	String webhookUrl)
{
}
