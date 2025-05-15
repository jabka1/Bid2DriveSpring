package team.bid2drivespring.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import team.bid2drivespring.model.Review;
import team.bid2drivespring.model.Auction;
import team.bid2drivespring.model.User;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByTargetAndType(User target, Review.ReviewType type);
    List<Review> findByAuctionAndType(Auction auction, Review.ReviewType type);
    void deleteAllByAuction(Auction auction);
}
