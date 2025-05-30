package team.bid2drivespring.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import team.bid2drivespring.model.Auction;
import team.bid2drivespring.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByActivationtoken(String token);
    Optional<User> findByRecoveryToken(String recoveryToken);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Page<User> findByVerificationPhotoUrlIsNotNullAndVerificationStatus(User.VerificationStatus status, Pageable pageable);
    boolean existsByPassportNumberAndCountryAndIdNot(String passportNumber, String country, Long id);
    List<User> findAllBySavedAuctionsContains(Auction auction);

    @Modifying
    @Query(value = "DELETE FROM saved_auctions WHERE auction_id = :auctionId", nativeQuery = true)
    void removeAuctionFromAllUsers(@Param("auctionId") Long auctionId);

}

