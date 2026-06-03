package de.fundrays.donation.service;

import de.fundrays.donation.domain.Donation;
import de.fundrays.donation.domain.DonationStatus;

public class DonationStateTransitionException extends RuntimeException
{

	public DonationStateTransitionException(Donation donation, DonationStatus targetStatus)
	{
		super("Donation " + donation.id + " cannot transition from " + donation.status + " to " + targetStatus);
	}
}
