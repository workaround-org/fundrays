package de.fundrays.donation.api;

import de.fundrays.campaign.service.CampaignNotActiveException;
import de.fundrays.campaign.service.CampaignNotFoundException;
import de.fundrays.donation.domain.Donation;
import de.fundrays.donation.service.DonationService;
import de.fundrays.donation.service.DonationSubmission;
import de.fundrays.donation.service.PaymentMethodUnavailableException;
import de.fundrays.payment.mollie.MollieGatewayException;
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
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;

@Path("/api/public/campaigns/{slug}/donate")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PublicDonationResource
{
	@ConfigProperty(name = "fundrays.base-url", defaultValue = "http://localhost:8080/")
	String baseUrl;

	@Inject
	DonationService donationService;

	@POST
	public DonateResponse donate(
		@PathParam("slug") String slug,
		@Valid PublicDonateRequest request)
	{
		Donation donation = new Donation();
		donation.amount = request.amount();
		donation.paymentMethod = request.paymentMethod();
		donation.donorName = request.donorName();
		donation.donorEmail = request.donorEmail();
		donation.message = request.message();

		try
		{
			// Build the redirect URL from the configured public base URL, not
			// from
			// the request URI: behind a proxy / in docker the request host is
			// localhost, and Mollie rejects a localhost redirect URL with HTTP
			// 422.
			//
			// Normalize the base so the URLs are well-formed whether or not the
			// configured value ends in a slash (FUNDRAYS_BASE_URL=https://x.de
			// must not yield https://x.dedonate/...).
			String base = baseUrl.endsWith("/")
				? baseUrl.substring(0, baseUrl.length() - 1)
				: baseUrl;
			DonationSubmission submission = donationService.submit(
				slug,
				donation,
				URI.create(base + "/donate/" + slug + "/thanks"),
				URI.create(base + "/webhooks/mollie"));
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
		catch (MollieGatewayException e)
		{
			throw new BadGatewayException("Payment could not be initiated");
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
			submission.paymentUrl());
	}
}
