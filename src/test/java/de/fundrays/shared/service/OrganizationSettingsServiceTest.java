package de.fundrays.shared.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fundrays.shared.domain.OrganizationSettings;
import de.fundrays.shared.repository.OrganizationSettingsRepository;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestTransaction
class OrganizationSettingsServiceTest
{
	@Inject
	OrganizationSettingsService settingsService;

	@Inject
	OrganizationSettingsRepository settingsRepository;

	@Test
	void loadOrCreate_createsRowWhenMissing()
	{
		// given — empty DB

		// when
		OrganizationSettings created = settingsService.loadOrCreate();

		// then
		assertNotNull(created);
		assertEquals(1L, created.id);
		assertEquals(1, settingsRepository.count());
	}

	@Test
	void loadOrCreate_returnsExistingRow()
	{
		// given
		OrganizationSettings existing = new OrganizationSettings();
		existing.orgName = "Demokratie e.V.";
		settingsRepository.persist(existing);

		// when
		OrganizationSettings loaded = settingsService.loadOrCreate();

		// then
		assertEquals("Demokratie e.V.", loaded.orgName);
		assertEquals(1, settingsRepository.count());
	}

	@Test
	void update_persistsAllOrgAndSmtpFields()
	{
		// given
		OrganizationSettings updates = new OrganizationSettings();
		updates.orgName = "Tag der Demokratie e.V.";
		updates.orgStreet = "Hauptstraße 1";
		updates.orgZip = "10115";
		updates.orgCity = "Berlin";
		updates.orgTaxId = "27/680/12345";
		updates.orgIssuingAuthority = "Finanzamt Berlin";
		updates.orgPurpose = "Förderung der Demokratie";
		updates.smtpHost = "smtp.example.org";
		updates.smtpPort = 587;
		updates.smtpUser = "mailer";
		updates.smtpFrom = "noreply@demokratie.de";
		updates.adminNotificationEmail = "admin@demokratie.de";

		// when
		settingsService.update(updates, "secret-password");

		// then
		OrganizationSettings stored = settingsRepository.load().orElseThrow();
		assertEquals("Tag der Demokratie e.V.", stored.orgName);
		assertEquals("Hauptstraße 1", stored.orgStreet);
		assertEquals("10115", stored.orgZip);
		assertEquals("Berlin", stored.orgCity);
		assertEquals("27/680/12345", stored.orgTaxId);
		assertEquals("Finanzamt Berlin", stored.orgIssuingAuthority);
		assertEquals("Förderung der Demokratie", stored.orgPurpose);
		assertEquals("smtp.example.org", stored.smtpHost);
		assertEquals(587, stored.smtpPort);
		assertEquals("mailer", stored.smtpUser);
		assertEquals("secret-password", stored.smtpPassword);
		assertEquals("noreply@demokratie.de", stored.smtpFrom);
		assertEquals("admin@demokratie.de", stored.adminNotificationEmail);
	}

	@Test
	void update_keepsExistingSmtpPasswordWhenNewPasswordIsBlank()
	{
		// given
		OrganizationSettings existing = new OrganizationSettings();
		existing.smtpPassword = "old-secret";
		settingsRepository.persist(existing);

		OrganizationSettings updates = new OrganizationSettings();
		updates.orgName = "Updated Org";

		// when
		settingsService.update(updates, ""); // blank password = no change

		// then
		OrganizationSettings stored = settingsRepository.load().orElseThrow();
		assertEquals("Updated Org", stored.orgName);
		assertEquals("old-secret", stored.smtpPassword);
	}

	@Test
	void update_keepsExistingSmtpPasswordWhenNewPasswordIsNull()
	{
		// given
		OrganizationSettings existing = new OrganizationSettings();
		existing.smtpPassword = "old-secret";
		settingsRepository.persist(existing);

		OrganizationSettings updates = new OrganizationSettings();

		// when
		settingsService.update(updates, null);

		// then
		OrganizationSettings stored = settingsRepository.load().orElseThrow();
		assertEquals("old-secret", stored.smtpPassword);
	}

	@Test
	void update_overwritesSmtpPasswordWhenNewPasswordProvided()
	{
		// given
		OrganizationSettings existing = new OrganizationSettings();
		existing.smtpPassword = "old-secret";
		settingsRepository.persist(existing);

		OrganizationSettings updates = new OrganizationSettings();

		// when
		settingsService.update(updates, "new-secret");

		// then
		OrganizationSettings stored = settingsRepository.load().orElseThrow();
		assertEquals("new-secret", stored.smtpPassword);
	}

	@Test
	void update_createsRowIfNoneExists()
	{
		// given — empty DB
		OrganizationSettings updates = new OrganizationSettings();
		updates.orgName = "Fresh Org";

		// when
		settingsService.update(updates, "pw");

		// then
		assertTrue(settingsRepository.load().isPresent());
		assertEquals("Fresh Org", settingsRepository.load().get().orgName);
	}
}
