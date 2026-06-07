package de.fundrays.donation.api;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fundrays.campaign.domain.Campaign;
import de.fundrays.campaign.domain.CampaignStatus;
import de.fundrays.campaign.repository.CampaignRepository;
import de.fundrays.donation.domain.Donation;
import de.fundrays.donation.domain.DonationStatus;
import de.fundrays.donation.domain.PaymentMethod;
import de.fundrays.donation.repository.DonationRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class AdminDonationsTest
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
	@TestSecurity(user = "admin", roles = "admin")
	void confirm_manuallyConfirmsPendingWeroDonation()
	{
		// given
		UUID donationId = persistPendingWeroDonation();
		Response indexResponse = given().when().get("/admin/donations/");
		CsrfCreds csrf = extractCsrf(indexResponse);

		// when
		var response = given()
			.cookie("csrf-token", csrf.cookie)
			.formParam("csrf-token", csrf.formValue)
			.when().post("/admin/donations/" + donationId + "/confirm");

		// then
		assertTrue(response.statusCode() < 400, "expected 2xx/3xx, got " + response.statusCode());
		QuarkusTransaction.requiringNew().run(() -> {
			Donation donation = donationRepository.find("id", donationId).firstResult();
			assertEquals(DonationStatus.CONFIRMED, donation.status);
		});
	}

	private UUID persistPendingWeroDonation()
	{
		return QuarkusTransaction.requiringNew().call(() -> {
			Campaign campaign = new Campaign();
			campaign.slug = "manual-confirm";
			campaign.title = "Manual Confirmation";
			campaign.goalAmount = 100000L;
			campaign.createdAt = Instant.now();
			campaign.status = CampaignStatus.ACTIVE;
			campaignRepository.persist(campaign);

			Donation donation = new Donation();
			donation.campaign = campaign;
			donation.amount = 1500L;
			donation.paymentMethod = PaymentMethod.MOLLIE;
			donation.paymentProviderRef = "mollie-manual";
			donation.status = DonationStatus.PENDING;
			donation.createdAt = Instant.now();
			donationRepository.persist(donation);
			return donation.id;
		});
	}

	private record CsrfCreds(String cookie, String formValue)
	{
	}

	private static CsrfCreds extractCsrf(Response indexResponse)
	{
		String cookie = indexResponse.cookie("csrf-token");
		Pattern pattern = Pattern.compile("name=\"csrf-token\"\\s+value=\"([^\"]+)\"");
		Matcher matcher = pattern.matcher(indexResponse.asString());
		String formValue = matcher.find() ? matcher.group(1) : cookie;
		return new CsrfCreds(cookie, formValue);
	}
}
