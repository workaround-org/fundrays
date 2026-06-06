package de.fundrays.shared.service;

import de.fundrays.shared.domain.OrganizationSettings;
import de.fundrays.shared.repository.OrganizationSettingsRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class OrganizationSettingsService
{
	@Inject
	OrganizationSettingsRepository settingsRepository;

	@Transactional
	public OrganizationSettings loadOrCreate()
	{
		return settingsRepository.load().orElseGet(() -> {
			OrganizationSettings created = new OrganizationSettings();
			settingsRepository.persist(created);
			return created;
		});
	}

	/**
	 * Applies field updates from the form. A blank or null
	 * {@code newSmtpPassword} keeps the stored password — the form never echoes
	 * the secret back.
	 */
	@Transactional
	public OrganizationSettings update(OrganizationSettings updates, String newSmtpPassword)
	{
		OrganizationSettings current = loadOrCreate();

		current.orgName = updates.orgName;
		current.orgStreet = updates.orgStreet;
		current.orgZip = updates.orgZip;
		current.orgCity = updates.orgCity;
		current.orgTaxId = updates.orgTaxId;
		current.orgIssuingAuthority = updates.orgIssuingAuthority;
		current.orgExemptionDate = updates.orgExemptionDate;
		current.orgPurpose = updates.orgPurpose;

		current.smtpHost = updates.smtpHost;
		current.smtpPort = updates.smtpPort;
		current.smtpUser = updates.smtpUser;
		current.smtpFrom = updates.smtpFrom;
		current.adminNotificationEmail = updates.adminNotificationEmail;

		if (newSmtpPassword != null && !newSmtpPassword.isBlank())
		{
			current.smtpPassword = newSmtpPassword;
		}

		return current;
	}
}
