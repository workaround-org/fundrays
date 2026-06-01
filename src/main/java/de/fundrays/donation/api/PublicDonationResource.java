package de.fundrays.donation.api;

import de.fundrays.campaign.service.CampaignNotActiveException;
import de.fundrays.campaign.service.CampaignNotFoundException;
import de.fundrays.donation.domain.Donation;
import de.fundrays.donation.service.DonationService;
import de.fundrays.shared.UnprocessableEntityException;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/public/campaigns/{slug}/donate")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PublicDonationResource
{
	@Inject
	DonationService donationService;

	@POST
	public DonateResponse donate(@PathParam("slug") String slug, @Valid PublicDonateRequest request)
	{
		Donation donation = new Donation();
		donation.amount = request.amount();
		donation.paymentMethod = request.paymentMethod();
		donation.donorName = request.donorName();
		donation.donorEmail = request.donorEmail();
		donation.message = request.message();

		try
		{
			Donation saved = donationService.submit(slug, donation);
			return toResponse(slug, saved);
		}
		catch (CampaignNotFoundException e)
		{
			throw new NotFoundException(e.getMessage());
		}
		catch (CampaignNotActiveException e)
		{
			throw new UnprocessableEntityException(e.getMessage());
		}
	}

	private DonateResponse toResponse(String slug, Donation d)
	{
		// Placeholder until real payment-provider URLs land (#6): redirect to
		// the thank-you page.
		String paymentUrl = "/donate/" + slug + "/thanks?donation=" + d.id;
		return new DonateResponse(d.id, d.amount, d.currency, d.status, d.createdAt, paymentUrl);
	}
}
