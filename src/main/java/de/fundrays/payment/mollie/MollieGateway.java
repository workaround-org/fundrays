package de.fundrays.payment.mollie;

import de.fundrays.donation.domain.Donation;

import java.net.URI;

public interface MollieGateway
{
	PaymentInitiation initiate(Donation donation, URI returnUrl, URI webhookUrl);
}
