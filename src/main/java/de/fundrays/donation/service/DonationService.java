package de.fundrays.donation.service;

import de.fundrays.campaign.domain.CampaignStatus;
import de.fundrays.campaign.repository.CampaignRepository;
import de.fundrays.campaign.service.CampaignNotActiveException;
import de.fundrays.campaign.service.CampaignNotFoundException;
import de.fundrays.donation.domain.Donation;
import de.fundrays.donation.domain.DonationStatus;
import de.fundrays.donation.domain.PaymentMethod;
import de.fundrays.donation.repository.DonationRepository;
import de.fundrays.payment.mollie.MollieConfig;
import de.fundrays.payment.mollie.MollieGateway;
import de.fundrays.payment.mollie.PaymentInitiation;
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
	MollieGateway mollieGateway;

	@Inject
	MollieConfig mollieConfig;

	@Transactional
	public DonationSubmission submit(String campaignSlug, Donation donation, URI returnUrl, URI webhookUrl)
	{
		var campaign = campaignRepository.findBySlug(campaignSlug)
			.orElseThrow(() -> new CampaignNotFoundException(campaignSlug));
		if (campaign.status != CampaignStatus.ACTIVE)
		{
			throw new CampaignNotActiveException(campaignSlug);
		}
		if (donation.paymentMethod != PaymentMethod.MOLLIE || !mollieConfig.enabled())
		{
			throw new PaymentMethodUnavailableException(donation.paymentMethod);
		}
		donation.campaign = campaign;
		donation.createdAt = Instant.now();
		donationRepository.persist(donation);
		donationRepository.flush();

		PaymentInitiation payment = mollieGateway.initiate(donation, returnUrl, webhookUrl);
		donation.paymentProviderRef = payment.paymentId();
		return new DonationSubmission(donation, payment.checkoutUrl());
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
		Donation donation = findMollieByProviderRefForUpdate(paymentProviderRef);
		return confirmPending(donation);
	}

	@Transactional
	public Donation failByProviderRef(String paymentProviderRef)
	{
		Donation donation = findMollieByProviderRefForUpdate(paymentProviderRef);
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
	public Donation confirmMollieManually(UUID donationId)
	{
		Donation donation = donationRepository.findByIdForUpdate(donationId)
			.orElseThrow(() -> new DonationNotFoundException(donationId));
		if (donation.paymentMethod != PaymentMethod.MOLLIE)
		{
			throw new PaymentMethodUnavailableException(donation.paymentMethod);
		}
		return confirmPending(donation);
	}

	private Donation findMollieByProviderRefForUpdate(String paymentProviderRef)
	{
		Donation donation = donationRepository.findByProviderRefForUpdate(paymentProviderRef)
			.orElseThrow(() -> new DonationProviderRefNotFoundException(paymentProviderRef));
		if (donation.paymentMethod != PaymentMethod.MOLLIE)
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
