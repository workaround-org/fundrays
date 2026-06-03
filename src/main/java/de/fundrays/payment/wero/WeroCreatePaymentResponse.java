package de.fundrays.payment.wero;

public record WeroCreatePaymentResponse(
	String transactionId,
	String paymentUrl,
	String deepLink,
	String qrPayload)
{
}
