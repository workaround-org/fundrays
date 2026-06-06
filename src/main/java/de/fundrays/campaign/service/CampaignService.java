package de.fundrays.campaign.service;

import de.fundrays.campaign.domain.Campaign;
import de.fundrays.campaign.repository.CampaignRepository;
import de.fundrays.donation.repository.DonationRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

@ApplicationScoped
public class CampaignService
{
	@Inject
	CampaignRepository campaignRepository;

	@Inject
	DonationRepository donationRepository;

	@Transactional(Transactional.TxType.SUPPORTS)
	public List<Campaign> findActive()
	{
		return campaignRepository.findActive();
	}

	@Transactional(Transactional.TxType.SUPPORTS)
	public Optional<Campaign> findBySlug(String slug)
	{
		return campaignRepository.findBySlug(slug);
	}

	@Transactional(Transactional.TxType.SUPPORTS)
	public long getRaisedAmount(UUID campaignId)
	{
		return donationRepository.sumConfirmedByCampaignId(campaignId);
	}

	@Transactional(Transactional.TxType.SUPPORTS)
	public long getDonationCount(UUID campaignId)
	{
		return donationRepository.countConfirmedByCampaignId(campaignId);
	}

	@Transactional
	public Campaign update(String slug, Consumer<Campaign> updater)
	{
		Campaign campaign = campaignRepository.findBySlug(slug)
			.orElseThrow(() -> new CampaignNotFoundException(slug));
		updater.accept(campaign);
		return campaign;
	}

	@Transactional
	public Campaign create(Campaign campaign)
	{
		if (campaignRepository.findBySlug(campaign.slug).isPresent())
		{
			throw new SlugConflictException(campaign.slug);
		}
		campaign.createdAt = Instant.now();
		try
		{
			campaignRepository.persist(campaign);
			campaignRepository.flush();
		}
		catch (PersistenceException e)
		{
			// DB unique constraint violated by concurrent insert with same slug
			throw new SlugConflictException(campaign.slug);
		}
		return campaign;
	}
}
