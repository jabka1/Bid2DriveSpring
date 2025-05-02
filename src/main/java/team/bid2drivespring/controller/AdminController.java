package team.bid2drivespring.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import team.bid2drivespring.model.Auction;
import team.bid2drivespring.model.User;
import team.bid2drivespring.service.AdminService;

@Controller
@RequestMapping("/administrator")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @GetMapping("/users/pendingVerification")
    public String getUsersPendingVerification(Model model,
                                              @RequestParam(defaultValue = "0") int page) {
        Pageable pageable = PageRequest.of(page, 5);
        Page<User> userPage = adminService.getUsersPendingVerification(pageable);

        model.addAttribute("users", userPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", userPage.getTotalPages());

        return "admin/pendingVerification";
    }


    @PostMapping("/users/{userId}/approve")
    public String approveVerification(@PathVariable Long userId, Model model) {
        try {
            adminService.approveVerification(userId);
            return "redirect:/administrator/users/pendingVerification";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "admin/pendingVerification";
        }
    }

    @PostMapping("/users/{userId}/reject")
    public String rejectVerification(@PathVariable Long userId, @RequestParam String comment, Model model) {
        try {
            adminService.rejectVerification(userId, comment);
            return "redirect:/administrator/users/pendingVerification";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "admin/pendingVerification";
        }
    }

    @GetMapping("/cars/pendingVerification")
    public String getCarsPendingVerification(Model model,
                                             @RequestParam(defaultValue = "0") int page) {
        Pageable pageable = PageRequest.of(page, 5);
        Page<Auction> auctions = adminService.getCarsPendingVerification(pageable);

        model.addAttribute("auctions", auctions);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", auctions.getTotalPages());

        return "admin/pendingCars";
    }

    @GetMapping("/cars/{id}")
    public String getAuctionVerificationPage(@PathVariable Long id, Model model) {
        try {
            Auction auction = adminService.getAuctionById(id);

            if (auction.getVerificationStatus() != Auction.AuctionVerificationStatus.PENDING) {
                model.addAttribute("error", "Auction has already been verified or rejected.");
                return "admin/pendingCars";
            }

            model.addAttribute("auction", auction);
            return "admin/carsVerification";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", "Auction not found.");
            return "admin/pendingCars";
        }
    }

    @PostMapping("/cars/{auctionId}/approve")
    public String approveAuction(@PathVariable Long auctionId, Model model) {
        try {
            adminService.approveAuction(auctionId);
            return "redirect:/administrator/cars/pendingVerification";
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
            return "admin/pendingCars";
        }
    }

    @PostMapping("/cars/{auctionId}/reject")
    public String rejectAuction(@PathVariable Long auctionId, @RequestParam String comment, Model model) {
        try {
            adminService.rejectAuction(auctionId, comment);
            return "redirect:/administrator/cars/pendingVerification";
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
            return "admin/pendingCars";
        }
    }


}
