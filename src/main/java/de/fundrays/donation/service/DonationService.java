package de.fundrays.donation.service;

import de.fundrays.campaign.domain.CampaignStatus;
import de.fundrays.campaign.repository.CampaignRepository;
import de.fundrays.campaign.service.CampaignNotActiveException;
import de.fundrays.campaign.service.CampaignNotFoundException;
import de.fundrays.donation.domain.Donation;
import de.fundrays.donation.domain.DonationStatus;
import de.fundrays.donation.repository.DonationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
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

	@Transactional
	public Donation submit(String campaignSlug, Donation donation)
	{
		var campaign = campaignRepository.findBySlug(campaignSlug)
			.orElseThrow(() -> new CampaignNotFoundException(campaignSlug));
		if (campaign.status != CampaignStatus.ACTIVE)
		{
			throw new CampaignNotActiveException(campaignSlug);
		}
		donation.campaign = campaign;
		donation.createdAt = Instant.now();
		donationRepository.persist(donation);
		return donation;
	}

	/**
	 * Transition a donation to CONFIRMED and fire the donor + admin mails.
	 * Idempotent — re-confirming a CONFIRMED donation is a no-op (no duplicate
	 * mails).
	 */
	@Transactional
	public Donation confirm(UUID donationId)
	{
		Donation donation = donationRepository.find("id", donationId).firstResult();
		if (donation == null)
		{
			throw new DonationNotFoundException(donationId);
		}
		if (donation.status == DonationStatus.CONFIRMED)
		{
			return donation;
		}
		donation.status = DonationStatus.CONFIRMED;
		donation.confirmedAt = Instant.now();

		confirmationMailer.sendConfirmation(donation);
		confirmationMailer.sendAdminNotification(donation);
		return donation;
	}
}
