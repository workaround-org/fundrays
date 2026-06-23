package de.fundrays.payment.mollie;

import com.mollie.mollie.models.components.Security;
import com.mollie.mollie.utils.SpeakeasyMetadata;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Native-image reflection registration for the Mollie SDK's security wiring.
 *
 * <p>
 * {@code com.mollie.mollie.utils.Security#configureSecurity} builds the
 * {@code Authorization: Bearer <key>} header by reflecting over the
 * {@link Security} component's declared fields and reading their
 * {@link SpeakeasyMetadata} annotation (the bearer scheme is declared as
 * {@code type=http,subtype=bearer,name=Authorization}). The annotation has
 * {@code RUNTIME} retention, so this works on the JVM out of the box.
 *
 * <p>
 * In a native image, reflective field access and runtime annotations are
 * stripped unless the classes are explicitly registered. Without this
 * registration the SDK fails to detect the bearer scheme, emits a malformed
 * Authorization header, and Mollie rejects the request with HTTP 400 "Invalid
 * Authorization header" (note: a wrong key would be 401).
 */
@RegisterForReflection(targets = {
	Security.class,
	SpeakeasyMetadata.class
}, classNames = {
	// package-private SDK class read while parsing the bearer scheme
	"com.mollie.mollie.utils.SecurityMetadata"
})
public final class MollieReflectionConfig
{
	private MollieReflectionConfig()
	{
	}
}
