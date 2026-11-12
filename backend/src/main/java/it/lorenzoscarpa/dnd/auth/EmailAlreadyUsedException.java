package it.lorenzoscarpa.dnd.auth;

public class EmailAlreadyUsedException extends RuntimeException {

	public EmailAlreadyUsedException() {
		super("Email already registered");
	}
}
