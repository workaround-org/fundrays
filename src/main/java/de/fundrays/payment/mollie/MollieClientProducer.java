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
		// Trim to defend against trailing newline/whitespace in the env var,
		// which
		// would otherwise produce an illegal "Authorization: Bearer <key>\n"
		// header
		// and a Mollie 400 "Invalid Authorization header".
		String key = config.apiKey().trim();
		String redacted = key.length() > 8 ? key.substring(0, 8) + "..." : "(short)";
		log.infof("Building Mollie client with key prefix: %s (enabled=%b)", redacted, config.enabled());
		return Client.builder()
			.security(Security.builder()
				.apiKey(key)
				.build())
			.build();
	}
}
