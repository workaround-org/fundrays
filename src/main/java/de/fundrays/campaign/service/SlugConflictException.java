package de.fundrays.campaign.service;

public class SlugConflictException extends RuntimeException
{
	private final String slug;

	public SlugConflictException(String slug)
	{
		super("Slug already in use: " + slug);
		this.slug = slug;
	}

	public String getSlug()
	{
		return slug;
	}
}
