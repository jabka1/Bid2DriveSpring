package team.bid2drivespring.repository;

import team.bid2drivespring.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByActivationtoken(String token);
    Optional<User> findByRecoveryToken(String recoveryToken);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
