package de.fundrays.payment.mollie;

import com.mollie.mollie.Client;
import com.mollie.mollie.models.components.Security;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

@ApplicationScoped
public class MollieClientProducer
{
	@Inject
	MollieConfig config;

	@Produces
	@ApplicationScoped
	public Client mollieClient()
	{
		return Client.builder()
			.security(Security.builder()
				.apiKey(config.apiKey())
				.build())
			.build();
	}
}
