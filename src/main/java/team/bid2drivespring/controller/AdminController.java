package team.bid2drivespring.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import team.bid2drivespring.model.User;
import team.bid2drivespring.service.AdminService;
import team.bid2drivespring.service.UserService;

import java.util.List;

@Controller
@RequestMapping("/administrator")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @GetMapping("/users/pendingVerification")
    public String getUsersPendingVerification(Model model) {
        List<User> users = adminService.getUsersPendingVerification();
        model.addAttribute("users", users);
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
