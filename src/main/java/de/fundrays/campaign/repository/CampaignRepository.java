package de.fundrays.campaign.repository;

import de.fundrays.campaign.domain.Campaign;
import de.fundrays.campaign.domain.CampaignStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class CampaignRepository implements PanacheRepository<Campaign>
{
	public Optional<Campaign> findBySlug(String slug)
	{
		return find("slug", slug).firstResultOptional();
	}

	public List<Campaign> findByStatus(CampaignStatus status)
	{
		return list("status", status);
	}

	public List<Campaign> findActive()
	{
		return findByStatus(CampaignStatus.ACTIVE);
	}

	public List<Campaign> listAllOrdered()
	{
		return list("order by createdAt desc");
	}
}
