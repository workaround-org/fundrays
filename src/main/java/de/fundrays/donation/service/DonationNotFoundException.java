package de.fundrays.donation.service;

import java.util.UUID;

public class DonationNotFoundException extends RuntimeException
{
	private final UUID donationId;

	public DonationNotFoundException(UUID donationId)
	{
		super("Donation not found: " + donationId);
		this.donationId = donationId;
	}

	public UUID getDonationId()
	{
		return donationId;
	}
}
