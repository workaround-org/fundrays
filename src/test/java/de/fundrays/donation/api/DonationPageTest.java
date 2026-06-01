package de.fundrays.donation.api;

import de.fundrays.campaign.domain.Campaign;
import de.fundrays.campaign.domain.CampaignStatus;
import de.fundrays.campaign.repository.CampaignRepository;
import de.fundrays.donation.repository.DonationRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;

import java.time.Instant;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class DonationPageTest
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
	void index_rendersActiveCampaignWithProgress()
	{
		// given
		QuarkusTransaction.requiringNew().run(() -> campaignRepository.persist(aCampaign("klima", CampaignStatus.ACTIVE)));

		// when
		var response = given()
			.when().get("/donate/klima");

		// then
		response.then().statusCode(200)
			.body(containsString("Campaign klima"))
			.body(containsString("prog-bar"))
			.body(containsString("donate-form"));
	}

	@Test
	void index_returns404ForPausedCampaign()
	{
		// given
		QuarkusTransaction.requiringNew().run(() -> campaignRepository.persist(aCampaign("paused", CampaignStatus.PAUSED)));

		// when
		var response = given()
			.when().get("/donate/paused");

		// then
		response.then().statusCode(404);
	}

	@Test
	void index_returns404ForUnknownCampaign()
	{
		// given — no campaigns in DB

		// when
		var response = given()
			.when().get("/donate/unknown");

		// then
		response.then().statusCode(404);
	}

	@Test
	void thanks_rendersForActiveCampaign()
	{
		// given
		QuarkusTransaction.requiringNew().run(() -> campaignRepository.persist(aCampaign("klima", CampaignStatus.ACTIVE)));

		// when
		var response = given()
			.when().get("/donate/klima/thanks");

		// then
		response.then().statusCode(200)
			.body(containsString("Vielen Dank"));
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
