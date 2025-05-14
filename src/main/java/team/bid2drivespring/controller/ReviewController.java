package team.bid2drivespring.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import team.bid2drivespring.model.Review;
import team.bid2drivespring.model.User;
import team.bid2drivespring.repository.AuctionRepository;
import team.bid2drivespring.repository.ReviewRepository;
import team.bid2drivespring.repository.UserRepository;
import team.bid2drivespring.service.UserService;

@Controller
@RequestMapping("/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final AuctionRepository auctionRepository;
    private final UserService userService;

    @PostMapping("/profile/{id}")
    public String addUserReview(@PathVariable("id") Long targetUserId,
                                @RequestParam("comment") String comment,
                                @RequestParam("rating") int rating,
                                Model model) {

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

        if (comment == null || comment.trim().isEmpty() || rating < 1 || rating > 5) {
            model.addAttribute("error", "Invalid comment or rating");
            return "error";
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        boolean alreadyExists = reviewRepository
                .findByTargetAndType(targetUser, Review.ReviewType.USER)
                .stream()
                .anyMatch(r -> r.getAuthor().getId().equals(currentUser.getId()));

        if (alreadyExists) {
            model.addAttribute("error", "You have already reviewed this user");
            return "error";
        }

        Review review = new Review();
        review.setAuthor(currentUser);
        review.setTarget(targetUser);
        review.setAuction(null);
        review.setComment(comment.trim());
        review.setRating(rating);
        review.setType(Review.ReviewType.USER);

        reviewRepository.save(review);

        return "redirect:/users/profile/" + targetUserId;
    }

}
