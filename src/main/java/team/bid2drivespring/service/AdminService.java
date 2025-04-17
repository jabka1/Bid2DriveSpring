package team.bid2drivespring.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import team.bid2drivespring.model.User;
import team.bid2drivespring.repository.UserRepository;

import java.util.List;

@Service
public class AdminService {

    @Autowired
    private UserRepository userRepository;

    public List<User> getUsersPendingVerification() {
        return userRepository.findByVerificationPhotoUrlIsNotNullAndVerificationStatus(User.VerificationStatus.PENDING);
    }

    public void approveVerification(Long userId, String comment) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setVerified(true);
        user.setVerificationStatus(User.VerificationStatus.APPROVED);
        user.setVerificationComment(comment);

        userRepository.save(user);
    }

    public void rejectVerification(Long userId, String comment) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setVerified(false);
        user.setVerificationStatus(User.VerificationStatus.REJECTED);
        user.setVerificationComment(comment);

        userRepository.save(user);
    }
}
