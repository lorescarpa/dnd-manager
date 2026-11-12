package it.lorenzoscarpa.dnd.auth;

import it.lorenzoscarpa.dnd.api.model.LoginRequest;
import it.lorenzoscarpa.dnd.api.model.LoginResponse;
import it.lorenzoscarpa.dnd.api.model.LoginResponse.TokenTypeEnum;
import it.lorenzoscarpa.dnd.api.model.RegisterRequest;
import it.lorenzoscarpa.dnd.api.model.User;
import it.lorenzoscarpa.dnd.common.security.TokenService;
import it.lorenzoscarpa.dnd.user.UserEntity;
import it.lorenzoscarpa.dnd.user.UserMapper;
import it.lorenzoscarpa.dnd.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private static final int BCRYPT_MAX_BYTES = 72;

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	private final TokenService tokenService;

	AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
			AuthenticationManager authenticationManager, TokenService tokenService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.authenticationManager = authenticationManager;
		this.tokenService = tokenService;
	}

	@Transactional
	public User register(RegisterRequest request) {
		String email = normalizeEmail(request.getEmail());
		if (exceedsBcryptLimit(request.getPassword())) {
			throw new PasswordTooLongException(BCRYPT_MAX_BYTES);
		}
		if (userRepository.existsByEmail(email)) {
			throw new EmailAlreadyUsedException();
		}
		UserEntity user = new UserEntity(email, passwordEncoder.encode(request.getPassword()), request.getDisplayName());
		try {
			// Flush now so a concurrent registration with the same email fails here, as a 409,
			// instead of at commit time.
			userRepository.saveAndFlush(user);
		} catch (DataIntegrityViolationException e) {
			throw new EmailAlreadyUsedException();
		}
		return UserMapper.toDto(user);
	}

	/** Throws BadCredentialsException for unknown email or wrong password (same error for both). */
	public LoginResponse login(LoginRequest request) {
		if (exceedsBcryptLimit(request.getPassword())) {
			// No stored password can be this long; BCrypt would throw instead of returning false.
			throw new BadCredentialsException("Bad credentials");
		}
		Authentication authentication = authenticationManager.authenticate(
				UsernamePasswordAuthenticationToken.unauthenticated(request.getEmail(), request.getPassword()));
		UUID userId = UUID.fromString(authentication.getName());
		return new LoginResponse(tokenService.issue(userId), TokenTypeEnum.BEARER);
	}

	private static String normalizeEmail(String email) {
		return email.toLowerCase(Locale.ROOT);
	}

	private static boolean exceedsBcryptLimit(String password) {
		return password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES;
	}
}
