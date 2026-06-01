package de.fundrays.donation.repository;

import de.fundrays.campaign.domain.Campaign;
import de.fundrays.campaign.domain.CampaignStatus;
import de.fundrays.campaign.repository.CampaignRepository;
import de.fundrays.donation.domain.Donation;
import de.fundrays.donation.domain.DonationStatus;
import de.fundrays.donation.domain.PaymentMethod;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestTransaction
class DonationRepositoryTest
{

	@Inject
	DonationRepository donationRepository;

	@Inject
	CampaignRepository campaignRepository;

	@BeforeEach
	void setup()
	{
		donationRepository.deleteAll();
		campaignRepository.deleteAll();
	}

	@Test
	void findByCampaignId_returnsOnlyDonationsForThatCampaign()
	{
		// given
		Campaign campaign = aCampaign();
		campaignRepository.persist(campaign);
		donationRepository.persist(aDonation(campaign, 500L, DonationStatus.CONFIRMED, "ref-1"));
		donationRepository.persist(aDonation(campaign, 1000L, DonationStatus.PENDING, "ref-2"));

		// when
		List<Donation> result = donationRepository.findByCampaignId(campaign.id);

		// then
		assertEquals(2, result.size());
	}

	@Test
	void findByProviderRef_returnsMatchingDonation()
	{
		// given
		Campaign campaign = aCampaign();
		campaignRepository.persist(campaign);
		donationRepository.persist(aDonation(campaign, 500L, DonationStatus.PENDING, "paypal-txn-42"));

		// when
		Optional<Donation> result = donationRepository.findByProviderRef("paypal-txn-42");

		// then
		assertTrue(result.isPresent());
		assertEquals(500L, result.get().amount);
	}

	@Test
	void findByProviderRef_returnsEmptyForUnknownRef()
	{
		// given — no donations in DB

		// when
		Optional<Donation> result = donationRepository.findByProviderRef("unknown-ref");

		// then
		assertTrue(result.isEmpty());
	}

	@Test
	void sumConfirmedByCampaignId_sumsOnlyConfirmedDonations()
	{
		// given
		Campaign campaign = aCampaign();
		campaignRepository.persist(campaign);
		donationRepository.persist(aDonation(campaign, 1000L, DonationStatus.CONFIRMED, "ref-1"));
		donationRepository.persist(aDonation(campaign, 2000L, DonationStatus.CONFIRMED, "ref-2"));
		donationRepository.persist(aDonation(campaign, 500L, DonationStatus.PENDING, "ref-3"));

		// when
		long total = donationRepository.sumConfirmedByCampaignId(campaign.id);

		// then
		assertEquals(3000L, total);
	}

	@Test
	void sumConfirmedByCampaignId_returnsZeroWhenNoDonations()
	{
		// given
		Campaign campaign = aCampaign();
		campaignRepository.persist(campaign);

		// when
		long total = donationRepository.sumConfirmedByCampaignId(campaign.id);

		// then
		assertEquals(0L, total);
	}

	@Test
	void countConfirmedByCampaignId_countsOnlyConfirmedDonations()
	{
		// given
		Campaign campaign = aCampaign();
		campaignRepository.persist(campaign);
		donationRepository.persist(aDonation(campaign, 1000L, DonationStatus.CONFIRMED, "ref-1"));
		donationRepository.persist(aDonation(campaign, 1000L, DonationStatus.FAILED, "ref-2"));
		donationRepository.persist(aDonation(campaign, 1000L, DonationStatus.CONFIRMED, "ref-3"));

		// when
		long count = donationRepository.countConfirmedByCampaignId(campaign.id);

		// then
		assertEquals(2, count);
	}

	@Test
	void listRecentConfirmedMessagesByCampaignId_returnsOnlyConfirmedWithMessageNewestFirst()
	{
		// given
		Campaign campaign = aCampaign();
		campaignRepository.persist(campaign);
		donationRepository.persist(aDonationWithMessage(campaign, DonationStatus.CONFIRMED, "Erste", Instant.now().minusSeconds(30)));
		donationRepository.persist(aDonationWithMessage(campaign, DonationStatus.CONFIRMED, "Zweite", Instant.now().minusSeconds(10)));
		donationRepository.persist(aDonationWithMessage(campaign, DonationStatus.PENDING, "Pending – ignoriert", Instant.now()));
		donationRepository.persist(aDonationWithMessage(campaign, DonationStatus.CONFIRMED, null, Instant.now()));
		donationRepository.persist(aDonationWithMessage(campaign, DonationStatus.CONFIRMED, "   ", Instant.now()));

		// when
		List<Donation> result = donationRepository.listRecentConfirmedMessagesByCampaignId(campaign.id, 5);

		// then
		assertEquals(2, result.size());
		assertEquals("Zweite", result.get(0).message);
		assertEquals("Erste", result.get(1).message);
	}

	@Test
	void listRecentConfirmedMessagesByCampaignId_respectsLimit()
	{
		// given
		Campaign campaign = aCampaign();
		campaignRepository.persist(campaign);
		for (int i = 0; i < 5; i++)
		{
			donationRepository.persist(aDonationWithMessage(campaign, DonationStatus.CONFIRMED, "Nachricht " + i, Instant.now().minusSeconds(60 - i)));
		}

		// when
		List<Donation> result = donationRepository.listRecentConfirmedMessagesByCampaignId(campaign.id, 3);

		// then
		assertEquals(3, result.size());
	}

	private Campaign aCampaign()
	{
		Campaign c = new Campaign();
		c.slug = "test-campaign-" + System.nanoTime();
		c.title = "Test Campaign";
		c.goalAmount = 100000L;
		c.createdAt = Instant.now();
		c.status = CampaignStatus.ACTIVE;
		return c;
	}

	private Donation aDonation(Campaign c, long amount, DonationStatus status, String providerRef)
	{
		Donation d = new Donation();
		d.campaign = c;
		d.amount = amount;
		d.status = status;
		d.paymentMethod = PaymentMethod.PAYPAL;
		d.paymentProviderRef = providerRef;
		d.createdAt = Instant.now();
		return d;
	}

	private Donation aDonationWithMessage(Campaign c, DonationStatus status, String message, Instant createdAt)
	{
		Donation d = new Donation();
		d.campaign = c;
		d.amount = 1000L;
		d.status = status;
		d.paymentMethod = PaymentMethod.PAYPAL;
		d.message = message;
		d.paymentProviderRef = "ref-" + System.nanoTime();
		d.createdAt = createdAt;
		return d;
	}
}
