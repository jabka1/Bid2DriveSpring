package team.bid2drivespring.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.multipart.MultipartFile;
import team.bid2drivespring.model.User;
import team.bid2drivespring.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
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

    @Autowired
    private AmazonS3 s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;


    /*public void registerUser(String username, String password, String email) {
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
    }*/

    public void registerUser(String username, String password, String email, String firstName,
                             String lastName, LocalDate dateOfBirth, String countryOfResidence, String city) {
        String encodedPassword = passwordEncoder.encode(password);
        String activationToken = java.util.UUID.randomUUID().toString();

        User user = new User();
        user.setUsername(username);
        user.setPassword(encodedPassword);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setDateOfBirth(dateOfBirth);
        user.setCountry(countryOfResidence);
        user.setCity(city);
        user.setActivationtoken(activationToken);
        user.setActivated(false);

        userRepository.save(user);

        emailService.sendActivationEmail(user.getEmail(), activationToken);
    }


    /*public void updateUserProfile(String username, String newPassword, String confirmPassword) {
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
    }*/

    public void updateUserProfile(String username, String newPassword, String confirmPassword, String firstName,
                                  String lastName, String dateOfBirthStr, String countryOfResidence, String city) {
        User currentUser = getCurrentUser();

        if (username != null && !username.equals(currentUser.getUsername())) {
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

        if (firstName != null && !firstName.isEmpty()) {
            currentUser.setFirstName(firstName);
        }
        if (lastName != null && !lastName.isEmpty()) {
            currentUser.setLastName(lastName);
        }
        if (dateOfBirthStr != null && !dateOfBirthStr.isEmpty()) {
            try {
                LocalDate dateOfBirth = LocalDate.parse(dateOfBirthStr);
                currentUser.setDateOfBirth(dateOfBirth);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid date format. Please use the correct format (YYYY-MM-DD).");
            }
        }
        if (countryOfResidence != null && !countryOfResidence.isEmpty()) {
            currentUser.setCountry(countryOfResidence);
        }
        if (city != null && !city.isEmpty()) {
            currentUser.setCity(city);
        }

        userRepository.save(currentUser);
    }


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

    public String uploadVerificationPhoto(MultipartFile file) throws IOException {
        User user = getCurrentUser();
        String key = "user_verify/" + UUID.randomUUID() + "-" + file.getOriginalFilename();

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());

        s3Client.putObject(bucketName, key, file.getInputStream(), metadata);
        String photoUrl = s3Client.getUrl(bucketName, key).toString();

        user.setVerificationPhotoUrl(photoUrl);
        user.setVerified(false);
        user.setVerificationStatus(User.VerificationStatus.PENDING);
        user.setVerificationComment(null);

        userRepository.save(user);

        return photoUrl;
    }

    public String uploadProfilePhoto(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("File is empty");
        }
        String key = "user_profile_photo/" + UUID.randomUUID().toString() + "-" + file.getOriginalFilename();

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());

        s3Client.putObject(bucketName, key, file.getInputStream(), metadata);
        String photoUrl = s3Client.getUrl(bucketName, key).toString();
        User user = getCurrentUser();
        user.setProfilePhotoUrl(photoUrl);
        userRepository.save(user);
        return photoUrl;
    }

    public void registerAdmin(String username, String password, String email,
                                     String firstName, String lastName, LocalDate dateOfBirth,
                                     String country, String city, User.Role role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setDateOfBirth(dateOfBirth);
        user.setCountry(country);
        user.setCity(city);

        user.setRole(role);
        user.setActivated(true);
        user.setVerified(true);
        user.setTwoFactorEnabled(true);
        user.setVerificationStatus(User.VerificationStatus.APPROVED);
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

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User with id " + id + " not found"));
    }

}
