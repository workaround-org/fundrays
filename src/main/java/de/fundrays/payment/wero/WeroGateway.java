package de.fundrays.payment.wero;

import de.fundrays.donation.domain.Donation;

import java.net.URI;

public interface WeroGateway
{

	WeroPaymentInitiation initiate(Donation donation, URI returnUrl, URI webhookUrl);
}
