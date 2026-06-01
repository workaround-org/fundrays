package de.fundrays.donation.api;

import de.fundrays.donation.domain.DonationStatus;

import java.time.Instant;
import java.util.UUID;

public record DonateResponse(
	UUID id,
	long amount,
	String currency,
	DonationStatus status,
	Instant createdAt,
	String paymentUrl)
{
}
