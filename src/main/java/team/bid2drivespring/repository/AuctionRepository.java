package team.bid2drivespring.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import team.bid2drivespring.model.Auction;

import java.util.List;
import java.util.Optional;

public interface AuctionRepository extends JpaRepository<Auction, Long>, JpaSpecificationExecutor<Auction> {
    Optional<Object> findByVin(String vin);
    Page<Auction> findByVerificationStatus(Auction.AuctionVerificationStatus status, Pageable pageable);

    @Query("SELECT DISTINCT a.carMake FROM Auction a")
    List<String> findDistinctCarMake();

    @Query("SELECT DISTINCT a.country FROM Auction a")
    List<String> findDistinctCountry();

    @Query("SELECT DISTINCT a.carModel FROM Auction a WHERE a.carMake = :make")
    List<String> findDistinctCarModelByCarMake(@Param("make") String make);

    @Query("SELECT DISTINCT a.region FROM Auction a WHERE a.country = :country")
    List<String> findDistinctRegionByCountry(@Param("country") String country);

}
