package de.fundrays.campaign.api;

import de.fundrays.campaign.domain.Campaign;
import de.fundrays.campaign.domain.CampaignStatus;
import de.fundrays.campaign.repository.CampaignRepository;
import de.fundrays.donation.repository.DonationRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import java.time.Instant;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class CampaignResourceTest
{
	@Inject
	CampaignRepository campaignRepository;

	@Inject
	DonationRepository donationRepository;

	@BeforeEach
	void setup()
	{
		QuarkusTransaction.requiringNew().run(() -> {
			donationRepository.deleteAll();
			campaignRepository.deleteAll();
		});
	}

	@Test
	void listActive_returnsOnlyActiveCampaigns()
	{
		// given
		QuarkusTransaction.requiringNew().run(() -> {
			campaignRepository.persist(aCampaign("active-one", CampaignStatus.ACTIVE));
			campaignRepository.persist(aCampaign("paused-one", CampaignStatus.PAUSED));
		});

		// when
		var response = given()
			.when().get("/api/campaigns");

		// then
		response.then().statusCode(200)
			.body("size()", equalTo(1))
			.body("[0].slug", equalTo("active-one"))
			.body("[0].raisedAmount", equalTo(0))
			.body("[0].donationCount", equalTo(0));
	}

	@Test
	void listActive_returnsEmptyListWhenNoCampaigns()
	{
		// given — no campaigns in DB

		// when
		var response = given()
			.when().get("/api/campaigns");

		// then
		response.then().statusCode(200)
			.body("size()", equalTo(0));
	}

	@Test
	void getBySlug_returnsCampaignWithProgress()
	{
		// given
		QuarkusTransaction.requiringNew().run(() -> campaignRepository.persist(aCampaign("my-campaign", CampaignStatus.ACTIVE)));

		// when
		var response = given()
			.when().get("/api/campaigns/my-campaign");

		// then
		response.then().statusCode(200)
			.body("slug", equalTo("my-campaign"))
			.body("goalAmount", equalTo(100000))
			.body("raisedAmount", equalTo(0))
			.body("donationCount", equalTo(0));
	}

	@Test
	void getBySlug_returns404ForUnknownSlug()
	{
		// given — no campaigns in DB

		// when
		var response = given()
			.when().get("/api/campaigns/nonexistent");

		// then
		response.then().statusCode(404);
	}

	@Test
	@TestSecurity(user = "admin", roles = { "admin" })
	void create_returnsCampaignWith201()
	{
		// given
		var body = """
			{"slug":"new-campaign","title":"New Campaign","goalAmount":10000}
			""";

		// when
		var response = given()
			.contentType(ContentType.JSON)
			.body(body)
			.when().post("/api/campaigns");

		// then
		response.then().statusCode(201)
			.body("slug", equalTo("new-campaign"))
			.body("id", notNullValue())
			.body("raisedAmount", equalTo(0));
	}

	@Test
	void create_withoutAuth_returns401()
	{
		// given
		var body = """
			{"slug":"new-campaign","title":"New Campaign","goalAmount":10000}
			""";

		// when
		var response = given()
			.contentType(ContentType.JSON)
			.body(body)
			.when().post("/api/campaigns");

		// then
		response.then().statusCode(401);
	}

	@Test
	@TestSecurity(user = "admin", roles = { "admin" })
	void create_withDuplicateSlug_returns409()
	{
		// given
		QuarkusTransaction.requiringNew().run(() -> campaignRepository.persist(aCampaign("existing-slug", CampaignStatus.ACTIVE)));
		var body = """
			{"slug":"existing-slug","title":"Duplicate","goalAmount":5000}
			""";

		// when
		var response = given()
			.contentType(ContentType.JSON)
			.body(body)
			.when().post("/api/campaigns");

		// then
		response.then().statusCode(409)
			.body("message", containsString("existing-slug"));
	}

	@Test
	@TestSecurity(user = "admin", roles = { "admin" })
	void update_changesTitleAndStatus()
	{
		// given
		QuarkusTransaction.requiringNew().run(() -> campaignRepository.persist(aCampaign("my-campaign", CampaignStatus.ACTIVE)));
		var body = """
			{"title":"Updated Title","status":"PAUSED"}
			""";

		// when
		var response = given()
			.contentType(ContentType.JSON)
			.body(body)
			.when().patch("/api/campaigns/my-campaign");

		// then
		response.then().statusCode(200)
			.body("slug", equalTo("my-campaign"))
			.body("title", equalTo("Updated Title"))
			.body("status", equalTo("PAUSED"));
	}

	@Test
	@TestSecurity(user = "admin", roles = { "admin" })
	void update_returns404ForUnknownSlug()
	{
		// given — no campaigns in DB
		var body = """
			{"status":"PAUSED"}
			""";

		// when
		var response = given()
			.contentType(ContentType.JSON)
			.body(body)
			.when().patch("/api/campaigns/nonexistent");

		// then
		response.then().statusCode(404);
	}

	@Test
	void update_withoutAuth_returns401()
	{
		// given
		var body = """
			{"status":"PAUSED"}
			""";

		// when
		var response = given()
			.contentType(ContentType.JSON)
			.body(body)
			.when().patch("/api/campaigns/any-campaign");

		// then
		response.then().statusCode(401);
	}

	private Campaign aCampaign(String slug, CampaignStatus status)
	{
		Campaign c = new Campaign();
		c.slug = slug;
		c.title = "Campaign " + slug;
		c.goalAmount = 100000L;
		c.createdAt = Instant.now();
		c.status = status;
		return c;
	}
}
