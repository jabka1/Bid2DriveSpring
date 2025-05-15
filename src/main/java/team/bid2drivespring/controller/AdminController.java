package team.bid2drivespring.controller;

import org.antlr.v4.runtime.TokenStreamRewriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import team.bid2drivespring.model.*;
import team.bid2drivespring.repository.AuctionRepository;
import team.bid2drivespring.repository.ReportRepository;
import team.bid2drivespring.repository.ReviewRepository;
import team.bid2drivespring.service.AdminService;
import team.bid2drivespring.service.AuctionService;
import team.bid2drivespring.service.EmailService;
import team.bid2drivespring.service.UserService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/administrator")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private AuctionService auctionService;

    @Autowired
    private UserService userService;

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



    @GetMapping("/reports/users")
    public String getUserReports(Model model, @RequestParam(defaultValue = "0") int page) {
        Pageable pageable = PageRequest.of(page, 10);
        Page<Report> userReports = adminService.getPendingReportsByType(Report.ReportType.USER, pageable);

        model.addAttribute("reports", userReports);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", userReports.getTotalPages());

        return "admin/userReportsList";
    }

    @GetMapping("/reports/auctions")
    public String getAuctionReports(Model model, @RequestParam(defaultValue = "0") int page) {
        Pageable pageable = PageRequest.of(page, 10);
        Page<Report> auctionReports = adminService.getPendingReportsByType(Report.ReportType.AUCTION, pageable);

        model.addAttribute("reports", auctionReports);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", auctionReports.getTotalPages());

        return "admin/auctionReportsList";
    }

    @GetMapping("/reports/users/{id}")
    public String viewUserReport(@PathVariable Long id, Model model) {
        Report report = adminService.getReportById(id);
        if (report.getType() != Report.ReportType.USER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This is not a user report.");
        }
        if (report.getStatus() != Report.ReportStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This report is already resolved.");
        }
        model.addAttribute("reviews", reviewRepository.findByTargetAndType(report.getReportedUser(), Review.ReviewType.USER));
        model.addAttribute("report", report);
        return "admin/userReportDetails";
    }

    @GetMapping("/reports/auctions/{id}")
    public String viewAuctionReport(@PathVariable Long id, Model model) {
        Report report = adminService.getReportById(id);
        if (report.getType() != Report.ReportType.AUCTION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This is not an auction report.");
        }
        if (report.getStatus() != Report.ReportStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This report is already resolved.");
        }
        Auction auction = report.getReportedAuction();
        if (auction == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Auction not found in report.");
        }

        List<Review> reviews = reviewRepository.findByAuctionAndType(auction, Review.ReviewType.AUCTION);

        model.addAttribute("report", report);
        model.addAttribute("auction", auction);
        model.addAttribute("reviews", reviews);

        return "admin/auctionReportDetails";
    }

    @PostMapping("/reports/auctions/{id}/action")
    @Transactional
    public String handleAuctionReportAction(@PathVariable Long id,
                                            @RequestParam("action") String action,
                                            @RequestParam("adminResponse") String adminResponse) {
        Report report = adminService.getReportById(id);

        if (report.getType() != Report.ReportType.AUCTION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This is not an auction report.");
        }
        if (report.getStatus() != Report.ReportStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only pending reports can be processed.");
        }
        if (report.getReportedAuction().getStatus() != Auction.AuctionStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only active auctions can be processed.");
        }

        Auction auction = report.getReportedAuction();

        switch (action) {
            case "WARNING" -> {
                emailService.sendAdminDecisionEmail(
                        auction.getSeller().getEmail(),
                        "Warning regarding auction: " + auction.getTitle(),
                        auction.getCarModel(), auction.getCarMake(), auction.getVin(),
                        adminResponse
                );
                report.setAdminResponse(adminResponse);
                report.setStatus(Report.ReportStatus.APPROVED);
                reportRepository.save(report);
            }
            case "BLOCK" -> {
                emailService.sendAdminDecisionEmail(
                        auction.getSeller().getEmail(),
                        "Auction Blocked: " + auction.getTitle(),
                        auction.getCarModel(), auction.getCarMake(), auction.getVin(),
                        adminResponse
                );
                auctionService.deleteAuction(auction);
            }
            case "REJECT" -> {
                report.setAdminResponse(adminResponse);
                report.setStatus(Report.ReportStatus.REJECTED);
                reportRepository.save(report);
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown action: " + action);
        }
        return "redirect:/administrator/reports/auctions";
    }

    @PostMapping("/reports/users/{id}/action")
    @Transactional
    public String handleUserReportAction(@PathVariable Long id,
                                         @RequestParam("action") String action,
                                         @RequestParam("adminResponse") String adminResponse) {
        Report report = adminService.getReportById(id);

        if (report.getType() != Report.ReportType.USER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This is not a user report.");
        }
        if (report.getStatus() != Report.ReportStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only pending reports can be processed.");
        }

        User reportedUser = report.getReportedUser();

        switch (action) {
            case "WARNING" -> {
                emailService.sendAdminDecisionEmailForUser(
                        reportedUser.getEmail(),
                        "Warning regarding user: ",
                        reportedUser.getFirstName(),
                        reportedUser.getLastName(),
                        adminResponse
                );
                report.setAdminResponse(adminResponse);
                report.setStatus(Report.ReportStatus.APPROVED);
                reportRepository.save(report);
            }
            case "BLOCK" -> {
                blockUser(reportedUser, adminResponse);
                report.setAdminResponse(adminResponse);
                report.setStatus(Report.ReportStatus.APPROVED);
                reportRepository.save(report);
            }
            case "REJECT" -> {
                report.setAdminResponse(adminResponse);
                report.setStatus(Report.ReportStatus.REJECTED);
                reportRepository.save(report);
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown action: " + action);
        }

        return "redirect:/administrator/reports/users";
    }

    @Transactional
    public void blockUser(User user, String adminResponse) {
        user.setBlocked(true);
        userService.save(user);

        List<Auction> auctionsToDelete = auctionRepository
                .findBySellerAndStatusIn(user, List.of(Auction.AuctionStatus.ACTIVE, Auction.AuctionStatus.NOT_SOLD));

        for (Auction auction : auctionsToDelete) {
            auctionService.deleteAuction(auction);
        }

        List<Auction> allAuctions = auctionRepository.findAll();
        for (Auction auction : allAuctions) {
            List<Bid> updatedBids = auction.getBids().stream()
                    .filter(bid -> !bid.getUserId().equals(user.getId()))
                    .collect(Collectors.toList());

            if (updatedBids.size() != auction.getBids().size()) {
                auction.setBids(updatedBids);
                auctionRepository.save(auction);
            }
        }

        emailService.sendAdminDecisionEmailForUser(user.getEmail(), "User Blocked: ", user.getFirstName(), user.getLastName(), adminResponse);
    }
}
