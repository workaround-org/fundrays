package de.fundrays.donation.service;

public class DonationProviderRefNotFoundException extends RuntimeException
{

	public DonationProviderRefNotFoundException(String paymentProviderRef)
	{
		super("Donation not found for payment provider reference: " + paymentProviderRef);
	}
}
