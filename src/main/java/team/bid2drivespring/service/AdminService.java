package team.bid2drivespring.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import team.bid2drivespring.model.User;
import team.bid2drivespring.repository.UserRepository;


@Service
public class AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    public Page<User> getUsersPendingVerification(Pageable pageable) {
        return userRepository.findByVerificationPhotoUrlIsNotNullAndVerificationStatus(User.VerificationStatus.PENDING, pageable);
    }

    public void approveVerification(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getVerificationStatus() == User.VerificationStatus.APPROVED ||
                user.getVerificationStatus() == User.VerificationStatus.REJECTED) {
            throw new IllegalStateException("User verification has already been processed");
        }

        user.setVerified(true);
        user.setVerificationStatus(User.VerificationStatus.APPROVED);
        user.setVerificationComment(null);
        emailService.sendVerificationApprovedEmail(user.getEmail());

        userRepository.save(user);
    }

    public void rejectVerification(Long userId, String comment) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getVerificationStatus() == User.VerificationStatus.APPROVED ||
                user.getVerificationStatus() == User.VerificationStatus.REJECTED) {
            throw new IllegalStateException("User verification has already been processed");
        }

        user.setVerified(false);
        user.setVerificationStatus(User.VerificationStatus.REJECTED);
        user.setVerificationComment(comment);
        emailService.sendVerificationRejectedEmail(user.getEmail());

        userRepository.save(user);
    }

}
