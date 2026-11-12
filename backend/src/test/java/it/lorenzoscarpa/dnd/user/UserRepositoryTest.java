package it.lorenzoscarpa.dnd.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import it.lorenzoscarpa.dnd.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

/** Runs against PostgreSQL (Testcontainers) with the Flyway schema, so the real indexes apply. */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
class UserRepositoryTest {

	@Autowired
	private UserRepository userRepository;

	@Test
	void findByEmailIgnoresCase() {
		UserEntity saved = userRepository.saveAndFlush(new UserEntity("mario@example.com", "hash", "Mario"));

		assertThat(userRepository.findByEmail("MARIO@Example.COM")).get()
				.extracting(UserEntity::getId).isEqualTo(saved.getId());
		assertThat(userRepository.findByEmail("luigi@example.com")).isEmpty();
	}

	@Test
	void existsByEmailIgnoresCase() {
		userRepository.saveAndFlush(new UserEntity("mario@example.com", "hash", "Mario"));

		assertThat(userRepository.existsByEmail("Mario@Example.com")).isTrue();
		assertThat(userRepository.existsByEmail("luigi@example.com")).isFalse();
	}

	@Test
	void databaseRejectsSameEmailWithDifferentCase() {
		userRepository.saveAndFlush(new UserEntity("mario@example.com", "hash", "Mario"));

		// Bypasses the service on purpose: the uq_users_email_lower index must hold on its own.
		assertThatExceptionOfType(DataIntegrityViolationException.class).isThrownBy(
				() -> userRepository.saveAndFlush(new UserEntity("Mario@Example.com", "hash", "Mario Bis")));
	}

	@Test
	void createdAtIsSetOnInsert() {
		UserEntity saved = userRepository.saveAndFlush(new UserEntity("mario@example.com", "hash", "Mario"));

		assertThat(saved.getCreatedAt()).isNotNull();
	}
}
