package it.lorenzoscarpa.dnd.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import it.lorenzoscarpa.dnd.api.model.LoginResponse;
import it.lorenzoscarpa.dnd.api.model.LoginResponse.TokenTypeEnum;
import it.lorenzoscarpa.dnd.api.model.RegisterRequest;
import it.lorenzoscarpa.dnd.api.model.User;
import it.lorenzoscarpa.dnd.common.security.SecurityTestConfig;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Web layer only: real security chain and error handler, AuthService mocked.
 * No Authorization header is ever sent, which also proves both endpoints are public.
 */
@WebMvcTest(AuthController.class)
@Import(SecurityTestConfig.class)
class AuthControllerTest {

	private static final String VALID_REGISTRATION = """
			{ "email": "mario@example.com", "password": "password123", "displayName": "Mario" }
			""";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AuthService authService;

	@MockitoBean // required by the AuthenticationManager bean in SecurityConfig
	private UserDetailsService userDetailsService;

	// ---------- register ----------

	@Test
	void registerReturns201WithTheCreatedUser() throws Exception {
		UUID id = UUID.randomUUID();
		given(authService.register(any())).willReturn(new User(id, "mario@example.com", "Mario",
				OffsetDateTime.of(2026, 11, 12, 10, 0, 0, 0, ZoneOffset.UTC)));

		postJson("/auth/register", VALID_REGISTRATION)
				.andExpect(status().isCreated())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.id").value(id.toString()))
				.andExpect(jsonPath("$.email").value("mario@example.com"))
				.andExpect(jsonPath("$.displayName").value("Mario"))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist());

		ArgumentCaptor<RegisterRequest> request = ArgumentCaptor.forClass(RegisterRequest.class);
		verify(authService).register(request.capture());
		assertThat(request.getValue().getPassword()).isEqualTo("password123");
	}

	@Test
	void registerWithInvalidFieldsReturns400WithEveryInvalidField() throws Exception {
		postJson("/auth/register", """
				{ "email": "not-an-email", "password": "short", "displayName": "Xy" }
				""")
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.errors[*].field").value(contains("displayName", "email", "password")))
				.andExpect(jsonPath("$.errors[0].message").value("size must be between 3 and 30"));

		verify(authService, never()).register(any());
	}

	@Test
	void registerWithMissingFieldsReturns400() throws Exception {
		postJson("/auth/register", "{}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[*].field").value(contains("displayName", "email", "password")));
	}

	@Test
	void registerWithFieldOutsideTheContractReturns400() throws Exception {
		postJson("/auth/register", """
				{ "email": "mario@example.com", "password": "password123", "displayName": "Mario",
				  "ownerId": "00000000-0000-0000-0000-000000000000" }
				""")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("ownerId"))
				.andExpect(jsonPath("$.errors[0].message").value("Unknown field"));

		verify(authService, never()).register(any());
	}

	@Test
	void registerWithMalformedJsonReturns400() throws Exception {
		postJson("/auth/register", "{ \"email\": ")
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.errors").isArray());
	}

	@Test
	void registerWithAlreadyUsedEmailReturns409() throws Exception {
		given(authService.register(any())).willThrow(new EmailAlreadyUsedException());

		postJson("/auth/register", VALID_REGISTRATION)
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.title").value("Conflict"));
	}

	@Test
	void registerWithPasswordOverBcryptLimitReturns400OnPassword() throws Exception {
		given(authService.register(any())).willThrow(new PasswordTooLongException(72));

		postJson("/auth/register", VALID_REGISTRATION)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("password"));
	}

	// ---------- login ----------

	@Test
	void loginReturnsBearerToken() throws Exception {
		given(authService.login(any())).willReturn(new LoginResponse("token-value", TokenTypeEnum.BEARER));

		postJson("/auth/login", """
				{ "email": "mario@example.com", "password": "password123" }
				""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").value("token-value"))
				.andExpect(jsonPath("$.tokenType").value("Bearer"));
	}

	@Test
	void loginWithWrongCredentialsReturns401() throws Exception {
		given(authService.login(any())).willThrow(new BadCredentialsException("Bad credentials"));

		postJson("/auth/login", """
				{ "email": "mario@example.com", "password": "wrong-password" }
				""")
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.detail").value("Invalid email or password"));
	}

	@Test
	void loginWithMissingFieldsReturns400() throws Exception {
		postJson("/auth/login", "{}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[*].field").value(contains("email", "password")));

		verify(authService, never()).login(any());
	}

	private ResultActions postJson(String path, String body) throws Exception {
		return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body));
	}
}
