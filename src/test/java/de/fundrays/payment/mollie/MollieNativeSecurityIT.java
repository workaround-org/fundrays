package de.fundrays.payment.mollie;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.restassured.http.ContentType;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Native reproduction of the Mollie reflection bugs (HTTP 400 "Invalid
 * Authorization header" and later HTTP 422 from a malformed request body).
 *
 * The Mollie SDK serializes the request and the {@code Authorization: Bearer
 * <key>} header, and deserializes the response, entirely through Jackson and
 * reflection over its model classes ({@code com.mollie.mollie.models.**}) and
 * util (de)serializers ({@code com.mollie.mollie.utils.**}). A native image
 * strips that reflective access unless the classes are registered, which is
 * done for the whole SDK in
 * {@code META-INF/native-image/com.mollie/mollie/reflect-config.json}. Without
 * the registration the SDK emits a malformed request/header and Mollie rejects
 * it (400/422), surfacing at the donate endpoint as 502 Bad Gateway. With the
 * registration the donation is created and the endpoint returns 200 with a
 * Mollie checkout URL.
 *
 * Runs only when a real Mollie test key AND a reachable datasource are provided
 * through the same env vars the deployment uses; otherwise it self-skips.
 */
@QuarkusIntegrationTest
class MollieNativeSecurityIT
{
	private static final String SLUG = "it-mollie-native";

	@BeforeEach
	void seedActiveCampaign() throws Exception
	{
		// given — a real test key and a reachable DB; otherwise skip
		assumeTrue(hasRealMollieKey(),
			"Skipping — set FUNDRAYS_MOLLIE_API_KEY to a real Mollie test key");
		assumeTrue(jdbcUrl() != null,
			"Skipping — set QUARKUS_DATASOURCE_JDBC_URL to a reachable Postgres");

		try (Connection c = connect())
		{
			deleteCampaign(c);
			try (PreparedStatement ps = c.prepareStatement(
				"INSERT INTO Campaign (id, slug, title, description, goalamount, currency, createdat, status) "
					+ "VALUES (gen_random_uuid(), ?, ?, ?, ?, ?, now(), 'ACTIVE')"))
			{
				ps.setString(1, SLUG);
				ps.setString(2, "Native Mollie IT");
				ps.setString(3, "Created by MollieNativeSecurityIT");
				ps.setLong(4, 500000L);
				ps.setString(5, "EUR");
				ps.executeUpdate();
			}
		}
	}

	@AfterEach
	void removeCampaign() throws Exception
	{
		if (!hasRealMollieKey() || jdbcUrl() == null)
		{
			return;
		}
		try (Connection c = connect())
		{
			deleteCampaign(c);
		}
	}

	@Test
	void donate_withMollie_buildsValidBearerHeader_andReturnsCheckoutUrl()
	{
		// when — POST a Mollie donation against the live Mollie test API
		// then — a correctly built Authorization header yields 200 + checkout
		// URL
		// (before the native reflection fix this fails with 502 Bad Gateway)
		given()
			.contentType(ContentType.JSON)
			.body("{\"amount\":500,\"paymentMethod\":\"MOLLIE\",\"donorName\":\"IT\"}")
			.when()
			.post("/api/public/campaigns/" + SLUG + "/donate")
			.then()
			.statusCode(200)
			.body("paymentUrl", notNullValue())
			.body("paymentUrl", startsWith("https://"));
	}

	private void deleteCampaign(Connection c) throws Exception
	{
		try (PreparedStatement ps = c.prepareStatement(
			"DELETE FROM Donation WHERE campaign_id IN (SELECT id FROM Campaign WHERE slug = ?)"))
		{
			ps.setString(1, SLUG);
			ps.executeUpdate();
		}
		try (PreparedStatement ps = c.prepareStatement("DELETE FROM Campaign WHERE slug = ?"))
		{
			ps.setString(1, SLUG);
			ps.executeUpdate();
		}
	}

	private static Connection connect() throws Exception
	{
		return DriverManager.getConnection(jdbcUrl(), dbUser(), dbPassword());
	}

	private static boolean hasRealMollieKey()
	{
		String key = System.getenv("FUNDRAYS_MOLLIE_API_KEY");
		return key != null && key.startsWith("test_") && key.length() > 20;
	}

	private static String jdbcUrl()
	{
		return System.getenv("QUARKUS_DATASOURCE_JDBC_URL");
	}

	private static String dbUser()
	{
		return System.getenv("QUARKUS_DATASOURCE_USERNAME");
	}

	private static String dbPassword()
	{
		return System.getenv("QUARKUS_DATASOURCE_PASSWORD");
	}
}
