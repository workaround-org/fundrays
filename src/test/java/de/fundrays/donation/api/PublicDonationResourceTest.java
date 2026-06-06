package de.fundrays.donation.api;

import de.fundrays.campaign.domain.Campaign;
import de.fundrays.campaign.domain.CampaignStatus;
import de.fundrays.campaign.repository.CampaignRepository;
import de.fundrays.donation.repository.DonationRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;

import java.time.Instant;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class PublicDonationResourceTest
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
	void donate_createsPendingDonationAndReturnsPaymentUrl()
	{
		// given
		QuarkusTransaction.requiringNew().run(() -> campaignRepository.persist(aCampaign("active-campaign", CampaignStatus.ACTIVE)));
		var body = """
			{"amount":1500,"paymentMethod":"PAYPAL"}
			""";

		// when
		var response = given()
			.contentType(ContentType.JSON)
			.body(body)
			.when().post("/api/public/campaigns/active-campaign/donate");

		// then
		response.then().statusCode(200)
			.body("id", notNullValue())
			.body("amount", equalTo(1500))
			.body("status", equalTo("PENDING"))
			.body("paymentUrl", containsString("/donate/active-campaign/thanks"));
	}

	@Test
	void donate_returns400ForAmountBelowMinimum()
	{
		// given — 499 cents is below the 500 cent minimum
		QuarkusTransaction.requiringNew().run(() -> campaignRepository.persist(aCampaign("active-campaign", CampaignStatus.ACTIVE)));
		var body = """
			{"amount":499,"paymentMethod":"PAYPAL"}
			""";

		// when
		var response = given()
			.contentType(ContentType.JSON)
			.body(body)
			.when().post("/api/public/campaigns/active-campaign/donate");

		// then
		response.then().statusCode(400);
	}

	@Test
	void donate_returns404ForUnknownCampaign()
	{
		// given — no campaigns in DB
		var body = """
			{"amount":1000,"paymentMethod":"STRIPE"}
			""";

		// when
		var response = given()
			.contentType(ContentType.JSON)
			.body(body)
			.when().post("/api/public/campaigns/nonexistent/donate");

		// then
		response.then().statusCode(404);
	}

	@Test
	void donate_returns422ForInactiveCampaign()
	{
		// given
		QuarkusTransaction.requiringNew().run(() -> campaignRepository.persist(aCampaign("paused-campaign", CampaignStatus.PAUSED)));
		var body = """
			{"amount":1000,"paymentMethod":"PAYPAL"}
			""";

		// when
		var response = given()
			.contentType(ContentType.JSON)
			.body(body)
			.when().post("/api/public/campaigns/paused-campaign/donate");

		// then
		response.then().statusCode(422)
			.body("message", notNullValue());
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
