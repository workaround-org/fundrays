package de.fundrays.payment.wero;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import de.fundrays.campaign.domain.Campaign;
import de.fundrays.campaign.domain.CampaignStatus;
import de.fundrays.campaign.repository.CampaignRepository;
import de.fundrays.donation.domain.Donation;
import de.fundrays.donation.domain.DonationStatus;
import de.fundrays.donation.domain.PaymentMethod;
import de.fundrays.donation.repository.DonationRepository;
import de.fundrays.shared.domain.OrganizationSettings;
import de.fundrays.shared.repository.OrganizationSettingsRepository;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class WeroWebhookResourceTest
{

	private static final String WEBHOOK_SECRET = "test-webhook-secret";

	@Inject
	CampaignRepository campaignRepository;

	@Inject
	DonationRepository donationRepository;

	@Inject
	OrganizationSettingsRepository settingsRepository;

	@Inject
	MockMailbox mailbox;

	@BeforeEach
	void setup()
	{
		mailbox.clear();
		QuarkusTransaction.requiringNew().run(() -> {
			donationRepository.deleteAll();
			campaignRepository.deleteAll();
			settingsRepository.deleteAll();
			OrganizationSettings settings = new OrganizationSettings();
			settings.smtpFrom = "noreply@fundrays.example";
			settingsRepository.persist(settings);
		});
	}

	@Test
	void webhook_validSignatureConfirmsPendingDonation()
	{
		// given
		persistDonation("wero-confirm-1", DonationStatus.PENDING, null);
		String payload = """
			{"transactionId":"wero-confirm-1","status":"CONFIRMED"}
			""";

		// when
		var response = given()
			.contentType(ContentType.JSON)
			.header("X-Wero-Signature", signature(payload))
			.body(payload)
			.when().post("/webhooks/wero");

		// then
		response.then().statusCode(200);
		QuarkusTransaction.requiringNew().run(() -> {
			Donation donation = donationRepository.findByProviderRef("wero-confirm-1").orElseThrow();
			assertEquals(DonationStatus.CONFIRMED, donation.status);
			assertNotNull(donation.confirmedAt);
		});
	}

	@Test
	void webhook_invalidSignatureIsRejected()
	{
		// given
		persistDonation("wero-invalid-signature", DonationStatus.PENDING, null);
		String payload = """
			{"transactionId":"wero-invalid-signature","status":"CONFIRMED"}
			""";

		// when
		var response = given()
			.contentType(ContentType.JSON)
			.header("X-Wero-Signature", "sha256=00")
			.body(payload)
			.when().post("/webhooks/wero");

		// then
		response.then().statusCode(401);
		QuarkusTransaction.requiringNew().run(() -> {
			Donation donation = donationRepository.findByProviderRef("wero-invalid-signature").orElseThrow();
			assertEquals(DonationStatus.PENDING, donation.status);
			assertNull(donation.confirmedAt);
		});
	}

	@Test
	void webhook_duplicateConfirmationIsNoOp()
	{
		// given
		persistDonation("wero-duplicate", DonationStatus.PENDING, "donor@example.org");
		String payload = """
			{"transactionId":"wero-duplicate","status":"COMPLETED"}
			""";
		String signature = signature(payload);

		// when
		given().contentType(ContentType.JSON).header("X-Wero-Signature", signature).body(payload)
			.when().post("/webhooks/wero")
			.then().statusCode(200);
		given().contentType(ContentType.JSON).header("X-Wero-Signature", signature).body(payload)
			.when().post("/webhooks/wero")
			.then().statusCode(200);

		// then
		assertEquals(1, mailbox.getMessagesSentTo("donor@example.org").size());
	}

	@Test
	void webhook_failureMarksPendingDonationFailed()
	{
		// given
		persistDonation("wero-failed", DonationStatus.PENDING, null);
		String payload = """
			{"paymentProviderRef":"wero-failed","status":"DECLINED"}
			""";

		// when
		var response = given()
			.contentType(ContentType.JSON)
			.header("X-Wero-Signature", signature(payload))
			.body(payload)
			.when().post("/webhooks/wero");

		// then
		response.then().statusCode(200);
		QuarkusTransaction.requiringNew().run(() -> {
			Donation donation = donationRepository.findByProviderRef("wero-failed").orElseThrow();
			assertEquals(DonationStatus.FAILED, donation.status);
			assertNull(donation.confirmedAt);
		});
	}

	private void persistDonation(String providerRef, DonationStatus status, String donorEmail)
	{
		QuarkusTransaction.requiringNew().run(() -> {
			Campaign campaign = new Campaign();
			campaign.slug = "campaign-" + providerRef;
			campaign.title = "Wero Campaign";
			campaign.goalAmount = 100000L;
			campaign.createdAt = Instant.now();
			campaign.status = CampaignStatus.ACTIVE;
			campaignRepository.persist(campaign);

			Donation donation = new Donation();
			donation.campaign = campaign;
			donation.amount = 1500L;
			donation.paymentMethod = PaymentMethod.WERO;
			donation.paymentProviderRef = providerRef;
			donation.status = status;
			donation.donorEmail = donorEmail;
			donation.createdAt = Instant.now();
			donationRepository.persist(donation);
		});
	}

	private String signature(String payload)
	{
		try
		{
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			return "sha256=" + HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
		}
		catch (Exception e)
		{
			throw new IllegalStateException(e);
		}
	}
}
