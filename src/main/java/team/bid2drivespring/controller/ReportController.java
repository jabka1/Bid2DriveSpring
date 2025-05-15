package team.bid2drivespring.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import team.bid2drivespring.model.Auction;
import team.bid2drivespring.model.User;
import team.bid2drivespring.repository.AuctionRepository;
import team.bid2drivespring.service.ReportService;
import team.bid2drivespring.service.UserService;

import java.io.IOException;

@Controller
@RequestMapping("/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final UserService userService;
    private final AuctionRepository auctionRepository;

    @GetMapping("/auction/{id}")
    public String reportAuctionForm(@PathVariable Long id, Model model) {

        User currentUser = userService.getCurrentUser();

        if (!currentUser.isVerified()) {
            model.addAttribute("message", "Check your profile settings and verify your account to create lots for auction.");
            return "error";
        }
        if (!currentUser.isActivated()) {
            model.addAttribute("message", "Check your email and activate your account.");
            return "error";
        }
        if (currentUser.isBlocked()) {
            model.addAttribute("message", "Your account was blocked.");
            return "error";
        }

        Auction auction = auctionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Auction not found"));

        if (auction.getStatus() != Auction.AuctionStatus.ACTIVE) {
            model.addAttribute("message", "You can only report active auctions.");
            return "error";
        }

        model.addAttribute("auctionId", id);
        return "report/reportAuctionForm";
    }

    @PostMapping("/auction/{id}")
    public String submitAuctionReport(@PathVariable Long id,
                                      @RequestParam("description") String description,
                                      @RequestParam(value = "image") MultipartFile image, Model model) throws IOException {

        User currentUser = userService.getCurrentUser();

        if (!currentUser.isVerified()) {
            model.addAttribute("message", "Check your profile settings and verify your account to create lots for auction.");
            return "error";
        }
        if (!currentUser.isActivated()) {
            model.addAttribute("message", "Check your email and activate your account.");
            return "error";
        }
        if (currentUser.isBlocked()) {
            model.addAttribute("message", "Your account was blocked.");
            return "error";
        }

        reportService.submitAuctionReport(id, description, image, currentUser);
        return "redirect:/auctions/view/" + id;
    }

    @GetMapping("/user/{id}")
    public String reportUserForm(@PathVariable Long id, Model model) {
        User currentUser = userService.getCurrentUser();

        if (!currentUser.isVerified()) {
            model.addAttribute("message", "Check your profile settings and verify your account to create lots for auction.");
            return "error";
        }
        if (!currentUser.isActivated()) {
            model.addAttribute("message", "Check your email and activate your account.");
            return "error";
        }
        if (currentUser.isBlocked()) {
            model.addAttribute("message", "Your account was blocked.");
            return "error";
        }

        model.addAttribute("userId", id);
        return "report/reportUserForm";
    }

    @PostMapping("/user/{id}")
    public String submitUserReport(@PathVariable Long id,
                                   @RequestParam("description") String description,
                                   @RequestParam(value = "image") MultipartFile image, Model model) throws IOException {
        User currentUser = userService.getCurrentUser();

        if (!currentUser.isVerified()) {
            model.addAttribute("message", "Check your profile settings and verify your account to create lots for auction.");
            return "error";
        }
        if (!currentUser.isActivated()) {
            model.addAttribute("message", "Check your email and activate your account.");
            return "error";
        }
        if (currentUser.isBlocked()) {
            model.addAttribute("message", "Your account was blocked.");
            return "error";
        }

        reportService.submitUserReport(id, description, image, currentUser);
        return "redirect:/users/profile/" + id;
    }
}
