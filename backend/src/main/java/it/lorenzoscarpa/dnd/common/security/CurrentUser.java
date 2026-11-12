package it.lorenzoscarpa.dnd.common.security;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Id of the authenticated user, read from the validated JWT. This is the only source for
 * ownerId and dmId: never take them from the request body.
 *
 * <p>Controllers implement generated interfaces, so they cannot add an
 * {@code @AuthenticationPrincipal} parameter; they inject this component instead.
 */
@Component
public class CurrentUser {

	public UUID id() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
			throw new IllegalStateException("No JWT-authenticated user in the security context");
		}
		return UUID.fromString(jwtAuthentication.getToken().getSubject());
	}
}
