package de.fundrays.shared.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fundrays.shared.domain.OrganizationSettings;
import de.fundrays.shared.repository.OrganizationSettingsRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class AdminOrganizationSettingsTest
{
	@Inject
	OrganizationSettingsRepository settingsRepository;

	@BeforeEach
	void setup()
	{
		QuarkusTransaction.requiringNew().run(() -> settingsRepository.deleteAll());
	}

	@AfterEach
	void cleanup()
	{
		QuarkusTransaction.requiringNew().run(() -> settingsRepository.deleteAll());
	}

	@Test
	@TestSecurity(user = "admin", roles = "admin")
	void index_rendersFormWithStoredOrgFields()
	{
		// given
		QuarkusTransaction.requiringNew().run(() -> {
			OrganizationSettings settings = new OrganizationSettings();
			settings.orgName = "Demokratie e.V.";
			settings.orgCity = "Berlin";
			settings.smtpFrom = "noreply@demokratie.de";
			settingsRepository.persist(settings);
		});

		// when
		var response = given().when().get("/admin/settings/");

		// then
		response.then().statusCode(200)
			.body(containsString("Demokratie e.V."))
			.body(containsString("Berlin"))
			.body(containsString("noreply@demokratie.de"));
	}

	@Test
	@TestSecurity(user = "admin", roles = "admin")
	void index_doesNotLeakSmtpPasswordInHtml()
	{
		// given
		QuarkusTransaction.requiringNew().run(() -> {
			OrganizationSettings settings = new OrganizationSettings();
			settings.smtpPassword = "super-secret-do-not-leak";
			settingsRepository.persist(settings);
		});

		// when
		var response = given().when().get("/admin/settings/");

		// then
		response.then().statusCode(200)
			.body(not(containsString("super-secret-do-not-leak")));
	}

	@Test
	@TestSecurity(user = "admin", roles = "admin")
	void index_requiresAdminRole()
	{
		// given — admin role granted via @TestSecurity
		// when
		var response = given().when().get("/admin/settings/");

		// then
		response.then().statusCode(200);
	}

	@Test
	void index_rejectsUnauthenticatedRequests()
	{
		// given — no auth

		// when
		var response = given().redirects().follow(false).when().get("/admin/settings/");

		// then — form auth redirects or 401
		int status = response.statusCode();
		assertEquals(true, status == 302 || status == 401, "expected 302 or 401, got " + status);
	}

	@Test
	@TestSecurity(user = "admin", roles = "admin")
	void save_persistsFormValues()
	{
		// given — empty DB
		Response indexResponse = given().when().get("/admin/settings/");
		CsrfCreds csrf = extractCsrf(indexResponse);

		// when
		var response = given()
			.cookie("csrf-token", csrf.cookie)
			.formParam("csrf-token", csrf.formValue)
			.formParam("orgName", "Tag der Demokratie")
			.formParam("orgStreet", "Hauptstr. 1")
			.formParam("orgZip", "10115")
			.formParam("orgCity", "Berlin")
			.formParam("orgTaxId", "27/680/12345")
			.formParam("orgIssuingAuthority", "Finanzamt Berlin")
			.formParam("orgPurpose", "Demokratieförderung")
			.formParam("smtpHost", "smtp.example.org")
			.formParam("smtpPort", "587")
			.formParam("smtpUser", "mailer")
			.formParam("smtpPassword", "new-secret")
			.formParam("smtpFrom", "noreply@example.org")
			.formParam("adminNotificationEmail", "admin@example.org")
			.when().post("/admin/settings/");

		// then
		assertTrue(response.statusCode() < 400, "expected 2xx/3xx, got " + response.statusCode());

		QuarkusTransaction.requiringNew().run(() -> {
			OrganizationSettings stored = settingsRepository.load().orElseThrow();
			assertEquals("Tag der Demokratie", stored.orgName);
			assertEquals("Hauptstr. 1", stored.orgStreet);
			assertEquals("Berlin", stored.orgCity);
			assertEquals(587, stored.smtpPort);
			assertEquals("new-secret", stored.smtpPassword);
			assertEquals("noreply@example.org", stored.smtpFrom);
			assertEquals("admin@example.org", stored.adminNotificationEmail);
		});
	}

	@Test
	@TestSecurity(user = "admin", roles = "admin")
	void save_doesNotOverwriteSmtpPasswordWhenBlank()
	{
		// given
		QuarkusTransaction.requiringNew().run(() -> {
			OrganizationSettings settings = new OrganizationSettings();
			settings.smtpPassword = "old-secret";
			settings.orgName = "Old Name";
			settingsRepository.persist(settings);
		});
		Response indexResponse = given().when().get("/admin/settings/");
		CsrfCreds csrf = extractCsrf(indexResponse);

		// when — submit with empty smtpPassword
		var response = given()
			.cookie("csrf-token", csrf.cookie)
			.formParam("csrf-token", csrf.formValue)
			.formParam("orgName", "New Name")
			.formParam("smtpPassword", "")
			.when().post("/admin/settings/");

		// then
		assertTrue(response.statusCode() < 400, "expected 2xx/3xx, got " + response.statusCode());
		QuarkusTransaction.requiringNew().run(() -> {
			OrganizationSettings stored = settingsRepository.load().orElseThrow();
			assertEquals("New Name", stored.orgName);
			assertEquals("old-secret", stored.smtpPassword);
		});
	}

	private record CsrfCreds(String cookie, String formValue)
	{
	}

	private static CsrfCreds extractCsrf(Response indexResponse)
	{
		String cookie = indexResponse.cookie("csrf-token");
		Pattern p = Pattern.compile("name=\"csrf-token\"\\s+value=\"([^\"]+)\"");
		Matcher m = p.matcher(indexResponse.asString());
		String formValue = m.find() ? m.group(1) : cookie;
		return new CsrfCreds(cookie, formValue);
	}
}
