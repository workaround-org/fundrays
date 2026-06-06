package de.fundrays.donation.domain;

import de.fundrays.campaign.domain.Campaign;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import java.time.Instant;
import java.util.UUID;

@Entity
public class Donation
{
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	public UUID id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	public Campaign campaign;

	/** Amount in euro cents */
	@Column(nullable = false)
	public long amount;

	@Column(nullable = false, length = 3)
	public String currency = "EUR";

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	public PaymentMethod paymentMethod;

	/**
	 * Transaction ID from the payment provider — unique to prevent
	 * double-processing
	 */
	@Column(unique = true)
	public String paymentProviderRef;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	public DonationStatus status = DonationStatus.PENDING;

	public String message;
	public String donorName;
	public String donorEmail;

	@Column(nullable = false)
	public Instant createdAt;

	public Instant confirmedAt;

	/** Set after Zuwendungsbestätigung has been sent (donations >= 100 EUR) */
	public Instant receiptSentAt;
}
