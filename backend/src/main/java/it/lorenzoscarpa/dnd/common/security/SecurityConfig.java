package it.lorenzoscarpa.dnd.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * HTTP security rules. Declaring a SecurityFilterChain bean replaces Spring Boot's
 * default chain (form login + HTTP Basic on every URL).
 */
@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityProblemHandler problemHandler)
			throws Exception {
		http
				// CSRF protects cookie-based sessions; we authenticate with a header token instead.
				.csrf(AbstractHttpConfigurer::disable)
				// No HttpSession: every request must carry its own credentials.
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.POST, "/auth/register", "/auth/login").permitAll()
						// Spring forwards failed requests to /error; keep it open so the real
						// status (e.g. 400) is not hidden behind a 401.
						.requestMatchers("/error").permitAll()
						// API documentation: Swagger UI, its config and the contract file.
						.requestMatchers(HttpMethod.GET, "/swagger-ui.html", "/swagger-ui/**",
								"/v3/api-docs/swagger-config", "/openapi/api.yaml").permitAll()
						.anyRequest().authenticated())
				// Reads "Authorization: Bearer <jwt>", validates it with the JwtDecoder bean
				// and, if valid, marks the request as authenticated.
				.oauth2ResourceServer(resourceServer -> resourceServer
						.jwt(Customizer.withDefaults())
						.authenticationEntryPoint(problemHandler)
						.accessDeniedHandler(problemHandler))
				// Same handler when no token is sent at all.
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint(problemHandler)
						.accessDeniedHandler(problemHandler));
		return http.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	/**
	 * Checks email + password at login. DaoAuthenticationProvider loads the user through the
	 * UserDetailsService and compares the BCrypt hash. For an unknown email it still hashes the
	 * password, so response time does not reveal whether the email exists.
	 */
	@Bean
	AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		return new ProviderManager(provider);
	}
}
