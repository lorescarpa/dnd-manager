package it.lorenzoscarpa.dnd.user;

import it.lorenzoscarpa.dnd.api.model.User;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

	private final UserRepository userRepository;

	UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public User getById(UUID id) {
		return userRepository.findById(id)
				.map(UserMapper::toDto)
				.orElseThrow(() -> new UserNotFoundException(id));
	}
}
