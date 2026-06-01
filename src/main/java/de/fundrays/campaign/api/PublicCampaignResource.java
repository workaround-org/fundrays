package de.fundrays.campaign.api;

import de.fundrays.campaign.domain.Campaign;
import de.fundrays.campaign.domain.CampaignStatus;
import de.fundrays.campaign.service.CampaignService;
import de.fundrays.donation.repository.DonationRepository;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/api/public/campaigns")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PublicCampaignResource
{
	private static final int RECENT_MESSAGES_LIMIT = 5;

	@Inject
	CampaignService campaignService;

	@Inject
	DonationRepository donationRepository;

	@GET
	@Path("/{slug}")
	public PublicCampaignResponse getBySlug(@PathParam("slug") String slug)
	{
		Campaign campaign = findActiveOr404(slug);
		return new PublicCampaignResponse(
			campaign.slug, campaign.title, campaign.description,
			campaign.goalAmount, campaign.currency, campaign.status, campaign.coverImageUrl);
	}

	@GET
	@Path("/{slug}/progress")
	public ProgressResponse progress(@PathParam("slug") String slug)
	{
		Campaign campaign = findActiveOr404(slug);
		long raised = campaignService.getRaisedAmount(campaign.id);
		long count = campaignService.getDonationCount(campaign.id);
		List<ProgressResponse.Message> messages = donationRepository
			.listRecentConfirmedMessagesByCampaignId(campaign.id, RECENT_MESSAGES_LIMIT).stream()
			.map(d -> new ProgressResponse.Message(d.donorName, d.message, d.createdAt))
			.toList();
		return new ProgressResponse(raised, campaign.goalAmount, percentage(raised, campaign.goalAmount), count, messages);
	}

	private Campaign findActiveOr404(String slug)
	{
		return campaignService.findBySlug(slug)
			.filter(c -> c.status == CampaignStatus.ACTIVE)
			.orElseThrow(() -> new NotFoundException("Campaign not found: " + slug));
	}

	private static int percentage(long raised, long goal)
	{
		if (goal <= 0) return 0;
		return (int)Math.min(100L, raised * 100L / goal);
	}
}
