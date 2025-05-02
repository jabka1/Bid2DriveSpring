package team.bid2drivespring.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import team.bid2drivespring.model.Auction;
import team.bid2drivespring.model.User;
import team.bid2drivespring.repository.AuctionRepository;
import team.bid2drivespring.repository.UserRepository;


@Service
public class AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private AuctionRepository auctionRepository;

    public Page<User> getUsersPendingVerification(Pageable pageable) {
        return userRepository.findByVerificationPhotoUrlIsNotNullAndVerificationStatus(User.VerificationStatus.PENDING, pageable);
    }

    public Page<Auction> getCarsPendingVerification(Pageable pageable) {
        return auctionRepository.findByVerificationStatus(Auction.AuctionVerificationStatus.PENDING, pageable);
    }

    public Auction getAuctionById(Long id) {
        return auctionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Auction not found with id = " + id));
    }

    public void approveVerification(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getVerificationStatus() == User.VerificationStatus.APPROVED ||
                user.getVerificationStatus() == User.VerificationStatus.REJECTED) {
            throw new IllegalStateException("User verification has already been processed");
        }

        user.setVerified(true);
        user.setVerificationStatus(User.VerificationStatus.APPROVED);
        user.setVerificationComment(null);
        emailService.sendVerificationApprovedEmail(user.getEmail());

        userRepository.save(user);
    }

    public void rejectVerification(Long userId, String comment) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getVerificationStatus() == User.VerificationStatus.APPROVED ||
                user.getVerificationStatus() == User.VerificationStatus.REJECTED) {
            throw new IllegalStateException("User verification has already been processed");
        }

        user.setVerified(false);
        user.setVerificationStatus(User.VerificationStatus.REJECTED);
        user.setVerificationComment(comment);
        emailService.sendVerificationRejectedEmail(user.getEmail());

        userRepository.save(user);
    }

    public void approveAuction(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("Auction not found"));

        if (auction.getVerificationStatus() == Auction.AuctionVerificationStatus.APPROVED ||
                auction.getVerificationStatus() == Auction.AuctionVerificationStatus.REJECTED) {
            throw new IllegalStateException("Auction has already been verified");
        }

        auction.setVerificationStatus(Auction.AuctionVerificationStatus.APPROVED);
        auction.setVerificationComment(null);
        emailService.sendCarApprovedEmail(auction.getSeller().getEmail(), auction.getCarMake(), auction.getCarModel(), auction.getVin());
        auctionRepository.save(auction);
    }

    public void rejectAuction(Long auctionId, String comment) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("Auction not found"));

        if (auction.getVerificationStatus() == Auction.AuctionVerificationStatus.APPROVED ||
                auction.getVerificationStatus() == Auction.AuctionVerificationStatus.REJECTED) {
            throw new IllegalStateException("Auction has already been verified");
        }

        auction.setVerificationStatus(Auction.AuctionVerificationStatus.REJECTED);
        auction.setVerificationComment(comment);
        emailService.sendCarRejectedEmail(auction.getSeller().getEmail(), auction.getCarMake(), auction.getCarModel(), auction.getVin(), auction.getVerificationComment());
        auctionRepository.save(auction);
    }
}
