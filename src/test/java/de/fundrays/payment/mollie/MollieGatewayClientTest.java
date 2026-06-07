package de.fundrays.payment.mollie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.fundrays.campaign.domain.Campaign;
import de.fundrays.donation.domain.Donation;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;

import java.net.URI;
import java.util.UUID;

@QuarkusTest
class MollieGatewayClientTest
{
	@Inject
	MollieGateway mollieGateway;

	@InjectMock
	MollieApiAdapter mollieApiAdapter;

	@Test
	void initiate_retriesTransientFailuresUpToThreeAttempts()
	{
		// given
		var expected = new PaymentInitiation("tr_retry", "https://checkout.mollie.com/pay/tr_retry");
		when(mollieApiAdapter.createPayment(any(), any(), any()))
			.thenThrow(new MollieTransientGatewayException("gateway unavailable"))
			.thenThrow(new MollieTransientGatewayException("gateway unavailable"))
			.thenReturn(expected);

		// when
		PaymentInitiation result = mollieGateway.initiate(
			aDonation(),
			URI.create("https://fundrays.example/thanks"),
			URI.create("https://fundrays.example/webhooks/mollie"));

		// then
		assertEquals("tr_retry", result.paymentId());
		verify(mollieApiAdapter, times(3)).createPayment(any(), any(), any());
	}

	@Test
	void initiate_doesNotRetryPermanentGatewayRejection()
	{
		// given
		when(mollieApiAdapter.createPayment(any(), any(), any()))
			.thenThrow(new MollieGatewayException("payment rejected"));

		// when / then
		assertThrows(MollieGatewayException.class, () -> mollieGateway.initiate(
			aDonation(),
			URI.create("https://fundrays.example/thanks"),
			URI.create("https://fundrays.example/webhooks/mollie")));
		verify(mollieApiAdapter).createPayment(any(), any(), any());
	}

	private Donation aDonation()
	{
		Campaign campaign = new Campaign();
		campaign.title = "Test Campaign";

		Donation donation = new Donation();
		donation.id = UUID.randomUUID();
		donation.amount = 1500L;
		donation.currency = "EUR";
		donation.campaign = campaign;
		return donation;
	}
}
