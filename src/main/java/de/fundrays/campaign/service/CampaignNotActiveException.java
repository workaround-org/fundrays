package de.fundrays.campaign.service;

public class CampaignNotActiveException extends RuntimeException
{
	public CampaignNotActiveException(String slug)
	{
		super("Campaign is not accepting donations: " + slug);
	}
}
