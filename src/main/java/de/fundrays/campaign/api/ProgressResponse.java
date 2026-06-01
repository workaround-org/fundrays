package de.fundrays.campaign.api;

import java.time.Instant;
import java.util.List;

public record ProgressResponse(
	long raisedAmount,
	long goalAmount,
	int percentage,
	long donationCount,
	List<Message> recentMessages)
{
	public record Message(String donorName, String message, Instant createdAt)
	{
	}
}
