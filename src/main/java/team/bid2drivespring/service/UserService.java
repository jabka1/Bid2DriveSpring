package team.bid2drivespring.service;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import team.bid2drivespring.model.User;
import team.bid2drivespring.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    public void registerUser(String username, String password, String email) {
        String encodedPassword = passwordEncoder.encode(password);
        String activationToken = java.util.UUID.randomUUID().toString();

        User user = new User();
        user.setUsername(username);
        user.setPassword(encodedPassword);
        user.setEmail(email);
        user.setActivationtoken(activationToken);
        user.setActivated(false);

        userRepository.save(user);

        emailService.sendActivationEmail(user.getEmail(), activationToken);
    }

    public void updateUserProfile(String username, String newPassword, String confirmPassword) {
        User currentUser = getCurrentUser();

        if (!username.equals(currentUser.getUsername())) {
            if (isUsernameTaken(username)) {
                throw new IllegalArgumentException("Username is already taken.");
            }
            currentUser.setUsername(username);
        }

        if (newPassword != null && !newPassword.isEmpty()) {
            if (confirmPassword == null || confirmPassword.isEmpty()) {
                throw new IllegalArgumentException("Please confirm your new password.");
            }

            if (!newPassword.equals(confirmPassword)) {
                throw new IllegalArgumentException("Passwords do not match.");
            }

            if (!newPassword.matches("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!\"#$%&'()*+,-./:;<=>?@\\[\\]^_`{|}~]).{8,}$")) {
                throw new IllegalArgumentException("Password must meet the security policy (Minimum 8 characters, including a capital letter, a number and a symbol (!, @, #))");
            }

            currentUser.setPassword(passwordEncoder.encode(newPassword));
        }

        userRepository.save(currentUser);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(currentUser, null,
                        AuthorityUtils.createAuthorityList("USER"))
        );
    }

    /*public boolean authenticate(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();

            if (user.getLockTime() > System.currentTimeMillis()) {
                return false;
            }

            boolean isPasswordValid = passwordEncoder.matches(password, user.getPassword());

            if (!isPasswordValid) {
                user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
                if (user.getFailedLoginAttempts() >= 3) {
                    user.setLockTime(System.currentTimeMillis() + 60000);
                }

                userRepository.save(user);
            } else {
                user.setFailedLoginAttempts(0);
                user.setLockTime(0);
                userRepository.save(user);
            }

            return isPasswordValid;
        }
        return false;
    }*/

    public void enableTwoFactorAuthentication(User user) {
        user.setTwoFactorEnabled(true);
        userRepository.save(user);
    }

    public void disableTwoFactorAuthentication(User user) {
        user.setTwoFactorEnabled(false);
        user.setTwoFactorCode(null);
        userRepository.save(user);
    }

    public String generateTwoFactorCode() {
        return RandomStringUtils.randomNumeric(6);
    }

    public String generatePasswordRecoveryToken(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        String token = UUID.randomUUID().toString();
        user.setRecoveryToken(token);
        user.setTokenExpiryTime(LocalDateTime.now().plusHours(1));
        userRepository.save(user);
        return token;
    }

    public void resetPasswordWithToken(String token, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("Passwords do not match.");
        }

        User user = userRepository.findByRecoveryToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid token."));

        if (user.getTokenExpiryTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Token has expired.");
        }

        if (!newPassword.matches("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!\"#$%&'()*+,-./:;<=>?@\\[\\]^_`{|}~]).{8,}$")) {
            throw new IllegalArgumentException("Password must meet the security policy (Minimum 8 characters, including a capital letter, a number and a symbol (!, @, #))");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setRecoveryToken(null);
        user.setTokenExpiryTime(null);
        userRepository.save(user);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (!userOpt.isPresent()) {
            throw new UsernameNotFoundException("User not found");
        }

        User user = userOpt.get();
        org.springframework.security.core.userdetails.User.UserBuilder builder = org.springframework.security.core.userdetails.User.withUsername(user.getUsername());
        builder.password(user.getPassword());
        builder.roles("USER");
        return builder.build();
    }

    public Optional<User> findByActivationToken(String token) {
        return userRepository.findByActivationtoken(token);
    }

    public void save(User user) {
        userRepository.save(user);
    }

    public User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }

    public User getCurrentUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }

    public boolean isUsernameTaken(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean isEmailTaken(String email) {
        return userRepository.existsByEmail(email);
    }
}
