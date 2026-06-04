package de.fundrays.donation.api;

import de.fundrays.campaign.domain.Campaign;
import de.fundrays.campaign.repository.CampaignRepository;
import de.fundrays.donation.domain.Donation;
import de.fundrays.donation.repository.DonationRepository;
import de.fundrays.donation.service.DonationNotFoundException;
import de.fundrays.donation.service.DonationService;
import de.fundrays.donation.service.DonationStateTransitionException;
import de.fundrays.donation.service.PaymentMethodUnavailableException;
import io.quarkiverse.renarde.Controller;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import org.jboss.resteasy.reactive.RestPath;

import java.util.List;
import java.util.UUID;

@Path("/admin/donations")
@RolesAllowed("admin")
public class AdminDonations extends Controller
{
	@Inject
	DonationRepository donationRepository;

	@Inject
	CampaignRepository campaignRepository;

	@Inject
	DonationService donationService;

	@CheckedTemplate
	static class Templates
	{
		private Templates()
		{
			/* This utility class should not be instantiated */
		}

		static native TemplateInstance index(List<Donation> donations, Campaign filterCampaign);
	}

	@GET
	@Path("/")
	public TemplateInstance index(@QueryParam("campaign") String campaignSlug)
	{
		if (campaignSlug != null && !campaignSlug.isBlank())
		{
			Campaign campaign = campaignRepository.findBySlug(campaignSlug).orElse(null);
			if (campaign != null)
			{
				List<Donation> donations = donationRepository.findByCampaignId(campaign.id);
				return Templates.index(donations, campaign);
			}
		}
		return Templates.index(donationRepository.listAllOrdered(), null);
	}

	@POST
	@Path("/{id}/confirm")
	public void confirm(@RestPath UUID id)
	{
		try
		{
			donationService.confirmWeroManually(id);
			flash("message", "Wero-Spende manuell bestätigt.");
		}
		catch (DonationNotFoundException | DonationStateTransitionException | PaymentMethodUnavailableException e)
		{
			flash("error", e.getMessage());
		}
		index(null);
	}
}
