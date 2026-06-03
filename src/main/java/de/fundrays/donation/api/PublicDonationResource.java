package de.fundrays.donation.api;

import de.fundrays.campaign.service.CampaignNotActiveException;
import de.fundrays.campaign.service.CampaignNotFoundException;
import de.fundrays.donation.domain.Donation;
import de.fundrays.donation.service.DonationSubmission;
import de.fundrays.donation.service.DonationService;
import de.fundrays.donation.service.PaymentMethodUnavailableException;
import de.fundrays.payment.wero.WeroGatewayException;
import de.fundrays.shared.BadGatewayException;
import de.fundrays.shared.UnprocessableEntityException;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

@Path("/api/public/campaigns/{slug}/donate")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PublicDonationResource
{
	@Inject
	DonationService donationService;

	@POST
	public DonateResponse donate(
		@PathParam("slug") String slug,
		@Valid PublicDonateRequest request,
		@Context UriInfo uriInfo)
	{
		Donation donation = new Donation();
		donation.amount = request.amount();
		donation.paymentMethod = request.paymentMethod();
		donation.donorName = request.donorName();
		donation.donorEmail = request.donorEmail();
		donation.message = request.message();

		try
		{
			DonationSubmission submission = donationService.submit(
				slug,
				donation,
				uriInfo.getBaseUriBuilder().path("donate").path(slug).path("thanks").build(),
				uriInfo.getBaseUriBuilder().path("webhooks").path("wero").build());
			return toResponse(submission);
		}
		catch (CampaignNotFoundException e)
		{
			throw new NotFoundException(e.getMessage());
		}
		catch (CampaignNotActiveException e)
		{
			throw new UnprocessableEntityException(e.getMessage());
		}
		catch (PaymentMethodUnavailableException e)
		{
			throw new UnprocessableEntityException(e.getMessage());
		}
		catch (WeroGatewayException e)
		{
			throw new BadGatewayException("Wero payment could not be initiated");
		}
	}

	private DonateResponse toResponse(DonationSubmission submission)
	{
		Donation d = submission.donation();
		return new DonateResponse(
			d.id,
			d.amount,
			d.currency,
			d.status,
			d.createdAt,
			submission.paymentUrl(),
			submission.paymentDeepLink(),
			submission.paymentQrPayload());
	}
}
