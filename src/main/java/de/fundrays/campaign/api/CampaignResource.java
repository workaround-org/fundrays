package de.fundrays.campaign.api;

import de.fundrays.campaign.domain.Campaign;
import de.fundrays.campaign.service.CampaignNotFoundException;
import de.fundrays.campaign.service.CampaignService;
import de.fundrays.campaign.service.QrCodeService;
import de.fundrays.campaign.service.SlugConflictException;
import de.fundrays.shared.ConflictException;
import org.jboss.resteasy.reactive.ResponseStatus;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Path("/api/campaigns")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CampaignResource
{
	private static final int QR_CODE_SIZE_PX = 512;

	@Inject
	CampaignService campaignService;

	@Inject
	QrCodeService qrCodeService;

	@GET
	public List<CampaignResponse> listActive()
	{
		return campaignService.findActive().stream()
			.map(this::toResponse)
			.toList();
	}

	@GET
	@Path("/{slug}")
	public CampaignResponse getBySlug(@PathParam("slug") String slug)
	{
		return campaignService.findBySlug(slug)
			.map(this::toResponse)
			.orElseThrow(() -> new NotFoundException("Campaign not found: " + slug));
	}

	@PATCH
	@Path("/{slug}")
	@RolesAllowed("admin")
	public CampaignResponse update(@PathParam("slug") String slug, @Valid UpdateCampaignRequest request)
	{
		try
		{
			return toResponse(campaignService.update(slug, c -> {
				if (request.title() != null) c.title = request.title();
				if (request.description() != null) c.description = request.description();
				if (request.goalAmount() != null) c.goalAmount = request.goalAmount();
				if (request.currency() != null) c.currency = request.currency();
				if (request.deadline() != null) c.deadline = request.deadline();
				if (request.coverImageUrl() != null) c.coverImageUrl = request.coverImageUrl();
				if (request.status() != null) c.status = request.status();
			}));
		}
		catch (CampaignNotFoundException e)
		{
			throw new NotFoundException(e.getMessage());
		}
	}

	@POST
	@ResponseStatus(201)
	@RolesAllowed("admin")
	public CampaignResponse create(@Valid CreateCampaignRequest request)
	{
		Campaign campaign = new Campaign();
		campaign.slug = request.slug();
		campaign.title = request.title();
		campaign.description = request.description();
		campaign.goalAmount = request.goalAmount();
		if (request.currency() != null)
		{
			campaign.currency = request.currency();
		}
		campaign.deadline = request.deadline();
		campaign.coverImageUrl = request.coverImageUrl();

		try
		{
			return toResponse(campaignService.create(campaign));
		}
		catch (SlugConflictException e)
		{
			throw new ConflictException(e.getMessage());
		}
	}

	@GET
	@Path("/{slug}/qrcode")
	@RolesAllowed("admin")
	@Produces({ "image/png", "image/svg+xml" })
	public Response qrcode(@PathParam("slug") String slug,
		@QueryParam("format") @DefaultValue("png") String format,
		@Context UriInfo uriInfo)
	{
		campaignService.findBySlug(slug)
			.orElseThrow(() -> new NotFoundException("Campaign not found: " + slug));

		String donationUrl = buildDonationUrl(uriInfo, slug);

		return switch (format)
		{
			case "png" -> Response.ok(qrCodeService.renderPng(donationUrl, QR_CODE_SIZE_PX))
				.type("image/png")
				.build();
			case "svg" -> Response.ok(qrCodeService.renderSvg(donationUrl, QR_CODE_SIZE_PX).getBytes(StandardCharsets.UTF_8))
				.type("image/svg+xml")
				.build();
			default -> throw new BadRequestException("Unsupported format: " + format + " (expected 'png' or 'svg')");
		};
	}

	private String buildDonationUrl(UriInfo uriInfo, String slug)
	{
		UriBuilder builder = uriInfo.getBaseUriBuilder().replacePath("/donate/" + slug);
		for (Map.Entry<String, List<String>> entry : uriInfo.getQueryParameters().entrySet())
		{
			if (entry.getKey().startsWith("utm_"))
			{
				builder.queryParam(entry.getKey(), entry.getValue().toArray());
			}
		}
		return builder.build().toString();
	}

	private CampaignResponse toResponse(Campaign c)
	{
		return new CampaignResponse(
			c.id, c.slug, c.title, c.description,
			c.goalAmount, c.currency, c.deadline, c.createdAt,
			c.status, c.coverImageUrl,
			campaignService.getRaisedAmount(c.id),
			campaignService.getDonationCount(c.id));
	}
}
