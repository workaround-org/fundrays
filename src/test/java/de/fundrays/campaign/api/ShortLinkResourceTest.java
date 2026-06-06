package de.fundrays.campaign.api;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.endsWith;

@QuarkusTest
class ShortLinkResourceTest
{
	@Test
	void shortlink_redirectsToDonatePage()
	{
		// given — no setup needed; redirect is independent of DB state

		// when
		var response = given()
			.redirects().follow(false)
			.when().get("/d/my-campaign");

		// then
		response.then().statusCode(302)
			.header("Location", endsWith("/donate/my-campaign"));
	}

	@Test
	void shortlink_doesNotRequireAuth()
	{
		// given — no @TestSecurity annotation

		// when
		var response = given()
			.redirects().follow(false)
			.when().get("/d/any-slug");

		// then
		response.then().statusCode(302);
	}
}
