package it.lorenzoscarpa.dnd.common.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * JWT settings bound from {@code app.jwt.*}. Invalid values stop the application at startup.
 *
 * @param secret     HMAC key; must be at least 32 bytes (256 bits) for HS256
 * @param expiration how long an issued token stays valid
 */
@ConfigurationProperties("app.jwt")
public record JwtProperties(String secret, @DefaultValue("PT3H") Duration expiration) {

	private static final int MIN_SECRET_BYTES = 32;

	public JwtProperties {
		if (secret == null || secret.isBlank()) {
			throw new IllegalStateException("JWT secret is missing: set the JWT_SECRET environment variable");
		}
		if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
			throw new IllegalStateException(
					"JWT secret is too short: HS256 needs at least " + MIN_SECRET_BYTES + " bytes");
		}
		if (expiration.isNegative() || expiration.isZero()) {
			throw new IllegalStateException("JWT expiration must be positive");
		}
	}

	// Never print the secret, even by accident (records include all fields in toString by default).
	@Override
	public String toString() {
		return "JwtProperties[secret=***, expiration=" + expiration + "]";
	}
}
