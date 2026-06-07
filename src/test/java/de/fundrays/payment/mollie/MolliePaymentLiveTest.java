package de.fundrays.payment.mollie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.mollie.mollie.models.components.PaymentResponseStatus;
import de.fundrays.campaign.domain.Campaign;
import de.fundrays.donation.domain.Donation;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests that hit the real Mollie test API. These tests run only
 * when FUNDRAYS_MOLLIE_API_KEY is set to a real test key in .env. They are
 * skipped automatically in CI unless the secret is configured there too.
 */
@QuarkusTest
@TestProfile(MolliePaymentLiveTest.LiveProfile.class)
class MolliePaymentLiveTest
{
	public static class LiveProfile implements QuarkusTestProfile
	{
		@Override
		public Map<String, String> getConfigOverrides()
		{
			String apiKey = System.getenv().getOrDefault("FUNDRAYS_MOLLIE_API_KEY", "test_placeholder");
			return Map.of(
				"fundrays.mollie.enabled", "true",
				"fundrays.mollie.api-key", apiKey);
		}
	}

	@Inject
	MollieApiAdapter mollieApiAdapter;

	@BeforeEach
	void skipIfNoRealKey()
	{
		String key = System.getenv("FUNDRAYS_MOLLIE_API_KEY");
		assumeTrue(
			key != null && key.startsWith("test_") && key.length() > 20,
			"Skipping Mollie live tests — paste your test key into .env to run them");
	}

	@Test
	void createPayment_returnsPaymentIdAndCheckoutUrl()
	{
		// given
		Donation donation = aDonation();

		// when
		PaymentInitiation result = mollieApiAdapter.createPayment(
			donation,
			URI.create("http://localhost:8080/donate/test/thanks"),
			URI.create("http://localhost:8080/webhooks/mollie"));

		// then
		assertNotNull(result.paymentId());
		assertTrue(result.paymentId().startsWith("tr_"), "Expected payment ID starting with 'tr_', got: " + result.paymentId());
		assertNotNull(result.checkoutUrl());
		assertFalse(result.checkoutUrl().isBlank());
		assertTrue(result.checkoutUrl().startsWith("https://"), "Checkout URL should be https");
	}

	@Test
	void fetchStatus_ofNewPayment_isOpen()
	{
		// given
		PaymentInitiation created = mollieApiAdapter.createPayment(
			aDonation(),
			URI.create("http://localhost:8080/donate/test/thanks"),
			URI.create("http://localhost:8080/webhooks/mollie"));

		// when
		PaymentResponseStatus status = mollieApiAdapter.fetchStatus(created.paymentId());

		// then
		assertEquals(PaymentResponseStatus.OPEN, status, "Freshly created payment should be OPEN");
	}

	@Test
	void createPayment_withSameIdempotencyKey_returnsSamePaymentId()
	{
		// given — same donation ID used twice
		Donation donation = aDonation();
		URI returnUrl = URI.create("http://localhost:8080/donate/test/thanks");
		URI webhookUrl = URI.create("http://localhost:8080/webhooks/mollie");

		// when
		PaymentInitiation first = mollieApiAdapter.createPayment(donation, returnUrl, webhookUrl);
		PaymentInitiation second = mollieApiAdapter.createPayment(donation, returnUrl, webhookUrl);

		// then — Mollie deduplicates by idempotency key
		assertEquals(first.paymentId(), second.paymentId(), "Idempotent calls should return the same payment");
	}

	private static Donation aDonation()
	{
		Campaign campaign = new Campaign();
		campaign.title = "Live Test Campaign";

		Donation donation = new Donation();
		donation.id = UUID.randomUUID();
		donation.amount = 500L;
		donation.currency = "EUR";
		donation.campaign = campaign;
		return donation;
	}
}
