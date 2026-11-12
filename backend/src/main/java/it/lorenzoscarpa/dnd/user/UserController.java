package it.lorenzoscarpa.dnd.user;

import it.lorenzoscarpa.dnd.api.UsersApi;
import it.lorenzoscarpa.dnd.api.model.User;
import it.lorenzoscarpa.dnd.common.security.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController implements UsersApi {

	private final UserService userService;
	private final CurrentUser currentUser;

	UserController(UserService userService, CurrentUser currentUser) {
		this.userService = userService;
		this.currentUser = currentUser;
	}

	@Override
	public ResponseEntity<User> getCurrentUser() {
		return ResponseEntity.ok(userService.getById(currentUser.id()));
	}
}
