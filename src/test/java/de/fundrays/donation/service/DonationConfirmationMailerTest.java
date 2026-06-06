package de.fundrays.donation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.fundrays.campaign.domain.Campaign;
import de.fundrays.campaign.domain.CampaignStatus;
import de.fundrays.campaign.repository.CampaignRepository;
import de.fundrays.donation.domain.Donation;
import de.fundrays.donation.domain.DonationStatus;
import de.fundrays.donation.domain.PaymentMethod;
import de.fundrays.donation.repository.DonationRepository;
import de.fundrays.shared.domain.OrganizationSettings;
import de.fundrays.shared.repository.OrganizationSettingsRepository;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.ext.mail.MailMessage;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class DonationConfirmationMailerTest
{
	@Inject
	DonationConfirmationMailer mailer;

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
		cleanDb();
	}

	@AfterEach
	void cleanup()
	{
		cleanDb();
	}

	private void cleanDb()
	{
		QuarkusTransaction.requiringNew().run(() -> {
			donationRepository.deleteAll();
			campaignRepository.deleteAll();
			settingsRepository.deleteAll();
		});
	}

	@Test
	void sendConfirmation_sendsMailWithCampaignAndAmountAndReference()
	{
		// given
		Donation donation = persistConfirmedDonation("donor@example.org", "Klimakampagne", 2500L, "TX-12345");
		persistSettings("noreply@demokratie.de", null);

		// when
		mailer.sendConfirmation(donation);

		// then
		List<MailMessage> sent = mailbox.getMailMessagesSentTo("donor@example.org");
		assertEquals(1, sent.size());
		MailMessage mail = sent.getFirst();
		assertNotNull(mail.getSubject());
		String body = mail.getText() != null ? mail.getText() : mail.getHtml();
		assertNotNull(body);
		assertTrue(body.contains("Klimakampagne"), "body should mention campaign name");
		assertTrue(body.contains("25,00") || body.contains("25.00"), "body should mention amount in euros");
		assertTrue(body.contains("TX-12345"), "body should mention transaction reference");
	}

	@Test
	void sendConfirmation_usesSmtpFromAddressFromSettings()
	{
		// given
		Donation donation = persistConfirmedDonation("donor@example.org", "Bildung für alle", 1000L, "TX-1");
		persistSettings("noreply@demokratie.de", null);

		// when
		mailer.sendConfirmation(donation);

		// then
		List<MailMessage> sent = mailbox.getMailMessagesSentTo("donor@example.org");
		assertEquals(1, sent.size());
		assertEquals("noreply@demokratie.de", sent.getFirst().getFrom());
	}

	@Test
	void sendConfirmation_skipsWhenDonorEmailIsMissing()
	{
		// given
		Donation donation = persistConfirmedDonation(null, "Some Campaign", 1000L, "TX-9");
		persistSettings("noreply@demokratie.de", null);

		// when
		mailer.sendConfirmation(donation);

		// then
		assertEquals(0, mailbox.getTotalMessagesSent());
	}

	@Test
	void sendConfirmation_skipsWhenDonorEmailIsBlank()
	{
		// given
		Donation donation = persistConfirmedDonation("   ", "Some Campaign", 1000L, "TX-9");
		persistSettings("noreply@demokratie.de", null);

		// when
		mailer.sendConfirmation(donation);

		// then
		assertEquals(0, mailbox.getTotalMessagesSent());
	}

	@Test
	void sendAdminNotification_sendsWhenAdminEmailIsSet()
	{
		// given
		Donation donation = persistConfirmedDonation("donor@example.org", "Climate", 5000L, "TX-A");
		persistSettings("noreply@demokratie.de", "admin@demokratie.de");

		// when
		mailer.sendAdminNotification(donation);

		// then
		List<MailMessage> sent = mailbox.getMailMessagesSentTo("admin@demokratie.de");
		assertEquals(1, sent.size());
		String body = sent.getFirst().getText() != null ? sent.getFirst().getText() : sent.getFirst().getHtml();
		assertNotNull(body);
		assertTrue(body.contains("Climate"));
		assertTrue(body.contains("50,00") || body.contains("50.00"));
	}

	@Test
	void sendAdminNotification_skipsWhenAdminEmailIsNotSet()
	{
		// given
		Donation donation = persistConfirmedDonation("donor@example.org", "Climate", 5000L, "TX-A");
		persistSettings("noreply@demokratie.de", null);

		// when
		mailer.sendAdminNotification(donation);

		// then
		assertEquals(0, mailbox.getTotalMessagesSent());
	}

	private Donation persistConfirmedDonation(String donorEmail, String campaignTitle, long amountCents, String txRef)
	{
		return QuarkusTransaction.requiringNew().call(() -> {
			Campaign c = new Campaign();
			c.slug = "slug-" + System.nanoTime();
			c.title = campaignTitle;
			c.goalAmount = 100000L;
			c.createdAt = Instant.now();
			c.status = CampaignStatus.ACTIVE;
			campaignRepository.persist(c);

			Donation d = new Donation();
			d.campaign = c;
			d.amount = amountCents;
			d.paymentMethod = PaymentMethod.PAYPAL;
			d.status = DonationStatus.CONFIRMED;
			d.donorEmail = donorEmail;
			d.paymentProviderRef = txRef;
			d.createdAt = Instant.now();
			d.confirmedAt = Instant.now();
			donationRepository.persist(d);
			return d;
		});
	}

	private void persistSettings(String from, String adminEmail)
	{
		QuarkusTransaction.requiringNew().run(() -> {
			OrganizationSettings s = new OrganizationSettings();
			s.orgName = "Demokratie e.V.";
			s.smtpFrom = from;
			s.adminNotificationEmail = adminEmail;
			settingsRepository.persist(s);
		});
	}
}
