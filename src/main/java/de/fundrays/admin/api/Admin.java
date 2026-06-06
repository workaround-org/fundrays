package de.fundrays.admin.api;

import de.fundrays.campaign.repository.CampaignRepository;
import de.fundrays.donation.domain.Donation;
import de.fundrays.donation.repository.DonationRepository;
import io.quarkiverse.renarde.Controller;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.vertx.ext.web.RoutingContext;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.util.List;

@Path("/admin")
@RolesAllowed("admin")
public class Admin extends Controller
{
	@Inject
	CampaignRepository campaignRepository;

	@Inject
	DonationRepository donationRepository;

	@Inject
	RoutingContext routingContext;

	@CheckedTemplate
	static class Templates
	{
		private Templates()
		{
			/* This utility class should not be instantiated */
		}

		static native TemplateInstance index(
			long totalCampaigns,
			long activeCampaigns,
			long totalDonations,
			long totalRaised,
			List<Donation> recentDonations);
	}

	@GET
	@Path("/")
	public TemplateInstance index()
	{
		long totalCampaigns = campaignRepository.count();
		long activeCampaigns = campaignRepository.findActive().size();
		long totalDonations = donationRepository.count();
		long totalRaised = donationRepository.sumAllConfirmed();
		List<Donation> recentDonations = donationRepository.listRecentConfirmed(5);
		return Templates.index(totalCampaigns, activeCampaigns, totalDonations, totalRaised, recentDonations);
	}

	@POST
	@Path("/logout")
	public Response logout()
	{
		var session = routingContext.session();
		if (session != null)
		{
			session.destroy();
		}
		return Response.seeOther(URI.create("/login")).build();
	}
}
