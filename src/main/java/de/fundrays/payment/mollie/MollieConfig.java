package de.fundrays.payment.mollie;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "fundrays.mollie")
public interface MollieConfig
{
	boolean enabled();

	String apiKey();
}
