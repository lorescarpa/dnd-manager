package it.lorenzoscarpa.dnd.auth;

import it.lorenzoscarpa.dnd.user.UserRepository;
import java.util.List;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads a user by email for the login check. The returned "username" is the user id,
 * so after a successful login {@code authentication.getName()} is the id to put in the JWT.
 */
@Service
class EmailUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	EmailUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String email) {
		return userRepository.findByEmail(email)
				.map(user -> User.withUsername(user.getId().toString())
						.password(user.getPasswordHash())
						.authorities(List.of()) // no roles: permissions depend on ownership, not on roles
						.build())
				.orElseThrow(() -> new UsernameNotFoundException("Unknown email"));
	}
}
