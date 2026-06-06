package de.fundrays.campaign.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;
import java.util.UUID;

@Entity
public class Campaign
{
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	public UUID id;

	@Column(unique = true, nullable = false)
	public String slug;

	@Column(nullable = false)
	public String title;

	@Column(columnDefinition = "TEXT")
	public String description;

	/** Goal amount in euro cents */
	@Column(nullable = false)
	public long goalAmount;

	@Column(nullable = false, length = 3)
	public String currency = "EUR";

	public Instant deadline;

	@Column(nullable = false)
	public Instant createdAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	public CampaignStatus status = CampaignStatus.ACTIVE;

	public String coverImageUrl;
}
