package de.fundrays.donation.repository;

import de.fundrays.donation.domain.Donation;
import de.fundrays.donation.domain.DonationStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class DonationRepository implements PanacheRepository<Donation>
{
	public List<Donation> findByCampaignId(UUID campaignId)
	{
		return list("campaign.id", campaignId);
	}

	public List<Donation> findByCampaignIdAndStatus(UUID campaignId, DonationStatus status)
	{
		return list("campaign.id = ?1 and status = ?2", campaignId, status);
	}

	public Optional<Donation> findByProviderRef(String paymentProviderRef)
	{
		return find("paymentProviderRef", paymentProviderRef).firstResultOptional();
	}

	public Optional<Donation> findByIdForUpdate(UUID donationId)
	{
		return find("id", donationId)
			.withLock(LockModeType.PESSIMISTIC_WRITE)
			.firstResultOptional();
	}

	public Optional<Donation> findByProviderRefForUpdate(String paymentProviderRef)
	{
		return find("paymentProviderRef", paymentProviderRef)
			.withLock(LockModeType.PESSIMISTIC_WRITE)
			.firstResultOptional();
	}

	/** Total confirmed amount in cents for a campaign */
	public long sumConfirmedByCampaignId(UUID campaignId)
	{
		Long result = getEntityManager()
			.createQuery("select sum(d.amount) from Donation d where d.campaign.id = ?1 and d.status = ?2", Long.class)
			.setParameter(1, campaignId)
			.setParameter(2, DonationStatus.CONFIRMED)
			.getSingleResult();
		return result == null ? 0L : result;
	}

	public long countConfirmedByCampaignId(UUID campaignId)
	{
		return count("campaign.id = ?1 and status = ?2", campaignId, DonationStatus.CONFIRMED);
	}

	public long sumAllConfirmed()
	{
		Long result = getEntityManager()
			.createQuery("select sum(d.amount) from Donation d where d.status = ?1", Long.class)
			.setParameter(1, DonationStatus.CONFIRMED)
			.getSingleResult();
		return result == null ? 0L : result;
	}

	public List<Donation> listRecentConfirmed(int limit)
	{
		return find("status = ?1 order by createdAt desc", DonationStatus.CONFIRMED)
			.page(0, limit)
			.list();
	}

	/**
	 * Confirmed donations for a campaign that carry a non-blank message, newest
	 * first.
	 */
	public List<Donation> listRecentConfirmedMessagesByCampaignId(UUID campaignId, int limit)
	{
		return find(
			"campaign.id = ?1 and status = ?2 and message is not null and trim(message) <> '' order by createdAt desc",
			campaignId, DonationStatus.CONFIRMED)
				.page(0, limit)
				.list();
	}

	public List<Donation> listAllOrdered()
	{
		return list("order by createdAt desc");
	}
}
