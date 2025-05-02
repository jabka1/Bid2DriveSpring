package team.bid2drivespring.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import team.bid2drivespring.model.Auction;

import java.util.Optional;

public interface AuctionRepository extends JpaRepository<Auction, Long> {
    Optional<Object> findByVin(String vin);
    Page<Auction> findByVerificationStatus(Auction.AuctionVerificationStatus status, Pageable pageable);
}
