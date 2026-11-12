package it.lorenzoscarpa.dnd.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import it.lorenzoscarpa.dnd.TestcontainersConfiguration;
import it.lorenzoscarpa.dnd.common.security.TokenService;
import it.lorenzoscarpa.dnd.user.UserEntity;
import it.lorenzoscarpa.dnd.user.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Whole application against PostgreSQL: security filters, JWT, services, Flyway schema.
 * Every test uses its own email, so tests do not depend on each other's data.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthIntegrationTest {

	private static final String PASSWORD = "password123";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private TokenService tokenService;

	@Autowired
	private JwtEncoder jwtEncoder;

	@Test
	void registerLoginAndReadOwnProfile() throws Exception {
		String email = uniqueEmail();
		String id = JsonPath.read(register(email.toUpperCase(), "Mario")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value(email)) // stored lower-case
				.andExpect(jsonPath("$.passwordHash").doesNotExist())
				.andReturn().getResponse().getContentAsString(), "$.id");

		String token = loginToken(email.toUpperCase(), PASSWORD);

		getMe(token)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.email").value(email))
				.andExpect(jsonPath("$.displayName").value("Mario"))
				.andExpect(jsonPath("$.createdAt").exists());
	}

	@Test
	void passwordIsStoredAsBcryptHash() throws Exception {
		String email = uniqueEmail();
		register(email, "Mario").andExpect(status().isCreated());

		String hash = userRepository.findByEmail(email).orElseThrow().getPasswordHash();
		assertThat(hash).startsWith("$2").isNotEqualTo(PASSWORD);
		assertThat(passwordEncoder.matches(PASSWORD, hash)).isTrue();
	}

	@Test
	void registeringTheSameEmailWithDifferentCaseReturns409() throws Exception {
		String email = uniqueEmail();
		register(email, "Mario").andExpect(status().isCreated());

		register(email.toUpperCase(), "Mario Bis")
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void loginWithWrongPasswordOrUnknownEmailReturnsTheSame401() throws Exception {
		String email = uniqueEmail();
		register(email, "Mario").andExpect(status().isCreated());

		login(email, "wrong-password")
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.detail").value("Invalid email or password"));
		login(uniqueEmail(), PASSWORD)
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.detail").value("Invalid email or password"));
	}

	@Test
	void loginWithPasswordOverBcryptLimitReturns401() throws Exception {
		login(uniqueEmail(), "€".repeat(25)) // 25 characters, 75 bytes
				.andExpect(status().isUnauthorized());
	}

	@Test
	void protectedEndpointWithoutTokenReturns401() throws Exception {
		mockMvc.perform(get("/users/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")))
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(401));
	}

	@Test
	void tamperedTokenReturns401() throws Exception {
		String email = uniqueEmail();
		register(email, "Mario").andExpect(status().isCreated());
		String token = loginToken(email, PASSWORD);
		String tampered = token.substring(0, token.length() - 4) + (token.endsWith("AAAA") ? "BBBB" : "AAAA");

		getMe(tampered)
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void expiredTokenReturns401() throws Exception {
		String email = uniqueEmail();
		register(email, "Mario").andExpect(status().isCreated());
		UUID userId = userRepository.findByEmail(email).map(UserEntity::getId).orElseThrow();

		// Correctly signed, but expired two hours ago (well past the 60 s clock skew).
		Instant issuedAt = Instant.now().minus(5, ChronoUnit.HOURS);
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject(userId.toString())
				.issuedAt(issuedAt)
				.expiresAt(issuedAt.plus(3, ChronoUnit.HOURS))
				.build();
		String expired = jwtEncoder.encode(
				JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

		getMe(expired).andExpect(status().isUnauthorized());
	}

	@Test
	void validTokenOfUnknownUserReturns401() throws Exception {
		getMe(tokenService.issue(UUID.randomUUID()))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	// ---------- helpers ----------

	private static String uniqueEmail() {
		return "user-" + UUID.randomUUID() + "@example.com";
	}

	private ResultActions register(String email, String displayName) throws Exception {
		return mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
				{ "email": "%s", "password": "%s", "displayName": "%s" }
				""".formatted(email, PASSWORD, displayName)));
	}

	private ResultActions login(String email, String password) throws Exception {
		return mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
				{ "email": "%s", "password": "%s" }
				""".formatted(email, password)));
	}

	private String loginToken(String email, String password) throws Exception {
		String body = login(email, password).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.accessToken");
	}

	private ResultActions getMe(String token) throws Exception {
		return mockMvc.perform(get("/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
	}
}
