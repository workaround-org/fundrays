package de.fundrays.payment.wero;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@ApplicationScoped
public class WeroWebhookSignatureVerifier
{

	private static final String HMAC_ALGORITHM = "HmacSHA256";

	@Inject
	WeroConfig config;

	public boolean isValid(String payload, String suppliedSignature)
	{
		if (payload == null || suppliedSignature == null || suppliedSignature.isBlank()
			|| config.webhookSecret() == null || config.webhookSecret().isBlank())
		{
			return false;
		}

		String signature = suppliedSignature.strip();
		if (signature.regionMatches(true, 0, "sha256=", 0, "sha256=".length()))
		{
			signature = signature.substring("sha256=".length());
		}

		try
		{
			byte[] supplied = HexFormat.of().parseHex(signature);
			Mac mac = Mac.getInstance(HMAC_ALGORITHM);
			mac.init(new SecretKeySpec(config.webhookSecret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
			byte[] expected = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
			return MessageDigest.isEqual(expected, supplied);
		}
		catch (GeneralSecurityException | IllegalArgumentException e)
		{
			return false;
		}
	}
}
