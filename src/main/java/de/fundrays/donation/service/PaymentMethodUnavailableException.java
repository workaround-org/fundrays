package de.fundrays.donation.service;

import de.fundrays.donation.domain.PaymentMethod;

public class PaymentMethodUnavailableException extends RuntimeException
{

	public PaymentMethodUnavailableException(PaymentMethod paymentMethod)
	{
		super("Payment method is not available: " + paymentMethod);
	}
}
