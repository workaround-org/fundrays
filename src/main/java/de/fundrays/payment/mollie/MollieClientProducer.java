package de.fundrays.payment.mollie;

import com.mollie.mollie.Client;
import com.mollie.mollie.models.components.Security;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class MollieClientProducer
{
	private static final Logger log = Logger.getLogger(MollieClientProducer.class);

	@Inject
	MollieConfig config;

	@Produces
	@ApplicationScoped
	public Client mollieClient()
	{
		String key = config.apiKey();
		String redacted = key.length() > 8 ? key.substring(0, 8) + "..." : "(short)";
		log.infof("Building Mollie client with key prefix: %s (enabled=%b)", redacted, config.enabled());
		return Client.builder()
			.security(Security.builder()
				.apiKey(key)
				.build())
			.build();
	}
}
