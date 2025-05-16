package team.bid2drivespring.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import team.bid2drivespring.model.Auction;
import team.bid2drivespring.model.User;
import team.bid2drivespring.repository.AuctionRepository;
import team.bid2drivespring.service.PdfGenerationService;
import team.bid2drivespring.util.UrlSafeEncryptionUtil;

@Controller
@RequestMapping("/verifyDelivery")
@RequiredArgsConstructor
public class AuctionDeliveryVerificationController {

    private final AuctionRepository auctionRepository;
    private final UrlSafeEncryptionUtil urlSafeEncryptionService;
    private final PdfGenerationService pdfGenerationService;

    @GetMapping("/{token}")
    public String viewDeliveryVerificationPage(@PathVariable String token, Model model) {
        long id;
        try {
            id = urlSafeEncryptionService.decodeToken(token);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid delivery token");
        }

        Auction auction = auctionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Auction not found"));

        Auction.AuctionStatus status = auction.getStatus();
        if (status != Auction.AuctionStatus.WAITING_FOR_SHIPMENT &&
                status != Auction.AuctionStatus.HANDED_OVER_TO_DELIVERY &&
                status != Auction.AuctionStatus.RECEIVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Auction is not in a valid shipping status");
        }

        User seller = auction.getSeller();
        User buyer = auction.getNewOwner();

        model.addAttribute("auction", auction);
        model.addAttribute("seller", seller);
        model.addAttribute("buyer", buyer);

        return "auctions/shippingDetails";
    }
}
