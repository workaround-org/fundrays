package de.fundrays.campaign.api;

import de.fundrays.campaign.domain.Campaign;
import de.fundrays.campaign.domain.CampaignStatus;
import de.fundrays.campaign.repository.CampaignRepository;
import de.fundrays.donation.domain.Donation;
import de.fundrays.donation.domain.DonationStatus;
import de.fundrays.donation.domain.PaymentMethod;
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
class PublicCampaignResourceTest
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
	void getBySlug_returnsActiveCampaign()
	{
		// given
		QuarkusTransaction.requiringNew().run(() -> campaignRepository.persist(aCampaign("active-one", CampaignStatus.ACTIVE)));

		// when
		var response = given()
			.when().get("/api/public/campaigns/active-one");

		// then
		response.then().statusCode(200)
			.body("slug", equalTo("active-one"))
			.body("title", equalTo("Campaign active-one"))
			.body("goalAmount", equalTo(100000))
			.body("status", equalTo("ACTIVE"));
	}

	@Test
	void getBySlug_returns404ForPausedCampaign()
	{
		// given
		QuarkusTransaction.requiringNew().run(() -> campaignRepository.persist(aCampaign("paused-one", CampaignStatus.PAUSED)));

		// when
		var response = given()
			.when().get("/api/public/campaigns/paused-one");

		// then
		response.then().statusCode(404);
	}

	@Test
	void getBySlug_returns404ForUnknownCampaign()
	{
		// given — no campaigns in DB

		// when
		var response = given()
			.when().get("/api/public/campaigns/nope");

		// then
		response.then().statusCode(404);
	}

	@Test
	void progress_aggregatesConfirmedDonationsAndExcludesPending()
	{
		// given
		QuarkusTransaction.requiringNew().run(() -> {
			Campaign c = aCampaign("with-donations", CampaignStatus.ACTIVE);
			campaignRepository.persist(c);
			donationRepository.persist(aDonation(c, 2000L, DonationStatus.CONFIRMED, "Danke!", "Alice", "ref-1"));
			donationRepository.persist(aDonation(c, 3000L, DonationStatus.CONFIRMED, null, "Bob", "ref-2"));
			donationRepository.persist(aDonation(c, 5000L, DonationStatus.PENDING, "Noch nicht", "Carol", "ref-3"));
		});

		// when
		var response = given()
			.when().get("/api/public/campaigns/with-donations/progress");

		// then
		response.then().statusCode(200)
			.body("raisedAmount", equalTo(5000))
			.body("goalAmount", equalTo(100000))
			.body("percentage", equalTo(5))
			.body("donationCount", equalTo(2))
			.body("recentMessages.size()", equalTo(1))
			.body("recentMessages[0].message", equalTo("Danke!"))
			.body("recentMessages[0].donorName", equalTo("Alice"));
	}

	@Test
	void progress_returns404ForPausedCampaign()
	{
		// given
		QuarkusTransaction.requiringNew().run(() -> campaignRepository.persist(aCampaign("paused-two", CampaignStatus.PAUSED)));

		// when
		var response = given()
			.when().get("/api/public/campaigns/paused-two/progress");

		// then
		response.then().statusCode(404);
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

	private Donation aDonation(Campaign c, long amount, DonationStatus status, String message, String donorName, String providerRef)
	{
		Donation d = new Donation();
		d.campaign = c;
		d.amount = amount;
		d.status = status;
		d.paymentMethod = PaymentMethod.PAYPAL;
		d.message = message;
		d.donorName = donorName;
		d.paymentProviderRef = providerRef;
		d.createdAt = Instant.now();
		return d;
	}
}
