package de.fundrays.campaign.repository;

import de.fundrays.campaign.domain.Campaign;
import de.fundrays.campaign.domain.CampaignStatus;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestTransaction
class CampaignRepositoryTest
{
	@Inject
	CampaignRepository campaignRepository;

	@Test
	void findBySlug_returnsMatchingCampaign()
	{
		// given
		campaignRepository.persist(aCampaign("spring-drive", CampaignStatus.ACTIVE));

		// when
		Optional<Campaign> result = campaignRepository.findBySlug("spring-drive");

		// then
		assertTrue(result.isPresent());
		assertEquals("spring-drive", result.get().slug);
	}

	@Test
	void findBySlug_returnsEmptyForUnknownSlug()
	{
		// given — no campaigns in DB

		// when
		Optional<Campaign> result = campaignRepository.findBySlug("nonexistent");

		// then
		assertTrue(result.isEmpty());
	}

	@Test
	void findActive_returnsOnlyActiveCampaigns()
	{
		// given
		long ts = System.nanoTime();
		String activeSlug = "repo-active-" + ts;
		campaignRepository.persist(aCampaign(activeSlug, CampaignStatus.ACTIVE));
		campaignRepository.persist(aCampaign("repo-paused-" + ts, CampaignStatus.PAUSED));
		campaignRepository.persist(aCampaign("repo-archived-" + ts, CampaignStatus.ARCHIVED));

		// when
		List<Campaign> active = campaignRepository.findActive();

		// then
		assertTrue(active.stream().anyMatch(c -> c.slug.equals(activeSlug)));
		assertTrue(active.stream().noneMatch(c -> c.slug.equals("repo-paused-" + ts)));
	}

	@Test
	void findByStatus_returnsAllMatchingStatus()
	{
		// given
		long ts = System.nanoTime();
		campaignRepository.persist(aCampaign("repo-completed-a-" + ts, CampaignStatus.COMPLETED));
		campaignRepository.persist(aCampaign("repo-completed-b-" + ts, CampaignStatus.COMPLETED));
		campaignRepository.persist(aCampaign("repo-active-b-" + ts, CampaignStatus.ACTIVE));

		// when
		List<Campaign> result = campaignRepository.findByStatus(CampaignStatus.COMPLETED);

		// then
		assertTrue(result.stream().anyMatch(c -> c.slug.equals("repo-completed-a-" + ts)));
		assertTrue(result.stream().anyMatch(c -> c.slug.equals("repo-completed-b-" + ts)));
	}

	private Campaign aCampaign(String slug, CampaignStatus status)
	{
		Campaign c = new Campaign();
		c.slug = slug;
		c.title = "Campaign " + slug;
		c.goalAmount = 10000L;
		c.createdAt = Instant.now();
		c.status = status;
		return c;
	}
}
