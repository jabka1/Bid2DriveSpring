package team.bid2drivespring.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import team.bid2drivespring.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByActivationtoken(String token);
    Optional<User> findByRecoveryToken(String recoveryToken);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    List<User> findByVerificationPhotoUrlIsNotNullAndVerificationStatus(User.VerificationStatus verificationStatus);
    Page<User> findByVerificationPhotoUrlIsNotNullAndVerificationStatus(User.VerificationStatus status, Pageable pageable);

}

