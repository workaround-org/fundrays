package de.fundrays.payment.wero;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.fundrays.donation.domain.Donation;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.util.UUID;

@QuarkusTest
class WeroGatewayClientTest
{

	@Inject
	WeroGateway weroGateway;

	@InjectMock
	@RestClient
	WeroApiClient apiClient;

	@Test
	void initiate_retriesTransientFailuresUpToThreeAttempts()
	{
		// given
		when(apiClient.createPayment(anyString(), anyString(), any()))
			.thenReturn(
				Response.status(Response.Status.SERVICE_UNAVAILABLE).build(),
				Response.status(Response.Status.BAD_GATEWAY).build(),
				Response.ok(new WeroCreatePaymentResponse(
					"wero-tx-retry",
					"https://pay.wero.example/retry",
					null,
					null)).build());

		// when
		WeroPaymentInitiation result = weroGateway.initiate(
			aDonation(),
			URI.create("https://fundrays.example/thanks"),
			URI.create("https://fundrays.example/webhooks/wero"));

		// then
		assertEquals("wero-tx-retry", result.transactionId());
		verify(apiClient, times(3)).createPayment(anyString(), anyString(), any());
	}

	@Test
	void initiate_doesNotRetryPermanentGatewayRejection()
	{
		// given
		when(apiClient.createPayment(anyString(), anyString(), any()))
			.thenReturn(Response.status(Response.Status.BAD_REQUEST).build());

		// when / then
		assertThrows(WeroGatewayException.class, () -> weroGateway.initiate(
			aDonation(),
			URI.create("https://fundrays.example/thanks"),
			URI.create("https://fundrays.example/webhooks/wero")));
		verify(apiClient).createPayment(anyString(), anyString(), any());
	}

	private Donation aDonation()
	{
		Donation donation = new Donation();
		donation.id = UUID.randomUUID();
		donation.amount = 1500L;
		donation.currency = "EUR";
		return donation;
	}
}
