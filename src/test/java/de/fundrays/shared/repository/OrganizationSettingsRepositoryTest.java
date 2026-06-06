package de.fundrays.shared.repository;

import de.fundrays.shared.domain.OrganizationSettings;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestTransaction
class OrganizationSettingsRepositoryTest
{
	@Inject
	OrganizationSettingsRepository settingsRepository;

	@Test
	void load_returnsEmptyWhenNotConfigured()
	{
		// given — no settings row in DB

		// when
		Optional<OrganizationSettings> result = settingsRepository.load();

		// then
		assertTrue(result.isEmpty());
	}

	@Test
	void load_returnsSettingsWhenPresent()
	{
		// given
		OrganizationSettings settings = new OrganizationSettings();
		settings.orgName = "Demokratie e.V.";
		settings.orgCity = "Berlin";
		settings.smtpFrom = "noreply@demokratie.de";
		settingsRepository.persist(settings);

		// when
		Optional<OrganizationSettings> result = settingsRepository.load();

		// then
		assertTrue(result.isPresent());
		assertEquals("Demokratie e.V.", result.get().orgName);
		assertEquals("Berlin", result.get().orgCity);
	}

	@Test
	void load_alwaysReturnsSingletonRow()
	{
		// given
		OrganizationSettings settings = new OrganizationSettings();
		settings.orgName = "Tag der Demokratie";
		settingsRepository.persist(settings);

		// when
		long count = settingsRepository.count();

		// then — only one settings row ever exists (singleton, id=1)
		assertEquals(1, count);
	}
}
