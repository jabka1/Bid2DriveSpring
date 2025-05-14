package team.bid2drivespring.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import team.bid2drivespring.model.Review;
import team.bid2drivespring.model.User;
import team.bid2drivespring.repository.ReviewRepository;
import team.bid2drivespring.repository.UserRepository;
import team.bid2drivespring.service.UserService;

import java.util.List;

@Controller
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    @GetMapping("/profile/{id}")
    public String viewUserProfile(@PathVariable Long id, Model model) {
        User currentUser = userService.getCurrentUser();

        if(!currentUser.isVerified()) {
            model.addAttribute("message", "Check your profile settings and verify your account to create lots for auction.");
            return "error";
        }
        if(!currentUser.isActivated()) {
            model.addAttribute("message", "Check your email and activate your account.");
            return "error";
        }
        if(currentUser.isBlocked()) {
            model.addAttribute("message", "Your account was blocked.");
            return "error";
        }

        if (currentUser.getId().equals(id)) {
            return "redirect:/profileSettings";
        }

        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (targetUser.getRole() == User.Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access to admin profiles is restricted");
        }

        List<Review> userReviews = reviewRepository.findByTargetAndType(targetUser, Review.ReviewType.USER);
        double averageRating = userReviews.isEmpty()
                ? 0.0
                : userReviews.stream().mapToInt(Review::getRating).average().orElse(0.0);

        model.addAttribute("user", targetUser);
        model.addAttribute("reviews", userReviews);
        model.addAttribute("averageRating", averageRating);
        return "users/profile";
    }
}

