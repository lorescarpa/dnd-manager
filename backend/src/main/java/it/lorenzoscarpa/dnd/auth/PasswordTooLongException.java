package it.lorenzoscarpa.dnd.auth;

/** BCrypt only uses the first 72 bytes; longer passwords are rejected instead of silently truncated. */
public class PasswordTooLongException extends RuntimeException {

	public PasswordTooLongException(int maxBytes) {
		super("Password must not exceed " + maxBytes + " bytes in UTF-8");
	}
}
