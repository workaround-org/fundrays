package de.fundrays.payment.mollie;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.jboss.resteasy.reactive.server.ServerRequestFilter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.ContainerRequestContext;

/**
 * The Mollie webhook is a machine-to-machine form-encoded POST — no browser
 * CSRF token involved. Runs before the quarkus-rest-csrf filter (priority 10 <
 * default 5000) to extract the payment ID from the form body and re-label the
 * content type as JSON, causing the CSRF filter to skip its form-token check
 * (require-form-url-encoded=false skips non-form requests).
 */
@ApplicationScoped
class MollieWebhookCsrfBypassFilter
{
	static final String PAYMENT_ID_KEY = "mollie.webhook.paymentId";

	@ServerRequestFilter(priority = 10)
	public void filter(ContainerRequestContext requestContext) throws IOException
	{
		if (!"POST".equals(requestContext.getMethod())
			|| !"/webhooks/mollie".equals(requestContext.getUriInfo().getPath()))
		{
			return;
		}

		InputStream stream = requestContext.getEntityStream();
		if (stream == null)
		{
			return;
		}

		byte[] body = stream.readAllBytes();
		requestContext.setEntityStream(new ByteArrayInputStream(body));

		requestContext.setProperty(PAYMENT_ID_KEY,
			parseFormParam(new String(body, StandardCharsets.UTF_8), "id"));

		requestContext.getHeaders().putSingle("Content-Type", "application/json");
	}

	private static String parseFormParam(String body, String key)
	{
		for (String pair : body.split("&"))
		{
			int eq = pair.indexOf('=');
			if (eq > 0)
			{
				String name = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
				if (key.equals(name))
				{
					return URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
				}
			}
		}
		return null;
	}
}
