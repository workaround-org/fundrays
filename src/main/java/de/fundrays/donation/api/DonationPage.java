package de.fundrays.donation.api;

import de.fundrays.campaign.domain.Campaign;
import de.fundrays.campaign.domain.CampaignStatus;
import de.fundrays.campaign.service.CampaignService;
import de.fundrays.donation.domain.Donation;
import de.fundrays.donation.domain.PaymentMethod;
import de.fundrays.donation.repository.DonationRepository;
import io.quarkiverse.renarde.Controller;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import java.util.List;

@Path("/donate")
public class DonationPage extends Controller
{
	private static final int RECENT_MESSAGES_LIMIT = 5;

	/**
	 * Payment methods offered on the public page. Wero is fully wired up in #6.
	 */
	private static final List<PaymentMethod> ENABLED_PAYMENT_METHODS = List.of(
		PaymentMethod.PAYPAL, PaymentMethod.WERO, PaymentMethod.STRIPE);

	@Inject
	CampaignService campaignService;

	@Inject
	DonationRepository donationRepository;

	@CheckedTemplate
	static class Templates
	{
		static native TemplateInstance index(Campaign campaign, long raised, long count,
			List<Donation> recentMessages, List<PaymentMethod> paymentMethods);

		static native TemplateInstance thanks(Campaign campaign, long raised, long count);
	}

	@GET
	@Path("/{slug}")
	public TemplateInstance index(@PathParam("slug") String slug)
	{
		Campaign campaign = activeCampaignOrNull(slug);
		notFoundIfNull(campaign);
		long raised = campaignService.getRaisedAmount(campaign.id);
		long count = campaignService.getDonationCount(campaign.id);
		List<Donation> recentMessages = donationRepository
			.listRecentConfirmedMessagesByCampaignId(campaign.id, RECENT_MESSAGES_LIMIT);
		return Templates.index(campaign, raised, count, recentMessages, ENABLED_PAYMENT_METHODS);
	}

	@GET
	@Path("/{slug}/thanks")
	public TemplateInstance thanks(@PathParam("slug") String slug)
	{
		Campaign campaign = activeCampaignOrNull(slug);
		notFoundIfNull(campaign);
		long raised = campaignService.getRaisedAmount(campaign.id);
		long count = campaignService.getDonationCount(campaign.id);
		return Templates.thanks(campaign, raised, count);
	}

	private Campaign activeCampaignOrNull(String slug)
	{
		return campaignService.findBySlug(slug)
			.filter(c -> c.status == CampaignStatus.ACTIVE)
			.orElse(null);
	}
}
