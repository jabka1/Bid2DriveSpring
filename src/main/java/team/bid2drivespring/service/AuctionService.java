package team.bid2drivespring.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import team.bid2drivespring.model.Auction;
import team.bid2drivespring.model.Bid;
import team.bid2drivespring.model.User;
import team.bid2drivespring.repository.AuctionRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import team.bid2drivespring.repository.ReportRepository;
import team.bid2drivespring.repository.ReviewRepository;
import team.bid2drivespring.repository.UserRepository;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuctionService {

    private final AmazonS3 s3Client;
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final ReportRepository reportRepository;
    private final ReviewRepository reviewRepository;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    private static final String AUCTION_PHOTO_FOLDER = "auction_photo/";



    private List<String> uploadFilesToS3(MultipartFile[] files) throws IOException {
        List<String> uploadedUrls = new ArrayList<>();

        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                String key = AUCTION_PHOTO_FOLDER + UUID.randomUUID() + "-" + file.getOriginalFilename();

                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentLength(file.getSize());
                metadata.setContentType(file.getContentType());

                s3Client.putObject(bucketName, key, file.getInputStream(), metadata);

                String fileUrl = s3Client.getUrl(bucketName, key).toString();
                uploadedUrls.add(fileUrl);
            }
        }

        return uploadedUrls;
    }

    public void deleteAuction(Auction auction) {
        for (String imageUrl : auction.getCarImagesUrls()) {
            String key = extractS3Key(imageUrl);
            s3Client.deleteObject(bucketName, key);
        }
        reviewRepository.deleteAllByAuction(auction);
        if (reportRepository.existsByReportedAuction(auction)) {
            reportRepository.deleteAllByReportedAuction(auction);
        }

        List<User> usersWithSavedAuction = userRepository.findAllBySavedAuctionsContains(auction);
        for (User user : usersWithSavedAuction) {
            user.getSavedAuctions().remove(auction);
        }
        userRepository.saveAll(usersWithSavedAuction);

        auctionRepository.delete(auction);
    }

    private String extractS3Key(String url) {
        return url.substring(url.indexOf("auction_photo/"));
    }

    public Page<Auction> findFilteredAuctions(
            Auction.AuctionType auctionType,
            Optional<String> carMake,
            Optional<String> carModel,
            Optional<Integer> yearFrom,
            Optional<Integer> yearTo,
            Optional<Integer> mileageFrom,
            Optional<Integer> mileageTo,
            Optional<Integer> horsepowerFrom,
            Optional<Integer> horsepowerTo,
            Optional<Integer> priceFrom,
            Optional<Integer> priceTo,
            Optional<Double> engineSizeFrom,
            Optional<Double> engineSizeTo,
            Optional<Auction.FuelType> fuelType,
            Optional<Auction.TransmissionType> transmission,
            Optional<Auction.BodyType> bodyType,
            Optional<Auction.DriveType> driveType,
            Optional<Auction.TechnicalCondition> technicalCondition,
            Optional<Auction.BodyCondition> bodyCondition,
            Optional<String> country,
            Optional<String> region,
            Optional<Integer> numberOfDoors,
            Pageable pageable
    ) {
        Specification<Auction> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("auctionType"), auctionType));
            predicates.add(cb.equal(root.get("status"), Auction.AuctionStatus.ACTIVE));
            predicates.add(cb.equal(root.get("verificationStatus"), Auction.AuctionVerificationStatus.APPROVED));
            predicates.add(cb.isNull(root.get("newOwner")));

            carMake.ifPresent(value -> predicates.add(cb.equal(root.get("carMake"), value)));
            carModel.ifPresent(value -> predicates.add(cb.equal(root.get("carModel"), value)));
            yearFrom.ifPresent(value -> predicates.add(cb.greaterThanOrEqualTo(root.get("year"), value)));
            yearTo.ifPresent(value -> predicates.add(cb.lessThanOrEqualTo(root.get("year"), value)));
            mileageFrom.ifPresent(value -> predicates.add(cb.greaterThanOrEqualTo(root.get("mileage"), value)));
            mileageTo.ifPresent(value -> predicates.add(cb.lessThanOrEqualTo(root.get("mileage"), value)));
            horsepowerFrom.ifPresent(value -> predicates.add(cb.greaterThanOrEqualTo(root.get("horsepower"), value)));
            horsepowerTo.ifPresent(value -> predicates.add(cb.lessThanOrEqualTo(root.get("horsepower"), value)));
            priceFrom.ifPresent(value -> predicates.add(cb.greaterThanOrEqualTo(root.get("startingPrice"), value)));
            priceTo.ifPresent(value -> predicates.add(cb.lessThanOrEqualTo(root.get("startingPrice"), value)));
            engineSizeFrom.ifPresent(value -> predicates.add(cb.greaterThanOrEqualTo(root.get("engineSize"), value)));
            engineSizeTo.ifPresent(value -> predicates.add(cb.lessThanOrEqualTo(root.get("engineSize"), value)));
            fuelType.ifPresent(value -> predicates.add(cb.equal(root.get("fuelType"), value)));
            transmission.ifPresent(value -> predicates.add(cb.equal(root.get("transmission"), value)));
            bodyType.ifPresent(value -> predicates.add(cb.equal(root.get("bodyType"), value)));
            driveType.ifPresent(value -> predicates.add(cb.equal(root.get("driveType"), value)));
            technicalCondition.ifPresent(value -> predicates.add(cb.equal(root.get("technicalCondition"), value)));
            bodyCondition.ifPresent(value -> predicates.add(cb.equal(root.get("bodyCondition"), value)));
            country.ifPresent(value -> predicates.add(cb.equal(root.get("country"), value)));
            region.ifPresent(value -> predicates.add(cb.equal(root.get("region"), value)));
            numberOfDoors.ifPresent(value -> predicates.add(cb.equal(root.get("numberOfDoors"), value)));

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return auctionRepository.findAll(spec, pageable);
    }

    public List<String> getAvailableMakes() {
        return auctionRepository.findDistinctCarMake();
    }

    public List<String> getModelsByMake(String make) {
        return auctionRepository.findDistinctCarModelByCarMake(make);
    }

    public List<String> getAvailableCountry() {
        return auctionRepository.findDistinctCountry();
    }

    public List<String> getRegionsByCountry(String country) {
        return auctionRepository.findDistinctRegionByCountry(country);
    }

    public Optional<Auction> findPubliclyVisibleAuctionById(Long id) {
        return auctionRepository.findByIdAndStatusAndVerificationStatusAndNewOwnerIsNull(
                id,
                Auction.AuctionStatus.ACTIVE,
                Auction.AuctionVerificationStatus.APPROVED
        );
    }

    public Optional<Auction> findById(Long id) {
        return auctionRepository.findById(id);
    }

    public void placeBid(Auction auction, User user, int proposedPrice) {
        if (auction.getBids() == null) {
            auction.setBids(new ArrayList<>());
        }

        Date now = new Date();

        if (auction.getAuctionType() == Auction.AuctionType.LIVE_BID) {
            if (now.before(auction.getStartTime())) {
                throw new IllegalStateException("This LiveBid auction hasn't started yet.");
            }
        }

        if (auction.getAuctionType() == Auction.AuctionType.STANDARD || auction.getAuctionType() == Auction.AuctionType.LIVE_BID) {
            int minAcceptablePrice = auction.getStartingPrice();
            if (!auction.getBids().isEmpty()) {
                int maxBid = auction.getBids().stream()
                        .mapToInt(Bid::getProposedPrice)
                        .max()
                        .orElse(minAcceptablePrice);
                minAcceptablePrice = Math.max(minAcceptablePrice, maxBid + 1);
            }

            if (proposedPrice < minAcceptablePrice) {
                throw new IllegalArgumentException("Bid must be at least " + minAcceptablePrice + " USD.");
            }
        }

        Bid bid = new Bid(proposedPrice, user.getId(), user.getUsername());
        auction.getBids().add(bid);
        auctionRepository.save(auction);
    }

    @Transactional
    public void assignWinnersToFinishedAuctions() {
        List<Auction> endedAuctions = auctionRepository
                .findAllByNewOwnerIsNullAndStatus(Auction.AuctionStatus.ACTIVE);
        Instant now = Instant.now();

        for (Auction auction : endedAuctions) {
            String auctionType = auction.getAuctionType().name();

            if (!auctionType.equals("STANDARD") && !auctionType.equals("LIVE_BID")) {
                continue;
            }

            Timestamp end = (Timestamp) auction.getEndTime();
            if (end == null) continue;

            Instant endUtc = end.toLocalDateTime().atOffset(ZoneOffset.UTC).toInstant();

            if (now.isAfter(endUtc)) {
                List<Bid> bids = auction.getBids();

                if (bids == null || bids.isEmpty()) {
                    auction.setStatus(Auction.AuctionStatus.NOT_SOLD);
                    auctionRepository.save(auction);
                    continue;
                }

                Optional<Bid> winningBid = bids.stream()
                        .max(Comparator.comparing(Bid::getProposedPrice));

                winningBid.ifPresent(bid -> {
                    Optional<User> userOpt = userRepository.findById(bid.getUserId());
                    userOpt.ifPresent(user -> {
                        auction.setNewOwner(user);
                        auction.setStatus(Auction.AuctionStatus.WAITING_FOR_SHIPMENT);
                        auctionRepository.save(auction);

                        reportRepository.deleteAllByReportedAuction(auction);
                    });
                });
            }
        }
    }

    @Transactional
    public void uploadImgsAndSaveAuction(Auction auction, MultipartFile[] carImages) throws IOException {
        List<String> uploadedUrls = uploadFilesToS3(carImages);
        auction.setCarImagesUrls(uploadedUrls);
        saveAuction(auction);
    }

    @Transactional
    public void saveAuction(Auction auction) {
        auctionRepository.save(auction);
    }
}
