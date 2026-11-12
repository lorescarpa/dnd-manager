package it.lorenzoscarpa.dnd.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class JwtPropertiesTest {

	private static final Duration ONE_HOUR = Duration.ofHours(1);

	@Test
	void rejectsMissingSecret() {
		assertThatIllegalStateException().isThrownBy(() -> new JwtProperties(null, ONE_HOUR))
				.withMessageContaining("JWT_SECRET");
		assertThatIllegalStateException().isThrownBy(() -> new JwtProperties("   ", ONE_HOUR));
	}

	@Test
	void requiresAtLeast32BytesOfSecret() {
		assertThatIllegalStateException().isThrownBy(() -> new JwtProperties("x".repeat(31), ONE_HOUR))
				.withMessageContaining("32 bytes");
		assertThat(new JwtProperties("x".repeat(32), ONE_HOUR).secret()).hasSize(32);
	}

	@Test
	void rejectsNonPositiveExpiration() {
		String secret = "x".repeat(32);
		assertThatIllegalStateException().isThrownBy(() -> new JwtProperties(secret, Duration.ZERO));
		assertThatIllegalStateException().isThrownBy(() -> new JwtProperties(secret, Duration.ofMinutes(-1)));
	}

	@Test
	void toStringDoesNotExposeTheSecret() {
		String secret = "super-secret-value-that-must-not-leak";
		assertThat(new JwtProperties(secret, ONE_HOUR).toString()).doesNotContain(secret);
	}
}
