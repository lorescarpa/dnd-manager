package it.lorenzoscarpa.dnd.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;

/** Plain unit test: real Nimbus encoder/decoder, no Spring context. */
class TokenServiceTest {

	private static final Duration EXPIRATION = Duration.ofHours(3);

	private final JwtProperties properties = new JwtProperties("unit-test-secret-for-hs256-at-least-32-bytes",
			EXPIRATION);
	private final JwtConfig jwtConfig = new JwtConfig(properties);
	private final JwtDecoder decoder = jwtConfig.jwtDecoder();

	@Test
	void issuedTokenCarriesUserIdAndConfiguredExpiration() {
		UUID userId = UUID.randomUUID();
		Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

		Jwt jwt = decoder.decode(tokenServiceAt(now).issue(userId));

		assertThat(jwt.getSubject()).isEqualTo(userId.toString());
		assertThat(jwt.getIssuedAt()).isEqualTo(now);
		assertThat(jwt.getExpiresAt()).isEqualTo(now.plus(EXPIRATION));
		assertThat(String.valueOf(jwt.getHeaders().get("alg"))).isEqualTo("HS256");
	}

	@Test
	void expiredTokenIsRejected() {
		// Issued long enough ago to be past expiration plus the decoder's 60 s clock skew.
		Instant issuedAt = Instant.now().minus(EXPIRATION).minus(Duration.ofMinutes(2));
		String token = tokenServiceAt(issuedAt).issue(UUID.randomUUID());

		assertThatExceptionOfType(JwtValidationException.class).isThrownBy(() -> decoder.decode(token));
	}

	@Test
	void tokenSignedWithAnotherSecretIsRejected() {
		JwtProperties otherProperties = new JwtProperties("another-secret-for-hs256-at-least-32-bytes!",
				EXPIRATION);
		TokenService otherIssuer = new TokenService(new JwtConfig(otherProperties).jwtEncoder(), otherProperties,
				Clock.systemUTC());
		String token = otherIssuer.issue(UUID.randomUUID());

		assertThatExceptionOfType(BadJwtException.class).isThrownBy(() -> decoder.decode(token));
	}

	private TokenService tokenServiceAt(Instant now) {
		return new TokenService(jwtConfig.jwtEncoder(), properties, Clock.fixed(now, ZoneOffset.UTC));
	}
}
