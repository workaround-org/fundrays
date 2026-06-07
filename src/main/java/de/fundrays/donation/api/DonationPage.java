package de.fundrays.donation.api;

import de.fundrays.campaign.domain.Campaign;
import de.fundrays.campaign.domain.CampaignStatus;
import de.fundrays.campaign.service.CampaignService;
import de.fundrays.donation.domain.Donation;
import de.fundrays.donation.domain.PaymentMethod;
import de.fundrays.donation.repository.DonationRepository;
import de.fundrays.payment.mollie.MollieConfig;
import io.quarkiverse.renarde.Controller;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;

@Path("/donate")
public class DonationPage extends Controller
{
	private static final int RECENT_MESSAGES_LIMIT = 5;

	@ConfigProperty(name = "fundrays.base-url", defaultValue = "http://localhost:8080/")
	String baseUrl;

	@Inject
	CampaignService campaignService;

	@Inject
	DonationRepository donationRepository;

	@Inject
	MollieConfig mollieConfig;

	@CheckedTemplate
	static class Templates
	{
		private Templates()
		{
			/* This utility class should not be instantiated */
		}

		static native TemplateInstance index(Campaign campaign, long raised, long count,
			List<Donation> recentMessages, List<PaymentMethod> paymentMethods, String baseUrl);

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
		List<PaymentMethod> paymentMethods = mollieConfig.enabled() ? List.of(PaymentMethod.MOLLIE) : List.of();
		return Templates.index(campaign, raised, count, recentMessages, paymentMethods, baseUrl);
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
