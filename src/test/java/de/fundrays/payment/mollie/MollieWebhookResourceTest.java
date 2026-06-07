package de.fundrays.payment.mollie;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import com.mollie.mollie.models.components.PaymentResponseStatus;
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
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class MollieWebhookResourceTest
{
	@Inject
	CampaignRepository campaignRepository;

	@Inject
	DonationRepository donationRepository;

	@Inject
	OrganizationSettingsRepository settingsRepository;

	@Inject
	MockMailbox mailbox;

	@InjectMock
	MollieApiAdapter mollieApiAdapter;

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
	void webhook_paidStatusConfirmsPendingDonation()
	{
		// given
		persistDonation("tr_paid1", DonationStatus.PENDING, null);
		when(mollieApiAdapter.fetchStatus("tr_paid1")).thenReturn(PaymentResponseStatus.PAID);

		// when
		var response = given()
			.contentType(ContentType.URLENC)
			.formParam("id", "tr_paid1")
			.when().post("/webhooks/mollie");

		// then
		response.then().statusCode(200);
		QuarkusTransaction.requiringNew().run(() -> {
			Donation donation = donationRepository.findByProviderRef("tr_paid1").orElseThrow();
			assertEquals(DonationStatus.CONFIRMED, donation.status);
			assertNotNull(donation.confirmedAt);
		});
	}

	@Test
	void webhook_canceledStatusMarksDonationFailed()
	{
		// given
		persistDonation("tr_canceled1", DonationStatus.PENDING, null);
		when(mollieApiAdapter.fetchStatus("tr_canceled1")).thenReturn(PaymentResponseStatus.CANCELED);

		// when
		var response = given()
			.contentType(ContentType.URLENC)
			.formParam("id", "tr_canceled1")
			.when().post("/webhooks/mollie");

		// then
		response.then().statusCode(200);
		QuarkusTransaction.requiringNew().run(() -> {
			Donation donation = donationRepository.findByProviderRef("tr_canceled1").orElseThrow();
			assertEquals(DonationStatus.FAILED, donation.status);
			assertNull(donation.confirmedAt);
		});
	}

	@Test
	void webhook_pendingStatusIsNoOp()
	{
		// given
		persistDonation("tr_pending1", DonationStatus.PENDING, null);
		when(mollieApiAdapter.fetchStatus("tr_pending1")).thenReturn(PaymentResponseStatus.PENDING);

		// when
		var response = given()
			.contentType(ContentType.URLENC)
			.formParam("id", "tr_pending1")
			.when().post("/webhooks/mollie");

		// then
		response.then().statusCode(200);
		QuarkusTransaction.requiringNew().run(() -> {
			Donation donation = donationRepository.findByProviderRef("tr_pending1").orElseThrow();
			assertEquals(DonationStatus.PENDING, donation.status);
		});
	}

	@Test
	void webhook_mollieApiErrorReturnsFiveHundred()
	{
		// given
		persistDonation("tr_error1", DonationStatus.PENDING, null);
		when(mollieApiAdapter.fetchStatus("tr_error1"))
			.thenThrow(new MollieGatewayException("Mollie unavailable"));

		// when
		var response = given()
			.contentType(ContentType.URLENC)
			.formParam("id", "tr_error1")
			.when().post("/webhooks/mollie");

		// then — 5xx causes Mollie to retry later
		response.then().statusCode(500);
		QuarkusTransaction.requiringNew().run(() -> {
			Donation donation = donationRepository.findByProviderRef("tr_error1").orElseThrow();
			assertEquals(DonationStatus.PENDING, donation.status);
		});
	}

	@Test
	void webhook_duplicatePaidCallIsIdempotent()
	{
		// given
		persistDonation("tr_dup1", DonationStatus.PENDING, "donor@example.org");
		when(mollieApiAdapter.fetchStatus("tr_dup1")).thenReturn(PaymentResponseStatus.PAID);

		// when — call the webhook twice
		given().contentType(ContentType.URLENC).formParam("id", "tr_dup1")
			.when().post("/webhooks/mollie")
			.then().statusCode(200);
		given().contentType(ContentType.URLENC).formParam("id", "tr_dup1")
			.when().post("/webhooks/mollie")
			.then().statusCode(200);

		// then — confirmation mail sent only once
		assertEquals(1, mailbox.getMessagesSentTo("donor@example.org").size());
	}

	private void persistDonation(String providerRef, DonationStatus status, String donorEmail)
	{
		QuarkusTransaction.requiringNew().run(() -> {
			Campaign campaign = new Campaign();
			campaign.slug = "campaign-" + providerRef;
			campaign.title = "Mollie Campaign";
			campaign.goalAmount = 100000L;
			campaign.createdAt = Instant.now();
			campaign.status = CampaignStatus.ACTIVE;
			campaignRepository.persist(campaign);

			Donation donation = new Donation();
			donation.campaign = campaign;
			donation.amount = 1500L;
			donation.paymentMethod = PaymentMethod.MOLLIE;
			donation.paymentProviderRef = providerRef;
			donation.status = status;
			donation.donorEmail = donorEmail;
			donation.createdAt = Instant.now();
			donationRepository.persist(donation);
		});
	}
}
