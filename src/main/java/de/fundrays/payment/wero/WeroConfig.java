package de.fundrays.payment.wero;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "fundrays.wero")
public interface WeroConfig
{

	boolean enabled();

	String apiKey();

	String webhookSecret();
}
