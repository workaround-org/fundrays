package de.fundrays.donation.api;

import de.fundrays.campaign.domain.Campaign;
import de.fundrays.campaign.domain.CampaignStatus;
import de.fundrays.campaign.repository.CampaignRepository;
import de.fundrays.payment.mollie.MollieGateway;
import de.fundrays.payment.mollie.PaymentInitiation;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import jakarta.inject.Inject;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Mollie rejects a redirect URL pointing at localhost / a non-public host with
 * HTTP 422. The redirect URL must therefore be built from the configured public
 * base URL ({@code fundrays.base-url}) like the webhook URL — not from the
 * incoming request URI, whose host is whatever the client connected to (e.g.
 * localhost behind a proxy / in docker).
 */
@QuarkusTest
@TestProfile(PublicDonationRedirectUrlTest.PublicBaseUrlProfile.class)
class PublicDonationRedirectUrlTest
{
	public static class PublicBaseUrlProfile implements QuarkusTestProfile
	{
		@Override
		public Map<String, String> getConfigOverrides()
		{
			return Map.of("fundrays.base-url", "https://public.fundrays.test/");
		}
	}

	@Inject
	CampaignRepository campaignRepository;

	@InjectMock
	MollieGateway mollieGateway;

	@BeforeEach
	void setup()
	{
		QuarkusTransaction.requiringNew().run(campaignRepository::deleteAll);
		when(mollieGateway.initiate(any(), any(), any()))
			.thenReturn(new PaymentInitiation(
				"tr_test1",
				"https://checkout.mollie.com/pay/tr_test1"));
	}

	@Test
	void donate_buildsRedirectUrlFromPublicBaseUrl_notRequestHost()
	{
		// given — an active campaign and a configured public base URL
		QuarkusTransaction.requiringNew()
			.run(() -> campaignRepository.persist(aCampaign("active-campaign", CampaignStatus.ACTIVE)));

		// when — a donation is submitted against the localhost test host
		given()
			.contentType(ContentType.JSON)
			.body("{\"amount\":1500,\"paymentMethod\":\"MOLLIE\"}")
			.when().post("/api/public/campaigns/active-campaign/donate")
			.then().statusCode(200);

		// then — the redirect URL handed to Mollie uses the public base URL
		// host,
		// not the localhost request host that would trigger a Mollie 422
		var returnUrl = ArgumentCaptor.forClass(URI.class);
		verify(mollieGateway).initiate(any(), returnUrl.capture(),
			eq(URI.create("https://public.fundrays.test/webhooks/mollie")));
		assertEquals("public.fundrays.test", returnUrl.getValue().getHost());
		assertEquals("https://public.fundrays.test/donate/active-campaign/thanks",
			returnUrl.getValue().toString());
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
