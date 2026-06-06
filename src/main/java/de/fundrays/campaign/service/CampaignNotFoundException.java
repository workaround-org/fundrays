package de.fundrays.campaign.service;

public class CampaignNotFoundException extends RuntimeException
{
	public CampaignNotFoundException(String slug)
	{
		super("Campaign not found: " + slug);
	}
}
