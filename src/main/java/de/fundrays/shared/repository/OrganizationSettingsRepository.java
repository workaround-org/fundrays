package de.fundrays.shared.repository;

import de.fundrays.shared.domain.OrganizationSettings;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class OrganizationSettingsRepository implements PanacheRepository<OrganizationSettings>
{
	public Optional<OrganizationSettings> load()
	{
		return findByIdOptional(1L);
	}
}
