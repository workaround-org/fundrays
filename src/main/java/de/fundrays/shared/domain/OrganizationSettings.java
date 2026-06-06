package de.fundrays.shared.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.LocalDate;

/**
 * Singleton row — always id=1. Use OrganizationSettingsRepository.load() to
 * retrieve.
 */
@Entity
public class OrganizationSettings
{
	@Id
	public Long id = 1L;

	public String orgName;
	public String orgStreet;
	public String orgZip;
	public String orgCity;

	/** Steuernummer or Freistellungsbescheid number */
	public String orgTaxId;
	public String orgIssuingAuthority;
	public LocalDate orgExemptionDate;

	/** Satzungsmäßiger Zweck — used in Zuwendungsbestätigung */
	@Column(columnDefinition = "TEXT")
	public String orgPurpose;

	public String smtpHost;
	public Integer smtpPort;
	public String smtpUser;

	/** Stored encrypted — encryption handled at service layer */
	public String smtpPassword;

	public String smtpFrom;
	public String adminNotificationEmail;
}
