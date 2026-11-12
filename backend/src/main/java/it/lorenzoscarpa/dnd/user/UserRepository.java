package it.lorenzoscarpa.dnd.user;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

	// Explicit lower() so PostgreSQL can use the uq_users_email_lower index.
	// A derived "findByEmailIgnoreCase" would generate upper(), which that index does not cover.
	@Query("select u from UserEntity u where lower(u.email) = lower(:email)")
	Optional<UserEntity> findByEmail(@Param("email") String email);

	@Query("select count(u) > 0 from UserEntity u where lower(u.email) = lower(:email)")
	boolean existsByEmail(@Param("email") String email);
}
