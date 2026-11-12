package it.lorenzoscarpa.dnd.user;

import it.lorenzoscarpa.dnd.api.model.User;
import java.time.ZoneOffset;

/** Converts user entities to API DTOs. passwordHash is never copied. */
public final class UserMapper {

	private UserMapper() {
	}

	public static User toDto(UserEntity entity) {
		return new User(
				entity.getId(),
				entity.getEmail(),
				entity.getDisplayName(),
				entity.getCreatedAt().atOffset(ZoneOffset.UTC));
	}
}
