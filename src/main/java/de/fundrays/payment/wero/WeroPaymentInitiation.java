package de.fundrays.payment.wero;

public record WeroPaymentInitiation(
	String transactionId,
	String paymentUrl,
	String deepLink,
	String qrPayload)
{

	public String redirectUrl()
	{
		return paymentUrl != null && !paymentUrl.isBlank() ? paymentUrl : deepLink;
	}
}
