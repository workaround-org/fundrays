package de.fundrays.donation.service;

import de.fundrays.campaign.domain.CampaignStatus;
import de.fundrays.campaign.repository.CampaignRepository;
import de.fundrays.campaign.service.CampaignNotActiveException;
import de.fundrays.campaign.service.CampaignNotFoundException;
import de.fundrays.donation.domain.Donation;
import de.fundrays.donation.domain.DonationStatus;
import de.fundrays.donation.domain.PaymentMethod;
import de.fundrays.donation.repository.DonationRepository;
import de.fundrays.payment.wero.WeroConfig;
import de.fundrays.payment.wero.WeroGateway;
import de.fundrays.payment.wero.WeroPaymentInitiation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class DonationService
{
	@Inject
	CampaignRepository campaignRepository;

	@Inject
	DonationRepository donationRepository;

	@Inject
	DonationConfirmationMailer confirmationMailer;

	@Inject
	WeroGateway weroGateway;

	@Inject
	WeroConfig weroConfig;

	@Transactional
	public DonationSubmission submit(String campaignSlug, Donation donation, URI returnUrl, URI webhookUrl)
	{
		var campaign = campaignRepository.findBySlug(campaignSlug)
			.orElseThrow(() -> new CampaignNotFoundException(campaignSlug));
		if (campaign.status != CampaignStatus.ACTIVE)
		{
			throw new CampaignNotActiveException(campaignSlug);
		}
		if (donation.paymentMethod != PaymentMethod.WERO || !weroConfig.enabled())
		{
			throw new PaymentMethodUnavailableException(donation.paymentMethod);
		}
		donation.campaign = campaign;
		donation.createdAt = Instant.now();
		donationRepository.persist(donation);
		donationRepository.flush();

		WeroPaymentInitiation payment = weroGateway.initiate(donation, returnUrl, webhookUrl);
		donation.paymentProviderRef = payment.transactionId();
		return new DonationSubmission(
			donation,
			payment.redirectUrl(),
			payment.deepLink(),
			payment.qrPayload());
	}

	/**
	 * Transition a donation to CONFIRMED and fire the donor + admin mails.
	 * Idempotent — re-confirming a CONFIRMED donation is a no-op (no duplicate
	 * mails).
	 */
	@Transactional
	public Donation confirm(UUID donationId)
	{
		Donation donation = donationRepository.findByIdForUpdate(donationId)
			.orElseThrow(() -> new DonationNotFoundException(donationId));
		return confirmPending(donation);
	}

	@Transactional
	public Donation confirmByProviderRef(String paymentProviderRef)
	{
		Donation donation = findWeroByProviderRefForUpdate(paymentProviderRef);
		return confirmPending(donation);
	}

	@Transactional
	public Donation failByProviderRef(String paymentProviderRef)
	{
		Donation donation = findWeroByProviderRefForUpdate(paymentProviderRef);
		if (donation.status == DonationStatus.FAILED || donation.status == DonationStatus.CONFIRMED)
		{
			return donation;
		}
		if (donation.status != DonationStatus.PENDING)
		{
			throw new DonationStateTransitionException(donation, DonationStatus.FAILED);
		}
		donation.status = DonationStatus.FAILED;
		return donation;
	}

	@Transactional
	public Donation confirmWeroManually(UUID donationId)
	{
		Donation donation = donationRepository.findByIdForUpdate(donationId)
			.orElseThrow(() -> new DonationNotFoundException(donationId));
		if (donation.paymentMethod != PaymentMethod.WERO)
		{
			throw new PaymentMethodUnavailableException(donation.paymentMethod);
		}
		return confirmPending(donation);
	}

	private Donation findWeroByProviderRefForUpdate(String paymentProviderRef)
	{
		Donation donation = donationRepository.findByProviderRefForUpdate(paymentProviderRef)
			.orElseThrow(() -> new DonationProviderRefNotFoundException(paymentProviderRef));
		if (donation.paymentMethod != PaymentMethod.WERO)
		{
			throw new PaymentMethodUnavailableException(donation.paymentMethod);
		}
		return donation;
	}

	private Donation confirmPending(Donation donation)
	{
		if (donation.status == DonationStatus.CONFIRMED)
		{
			return donation;
		}
		if (donation.status != DonationStatus.PENDING)
		{
			throw new DonationStateTransitionException(donation, DonationStatus.CONFIRMED);
		}
		donation.status = DonationStatus.CONFIRMED;
		donation.confirmedAt = Instant.now();

		confirmationMailer.sendConfirmation(donation);
		confirmationMailer.sendAdminNotification(donation);
		return donation;
	}
}
