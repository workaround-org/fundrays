package de.fundrays.admin.service;

import de.fundrays.admin.domain.AdminUser;
import de.fundrays.admin.repository.AdminUserRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestTransaction
class AdminBootstrapTest
{
	@Inject
	AdminBootstrap adminBootstrap;

	@Inject
	AdminUserRepository adminUserRepository;

	@Test
	void ensureAdmin_createsUserWhenMissing()
	{
		// given — no admin with this username exists
		assertTrue(adminUserRepository.findByUsername("ops").isEmpty());

		// when
		adminBootstrap.ensureAdmin("ops", "s3cret-pw");

		// then
		AdminUser created = adminUserRepository.findByUsername("ops").orElseThrow();
		assertEquals("admin", created.roles);
		assertNotNull(created.createdAt);
		assertTrue(BcryptUtil.matches("s3cret-pw", created.passwordHash));
	}

	@Test
	void ensureAdmin_resetsPasswordWhenChanged()
	{
		// given — an existing admin with an old password
		AdminUser existing = new AdminUser();
		existing.username = "ops";
		existing.passwordHash = BcryptUtil.bcryptHash("old-password");
		existing.roles = "admin";
		existing.displayName = "Administrator";
		existing.createdAt = Instant.now();
		adminUserRepository.persist(existing);

		// when
		adminBootstrap.ensureAdmin("ops", "new-password");

		// then — password updated, no duplicate row created
		assertEquals(1, adminUserRepository.count());
		AdminUser updated = adminUserRepository.findByUsername("ops").orElseThrow();
		assertTrue(BcryptUtil.matches("new-password", updated.passwordHash));
		assertFalse(BcryptUtil.matches("old-password", updated.passwordHash));
	}

	@Test
	void ensureAdmin_leavesHashUntouchedWhenPasswordMatches()
	{
		// given — an existing admin whose password already matches
		AdminUser existing = new AdminUser();
		existing.username = "ops";
		existing.passwordHash = BcryptUtil.bcryptHash("same-password");
		existing.roles = "admin";
		existing.displayName = "Administrator";
		existing.createdAt = Instant.now();
		adminUserRepository.persist(existing);
		String originalHash = existing.passwordHash;

		// when
		adminBootstrap.ensureAdmin("ops", "same-password");

		// then — hash is not re-generated
		AdminUser unchanged = adminUserRepository.findByUsername("ops").orElseThrow();
		assertEquals(originalHash, unchanged.passwordHash);
	}
}
