package it.lorenzoscarpa.dnd.user;

import java.util.UUID;

/** A valid token refers to a user that no longer exists (e.g. after a database reset). */
public class UserNotFoundException extends RuntimeException {

	public UserNotFoundException(UUID id) {
		super("User not found: " + id);
	}
}
