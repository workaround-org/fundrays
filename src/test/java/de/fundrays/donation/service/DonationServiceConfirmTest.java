package de.fundrays.donation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class DonationServiceConfirmTest
{
	@Inject
	DonationService donationService;

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
			OrganizationSettings s = new OrganizationSettings();
			s.orgName = "Demokratie e.V.";
			s.smtpFrom = "noreply@demokratie.de";
			settingsRepository.persist(s);
		});
	}

	@AfterEach
	void cleanup()
	{
		QuarkusTransaction.requiringNew().run(() -> {
			donationRepository.deleteAll();
			campaignRepository.deleteAll();
			settingsRepository.deleteAll();
		});
	}

	@Test
	void confirm_marksConfirmedAndSendsDonorMail()
	{
		// given
		UUID donationId = persistPendingDonation("donor@example.org", "Klimakampagne", 2500L, "TX-1");

		// when
		donationService.confirm(donationId);

		// then
		QuarkusTransaction.requiringNew().run(() -> {
			Donation reloaded = donationRepository.find("id", donationId).firstResult();
			assertEquals(DonationStatus.CONFIRMED, reloaded.status);
			assertNotNull(reloaded.confirmedAt);
		});
		assertEquals(1, mailbox.getMailMessagesSentTo("donor@example.org").size());
	}

	@Test
	void confirm_isIdempotent_doesNotSendDuplicateMail()
	{
		// given
		UUID donationId = persistPendingDonation("donor@example.org", "Klimakampagne", 2500L, "TX-2");

		// when
		donationService.confirm(donationId);
		donationService.confirm(donationId);

		// then
		assertEquals(1, mailbox.getMailMessagesSentTo("donor@example.org").size());
	}

	@Test
	void confirm_doesNotSendMailWhenDonorEmailMissing()
	{
		// given
		UUID donationId = persistPendingDonation(null, "Klimakampagne", 2500L, "TX-3");

		// when
		donationService.confirm(donationId);

		// then
		assertEquals(0, mailbox.getTotalMessagesSent());
		QuarkusTransaction.requiringNew().run(() -> {
			Donation reloaded = donationRepository.find("id", donationId).firstResult();
			assertEquals(DonationStatus.CONFIRMED, reloaded.status);
		});
	}

	@Test
	void confirm_throwsWhenDonationMissing()
	{
		// given
		UUID unknown = UUID.randomUUID();

		// when / then
		assertThrows(DonationNotFoundException.class, () -> donationService.confirm(unknown));
	}

	private UUID persistPendingDonation(String donorEmail, String campaignTitle, long amountCents, String txRef)
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
			d.status = DonationStatus.PENDING;
			d.donorEmail = donorEmail;
			d.paymentProviderRef = txRef;
			d.createdAt = Instant.now();
			donationRepository.persist(d);
			return d.id;
		});
	}
}
