package it.lorenzoscarpa.dnd.auth;

import it.lorenzoscarpa.dnd.api.AuthApi;
import it.lorenzoscarpa.dnd.api.model.LoginRequest;
import it.lorenzoscarpa.dnd.api.model.LoginResponse;
import it.lorenzoscarpa.dnd.api.model.RegisterRequest;
import it.lorenzoscarpa.dnd.api.model.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController implements AuthApi {

	private final AuthService authService;

	AuthController(AuthService authService) {
		this.authService = authService;
	}

	@Override
	public ResponseEntity<User> register(RegisterRequest registerRequest) {
		return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(registerRequest));
	}

	@Override
	public ResponseEntity<LoginResponse> login(LoginRequest loginRequest) {
		return ResponseEntity.ok(authService.login(loginRequest));
	}
}
