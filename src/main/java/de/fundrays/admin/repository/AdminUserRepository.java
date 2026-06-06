package de.fundrays.admin.repository;

import de.fundrays.admin.domain.AdminUser;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class AdminUserRepository implements PanacheRepository<AdminUser>
{
	public Optional<AdminUser> findByUsername(String username)
	{
		return find("username", username).firstResultOptional();
	}

	public boolean isLastAdmin()
	{
		return count() == 1;
	}
}
