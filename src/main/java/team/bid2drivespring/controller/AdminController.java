package team.bid2drivespring.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
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
}
