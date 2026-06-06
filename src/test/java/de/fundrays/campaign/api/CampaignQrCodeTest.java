package de.fundrays.campaign.api;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import de.fundrays.campaign.domain.Campaign;
import de.fundrays.campaign.domain.CampaignStatus;
import de.fundrays.campaign.repository.CampaignRepository;
import de.fundrays.donation.repository.DonationRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import javax.imageio.ImageIO;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class CampaignQrCodeTest
{
	private static final byte[] PNG_MAGIC = { (byte)0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n' };

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
	@TestSecurity(user = "admin", roles = { "admin" })
	void qrcode_returnsPngByDefault()
	{
		// given
		QuarkusTransaction.requiringNew().run(() -> campaignRepository.persist(aCampaign("my-campaign")));

		// when
		var response = given()
			.when().get("/api/campaigns/my-campaign/qrcode");

		// then
		response.then().statusCode(200)
			.contentType("image/png");
		byte[] body = response.then().extract().asByteArray();
		byte[] magic = new byte[8];
		System.arraycopy(body, 0, magic, 0, 8);
		assertArrayEquals(PNG_MAGIC, magic, "Response body must start with the PNG magic number");
	}

	@Test
	@TestSecurity(user = "admin", roles = { "admin" })
	void qrcode_returnsSvgWhenRequested()
	{
		// given
		QuarkusTransaction.requiringNew().run(() -> campaignRepository.persist(aCampaign("my-campaign")));

		// when
		var response = given()
			.when().get("/api/campaigns/my-campaign/qrcode?format=svg");

		// then
		response.then().statusCode(200)
			.contentType(startsWith("image/svg+xml"))
			.body(startsWith("<svg"))
			.body(containsString("<path"));
	}

	@Test
	@TestSecurity(user = "admin", roles = { "admin" })
	void qrcode_encodesDonationUrl() throws Exception
	{
		// given
		QuarkusTransaction.requiringNew().run(() -> campaignRepository.persist(aCampaign("my-campaign")));

		// when
		byte[] png = given()
			.when().get("/api/campaigns/my-campaign/qrcode")
			.then().statusCode(200)
			.extract().asByteArray();

		// then
		String decoded = decodePng(png);
		assertTrue(decoded.endsWith("/donate/my-campaign"),
			"Decoded QR payload should end with /donate/my-campaign, was: " + decoded);
		assertTrue(decoded.startsWith("http"),
			"Decoded QR payload should be an absolute URL, was: " + decoded);
	}

	@Test
	@TestSecurity(user = "admin", roles = { "admin" })
	void qrcode_includesUtmParameters() throws Exception
	{
		// given
		QuarkusTransaction.requiringNew().run(() -> campaignRepository.persist(aCampaign("my-campaign")));

		// when
		byte[] png = given()
			.when().get("/api/campaigns/my-campaign/qrcode?utm_source=flyer&utm_medium=print")
			.then().statusCode(200)
			.extract().asByteArray();

		// then
		String decoded = decodePng(png);
		assertTrue(decoded.contains("/donate/my-campaign"),
			"Decoded URL should contain donate path, was: " + decoded);
		assertTrue(decoded.contains("utm_source=flyer"),
			"Decoded URL should contain utm_source=flyer, was: " + decoded);
		assertTrue(decoded.contains("utm_medium=print"),
			"Decoded URL should contain utm_medium=print, was: " + decoded);
	}

	@Test
	@TestSecurity(user = "admin", roles = { "admin" })
	void qrcode_returns404ForUnknownSlug()
	{
		// given — no campaigns in DB

		// when
		var response = given()
			.when().get("/api/campaigns/nonexistent/qrcode");

		// then
		response.then().statusCode(404);
	}

	@Test
	void qrcode_returns401WithoutAuth()
	{
		// given
		QuarkusTransaction.requiringNew().run(() -> campaignRepository.persist(aCampaign("my-campaign")));

		// when
		var response = given()
			.when().get("/api/campaigns/my-campaign/qrcode");

		// then
		response.then().statusCode(401);
	}

	@Test
	@TestSecurity(user = "admin", roles = { "admin" })
	void qrcode_returns400ForUnknownFormat()
	{
		// given
		QuarkusTransaction.requiringNew().run(() -> campaignRepository.persist(aCampaign("my-campaign")));

		// when
		var response = given()
			.when().get("/api/campaigns/my-campaign/qrcode?format=pdf");

		// then
		response.then().statusCode(400);
	}

	private static String decodePng(byte[] bytes) throws Exception
	{
		BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
		LuminanceSource src = new BufferedImageLuminanceSource(img);
		BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(src));
		return new MultiFormatReader().decode(bitmap).getText();
	}

	private Campaign aCampaign(String slug)
	{
		Campaign c = new Campaign();
		c.slug = slug;
		c.title = "Campaign " + slug;
		c.goalAmount = 100000L;
		c.createdAt = Instant.now();
		c.status = CampaignStatus.ACTIVE;
		return c;
	}
}
