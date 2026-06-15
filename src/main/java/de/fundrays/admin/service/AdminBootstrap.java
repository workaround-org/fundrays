package de.fundrays.admin.service;

import de.fundrays.admin.domain.AdminUser;
import de.fundrays.admin.repository.AdminUserRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Optional;

/**
 * Ensures an admin account exists on startup, configured via environment.
 *
 * <p>
 * Set {@code FUNDRAYS_ADMIN_USERNAME} and {@code FUNDRAYS_ADMIN_PASSWORD}
 * (Quarkus maps these to {@code fundrays.admin.username/password}). On boot:
 * <ul>
 * <li>if the user does not exist, it is created with a freshly hashed
 * password;</li>
 * <li>if it exists but the stored hash no longer matches the configured
 * password, the password is reset (so the env var is the source of truth).</li>
 * </ul>
 * The dev seed still provides {@code admin/admin123} for local use, so this is
 * only needed in production. If either value is blank, bootstrap is skipped.
 */
@ApplicationScoped
public class AdminBootstrap
{
	private static final Logger LOG = Logger.getLogger(AdminBootstrap.class);

	@Inject
	AdminUserRepository adminUserRepository;

	@ConfigProperty(name = "fundrays.admin.username")
	Optional<String> username;

	@ConfigProperty(name = "fundrays.admin.password")
	Optional<String> password;

	@Transactional
	void onStart(@Observes StartupEvent event)
	{
		String user = username.map(String::trim).filter(s -> !s.isEmpty()).orElse(null);
		String pass = password.filter(s -> !s.isEmpty()).orElse(null);

		if (user == null || pass == null)
		{
			LOG.debug("Admin bootstrap skipped: fundrays.admin.username/password not set");
			return;
		}

		ensureAdmin(user, pass);
	}

	/**
	 * Creates the admin user if absent, or resets its password if it no longer
	 * matches {@code password}. Runs inside the caller's transaction.
	 */
	void ensureAdmin(String user, String password)
	{
		AdminUser existing = adminUserRepository.findByUsername(user).orElse(null);
		if (existing == null)
		{
			AdminUser admin = new AdminUser();
			admin.username = user;
			admin.passwordHash = BcryptUtil.bcryptHash(password);
			admin.roles = "admin";
			admin.displayName = "Administrator";
			admin.createdAt = Instant.now();
			adminUserRepository.persist(admin);
			LOG.infof("Admin bootstrap: created admin user '%s'", user);
		}
		else if (!BcryptUtil.matches(password, existing.passwordHash))
		{
			existing.passwordHash = BcryptUtil.bcryptHash(password);
			LOG.infof("Admin bootstrap: reset password for admin user '%s'", user);
		}
		else
		{
			LOG.debugf("Admin bootstrap: admin user '%s' already up to date", user);
		}
	}
}
