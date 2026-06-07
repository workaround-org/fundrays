package de.fundrays.donation.service;

import de.fundrays.donation.domain.Donation;

public record DonationSubmission(Donation donation, String paymentUrl)
{
}
