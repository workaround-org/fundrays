package de.fundrays.donation.api;

import de.fundrays.donation.domain.PaymentMethod;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PublicDonateRequest(
	@Min(value = 500, message = "Mindestbetrag sind 5,00 € (500 Cent)") long amount,
	@NotNull PaymentMethod paymentMethod,
	String donorName,
	String donorEmail,
	String message)
{
}
