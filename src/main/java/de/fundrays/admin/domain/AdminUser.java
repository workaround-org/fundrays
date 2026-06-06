package de.fundrays.admin.domain;

import io.quarkus.security.jpa.Password;
import io.quarkus.security.jpa.Roles;
import io.quarkus.security.jpa.UserDefinition;
import io.quarkus.security.jpa.Username;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.Instant;
import java.util.UUID;

@Entity
@UserDefinition
public class AdminUser
{
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	public UUID id;

	@Username
	@Column(unique = true, nullable = false)
	public String username;

	/**
	 * BCrypt hash — use BcryptUtil.bcryptHash(plaintext) when creating users
	 */
	@Password
	@Column(nullable = false)
	public String passwordHash;

	@Roles
	@Column(nullable = false)
	public String roles = "admin";

	@Column(nullable = false)
	public String displayName;

	@Column(nullable = false)
	public Instant createdAt;

	public Instant lastLoginAt;
}
