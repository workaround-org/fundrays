package de.fundrays.admin.repository;

import de.fundrays.admin.domain.AdminUser;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestTransaction
class AdminUserRepositoryTest
{
	@Inject
	AdminUserRepository adminUserRepository;

	@Test
	void findByUsername_returnsMatchingUser()
	{
		// given
		adminUserRepository.deleteAll();
		adminUserRepository.persist(anAdminUser("alice"));

		// when
		Optional<AdminUser> result = adminUserRepository.findByUsername("alice");

		// then
		assertTrue(result.isPresent());
		assertEquals("alice", result.get().username);
	}

	@Test
	void findByUsername_returnsEmptyForUnknownUsername()
	{
		// given
		adminUserRepository.deleteAll();

		// when
		Optional<AdminUser> result = adminUserRepository.findByUsername("nobody");

		// then
		assertTrue(result.isEmpty());
	}

	@Test
	void isLastAdmin_returnsTrueWhenOnlyOneAdminExists()
	{
		// given
		adminUserRepository.deleteAll();
		adminUserRepository.persist(anAdminUser("only-admin"));

		// when
		boolean result = adminUserRepository.isLastAdmin();

		// then
		assertTrue(result);
	}

	@Test
	void isLastAdmin_returnsFalseWhenMultipleAdminsExist()
	{
		// given
		adminUserRepository.deleteAll();
		adminUserRepository.persist(anAdminUser("admin-one"));
		adminUserRepository.persist(anAdminUser("admin-two"));

		// when
		boolean result = adminUserRepository.isLastAdmin();

		// then
		assertFalse(result);
	}

	private AdminUser anAdminUser(String username)
	{
		AdminUser user = new AdminUser();
		user.username = username;
		user.passwordHash = "$2a$10$6u1ybSDSJBwHFiUiSn3MeORUMnsX1fMyvLOKa.LvHOzjLuHT7qbR6";
		user.displayName = username;
		user.createdAt = Instant.now();
		return user;
	}
}
