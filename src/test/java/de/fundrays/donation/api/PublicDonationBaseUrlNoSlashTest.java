package de.fundrays.donation.api;

import de.fundrays.campaign.domain.Campaign;
import de.fundrays.campaign.domain.CampaignStatus;
import de.fundrays.campaign.repository.CampaignRepository;
import de.fundrays.payment.mollie.MollieGateway;
import de.fundrays.payment.mollie.PaymentInitiation;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * When {@code fundrays.base-url} is configured WITHOUT a trailing slash (e.g.
 * {@code FUNDRAYS_BASE_URL=https://fundrays.de}), the redirect/webhook URLs
 * must still be well-formed — not {@code https://fundrays.dedonate/...}. The
 * resource must normalize the base URL rather than blindly concatenating the
 * path.
 */
@QuarkusTest
@TestProfile(PublicDonationBaseUrlNoSlashTest.NoSlashBaseUrlProfile.class)
class PublicDonationBaseUrlNoSlashTest
{
	public static class NoSlashBaseUrlProfile implements QuarkusTestProfile
	{
		@Override
		public Map<String, String> getConfigOverrides()
		{
			return Map.of("fundrays.base-url", "https://fundrays.de");
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
	void donate_normalizesBaseUrlWithoutTrailingSlash()
	{
		// given — an active campaign and a base URL with no trailing slash
		QuarkusTransaction.requiringNew()
			.run(() -> campaignRepository.persist(aCampaign("test-fundrays", CampaignStatus.ACTIVE)));

		// when — a donation is submitted
		given()
			.contentType(ContentType.JSON)
			.body("{\"amount\":1500,\"paymentMethod\":\"MOLLIE\"}")
			.when().post("/api/public/campaigns/test-fundrays/donate")
			.then().statusCode(200);

		// then — the URLs are well-formed (single slash after the host)
		var returnUrl = ArgumentCaptor.forClass(URI.class);
		var webhookUrl = ArgumentCaptor.forClass(URI.class);
		verify(mollieGateway).initiate(any(), returnUrl.capture(), webhookUrl.capture());
		assertEquals("https://fundrays.de/donate/test-fundrays/thanks",
			returnUrl.getValue().toString());
		assertEquals("https://fundrays.de/webhooks/mollie",
			webhookUrl.getValue().toString());
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
