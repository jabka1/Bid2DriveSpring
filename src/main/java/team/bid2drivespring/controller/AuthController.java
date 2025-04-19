package team.bid2drivespring.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import team.bid2drivespring.model.User;
import team.bid2drivespring.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import team.bid2drivespring.service.EmailService;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;

@Controller
public class AuthController {

    @Value("${google.recaptcha.secret.key}")
    private String recaptchaSecretKey;

    @Value("${google.recaptcha.site.key}")
    private String recaptchaSiteKey;

    @Value("${admin.secret.key}")
    private String adminSecretKey;

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        model.addAttribute("recaptchaSiteKey", recaptchaSiteKey);
        return "register";
    }

    /*@PostMapping("/register")
    public String register(@RequestParam String username, @RequestParam String password,
                           @RequestParam String email, @RequestParam("g-recaptcha-response") String gRecaptchaResponse,
                           Model model) {
        model.addAttribute("recaptchaSiteKey", recaptchaSiteKey);
        if (!password.matches("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!\"#$%&'()*+,-./:;<=>?@\\[\\]^_`{|}~]).{8,}$")) {
            model.addAttribute("error", "Password must meet the security policy (Minimum 8 characters, including a capital letter, a number and a symbol (!, @, #))");
            return "register";
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            model.addAttribute("error", "Invalid email format!");
            return "register";
        }
        if (userService.isUsernameTaken(username)) {
            model.addAttribute("error", "Username is already taken!");
            return "register";
        }
        if (userService.isEmailTaken(email)) {
            model.addAttribute("error", "Email is already registered!");
            return "register";
        }
        if (!verifyRecaptcha(gRecaptchaResponse)) {
            model.addAttribute("error", "reCAPTCHA verification failed!");
            return "register";
        }
        userService.registerUser(username, password, email);
        return "redirect:/login";
    }*/

    @PostMapping("/register")
    public String register(@RequestParam String username, @RequestParam String password,
                           @RequestParam String email, @RequestParam String firstName,
                           @RequestParam String lastName, @RequestParam String dateOfBirthStr,
                           @RequestParam String countryOfResidence, @RequestParam String city,
                           @RequestParam("g-recaptcha-response") String gRecaptchaResponse,
                           Model model) {
        model.addAttribute("recaptchaSiteKey", recaptchaSiteKey);

        if (!password.matches("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!\"#$%&'()*+,-./:;<=>?@\\[\\]^_`{|}~]).{8,}$")) {
            model.addAttribute("error", "Password must meet the security policy (Minimum 8 characters, including a capital letter, a number and a symbol (!, @, #))");
            return "register";
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            model.addAttribute("error", "Invalid email format!");
            return "register";
        }

        if (userService.isUsernameTaken(username)) {
            model.addAttribute("error", "Username is already taken!");
            return "register";
        }

        if (userService.isEmailTaken(email)) {
            model.addAttribute("error", "Email is already registered!");
            return "register";
        }

        if (!verifyRecaptcha(gRecaptchaResponse)) {
            model.addAttribute("error", "reCAPTCHA verification failed!");
            return "register";
        }

        LocalDate dateOfBirth = null;
        try {
            dateOfBirth = LocalDate.parse(dateOfBirthStr);
        } catch (Exception e) {
            model.addAttribute("error", "Invalid date format. Please use the correct format (YYYY-MM-DD).");
            return "register";
        }

        userService.registerUser(username, password, email, firstName, lastName, dateOfBirth, countryOfResidence, city);

        return "redirect:/login";
    }


    @GetMapping("/activate")
    public String activateAccount(@RequestParam String token, Model model) {
        Optional<User> userOpt = userService.findByActivationToken(token);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setActivated(true);
            user.setActivationtoken(null);
            userService.save(user);
            model.addAttribute("message", "Your account has been activated successfully!");
            return "activation_status";
        } else {
            model.addAttribute("message", "Invalid activation token!");
            return "activation_status";
        }
    }

    @GetMapping("/login")
    public String showLoginPage() { return "login"; }

    @GetMapping("/profileSettings")
    public String showProfileSettingsPage(Model model) {
        User user = userService.getCurrentUser();
        model.addAttribute("user", user);
        return "profileSettings";
    }

    @GetMapping("/2fa")
    public String show2faPage() {
        return "2fa";
    }

    @PostMapping("/2fa")
    public String verify2faCode(@RequestParam String code, Model model) {
        User user = userService.getCurrentUser();
        if (user.getTwoFactorCode().equals(code)) {
            user.setTwoFactorCode(null);
            userService.save(user);
            return "redirect:/profileSettings";
        } else {
            model.addAttribute("error", "Invalid 2FA code");
            return "2fa";
        }
    }

    @PostMapping("/2fa/enable")
    public String enableTwoFactor() {
        User user = userService.getCurrentUser();
        userService.enableTwoFactorAuthentication(user);
        return "redirect:/profileSettings";
    }

    @PostMapping("/2fa/disable")
    public String disableTwoFactor() {
        User user = userService.getCurrentUser();
        userService.disableTwoFactorAuthentication(user);
        return "redirect:/profileSettings";
    }

    @GetMapping("/changeProfileInfo")
    public String showChangeProfilePage(Model model) {
        User currentUser = userService.getCurrentUser();
        model.addAttribute("user", currentUser);
        return "changeProfileInfo";
    }

    /*@PostMapping("/changeProfileInfo")
    public String changeProfileInfo(@RequestParam String username,
                                    @RequestParam String password,
                                    @RequestParam String confirmPassword,
                                    Model model) {
        try {
            userService.updateUserProfile(username, password, confirmPassword);
            return "redirect:/profileSettings";
        } catch (IllegalArgumentException e) {
            User currentUser = userService.getCurrentUser();
            model.addAttribute("user", currentUser);
            model.addAttribute("error", e.getMessage());
            return "changeProfileInfo";
        }
    }*/

    @PostMapping("/changeProfileInfo")
    public String changeProfileInfo(@RequestParam(required = false) String username,
                                    @RequestParam(required = false) String password,
                                    @RequestParam(required = false) String confirmPassword,
                                    @RequestParam(required = false) String firstName,
                                    @RequestParam(required = false) String lastName,
                                    @RequestParam(required = false) String dateOfBirthStr,
                                    @RequestParam(required = false) String countryOfResidence,
                                    @RequestParam(required = false) String city,
                                    Model model) {
        try {
            userService.updateUserProfile(username, password, confirmPassword, firstName, lastName, dateOfBirthStr, countryOfResidence, city);
            return "redirect:/profileSettings";
        } catch (IllegalArgumentException e) {
            User currentUser = userService.getCurrentUser();
            model.addAttribute("user", currentUser);
            model.addAttribute("error", e.getMessage());
            return "changeProfileInfo";
        }
    }

    @GetMapping("/generateTokenForPasswordRecovery")
    public String showForgotPasswordPage() {
        return "generateTokenForPasswordRecovery";
    }

    @PostMapping("/generateTokenForPasswordRecovery")
    public String generateRecoveryToken(@RequestParam String username, Model model) {
        try {
            String token = userService.generatePasswordRecoveryToken(username);
            emailService.sendPasswordRecoveryEmail(username, token);
            return "passwordRecovery";
        } catch (UsernameNotFoundException e) {
            model.addAttribute("error", "Username not found!");
            return "generateTokenForPasswordRecovery";
        } catch (Exception e) {
            model.addAttribute("error", "An error occurred while sending the recovery email.");
            return "generateTokenForPasswordRecovery";
        }
    }

    @PostMapping("/passwordRecovery")
    public String resetPassword(@RequestParam String token,
                                @RequestParam String password,
                                @RequestParam String confirmPassword,
                                Model model) {
        try {
            userService.resetPasswordWithToken(token, password, confirmPassword);
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "passwordRecovery";
        }
    }

    @GetMapping("/uploadPassportPhoto")
    public String showUploadPassportPhotoPage(Model model, RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser();
        if (user.getVerificationStatus() != User.VerificationStatus.NOT_SUBMITTED &&
                user.getVerificationStatus() != User.VerificationStatus.REJECTED) {
            redirectAttributes.addFlashAttribute("error", "You cannot access this page at this time.");
            return "redirect:/profileSettings";
        }

        model.addAttribute("user", user);
        return "uploadPassportPhoto";
    }

    @PostMapping("/uploadPassportPhoto")
    public String uploadPassportPhoto(
            @RequestParam("file") MultipartFile file,
            @RequestParam("passportNumber") String passportNumber,
            Model model
    ) {
        try {
            String photoUrl = userService.uploadVerificationPhoto(file, passportNumber);

            model.addAttribute("success", "Passport photo and number uploaded successfully!");
            model.addAttribute("user", userService.getCurrentUser());
        } catch (IOException e) {
            model.addAttribute("error", "Failed to upload passport photo.");
        }

        return "profileSettings";
    }

    @PostMapping("/uploadProfilePhoto")
    public String uploadProfilePhoto(@RequestParam("file") MultipartFile file, Model model) {
        try {
            String photoUrl = userService.uploadProfilePhoto(file);
            model.addAttribute("success", "Profile photo uploaded successfully!");
        } catch (IOException e) {
            model.addAttribute("error", "Failed to upload profile photo.");
        }
        User user = userService.getCurrentUser();
        model.addAttribute("user", user);
        return "profileSettings";
    }

    @GetMapping("/createAdmin")
    public String showCreateAdminPage() {
        return "admin/createAdmin";
    }

    @PostMapping("/createAdmin")
    public String createAdmin(
                                @RequestParam String token,
                                @RequestParam String username,
                              @RequestParam String password,
                              @RequestParam String email,
                              @RequestParam String firstName,
                              @RequestParam String lastName,
                              @RequestParam String dateOfBirthStr,
                              @RequestParam String country,
                              @RequestParam String city,
                              Model model) {
        if (!token.equals(adminSecretKey)) {
            model.addAttribute("error", "Invalid admin creation token!");
            return "admin/createAdmin";
        }

        if (userService.isUsernameTaken(username)) {
            model.addAttribute("error", "Username is already taken!");
            return "admin/createAdmin";
        }

        if (userService.isEmailTaken(email)) {
            model.addAttribute("error", "Email is already registered!");
            return "admin/createAdmin";
        }

        LocalDate dateOfBirth;
        try {
            dateOfBirth = LocalDate.parse(dateOfBirthStr);
        } catch (Exception e) {
            model.addAttribute("error", "Invalid date format. Use YYYY-MM-DD.");
            return "admin/createAdmin";
        }

        userService.registerAdmin(
                username, password, email, firstName, lastName,
                dateOfBirth, country, city, User.Role.ADMIN
        );

        model.addAttribute("success", "Admin created successfully!");
        return "admin/createAdmin";
    }


    private boolean verifyRecaptcha(String gRecaptchaResponse) {
        String url = "https://www.google.com/recaptcha/api/siteverify";
        String params = "secret=" + recaptchaSecretKey + "&response=" + gRecaptchaResponse;

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(url + "?" + params, HttpMethod.POST, entity, String.class);

        return response.getBody().contains("\"success\": true");
    }
}
