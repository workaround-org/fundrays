package de.fundrays.campaign.api;

import de.fundrays.campaign.domain.CampaignStatus;

public record PublicCampaignResponse(
	String slug,
	String title,
	String description,
	long goalAmount,
	String currency,
	CampaignStatus status,
	String coverImageUrl)
{
}
