package team.bid2drivespring.controller;

import com.amazonaws.services.s3.AmazonS3;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import team.bid2drivespring.model.Auction;
import team.bid2drivespring.model.Auction.*;
import team.bid2drivespring.model.Review;
import team.bid2drivespring.model.User;
import team.bid2drivespring.repository.AuctionRepository;
import team.bid2drivespring.repository.ReportRepository;
import team.bid2drivespring.repository.ReviewRepository;
import team.bid2drivespring.repository.UserRepository;
import team.bid2drivespring.service.AuctionService;
import team.bid2drivespring.service.EmailService;
import team.bid2drivespring.service.PdfGenerationService;
import team.bid2drivespring.service.UserService;
import team.bid2drivespring.util.UrlSafeEncryptionUtil;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.Principal;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/auctions")
@RequiredArgsConstructor
public class AuctionController {

    @Autowired
    private final AuctionService auctionService;

    @Autowired
    private UserService userService;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private UrlSafeEncryptionUtil urlSafeEncryptionService;

    @Autowired
    private PdfGenerationService pdfGenerationService;

    @Autowired
    private EmailService emailService;

    @Value("${app.domain}")
    private String appDomain;



    @GetMapping("/create")
    public String showCreateForm(Model model) {

        User currentUser = userService.getCurrentUser();

        if(!currentUser.isVerified()) {
            model.addAttribute("message", "Check your profile settings and verify your account to create lots for auction.");
            return "error";
        }
        if(!currentUser.isActivated()) {
            model.addAttribute("message", "Check your email and activate your account.");
            return "error";
        }
        if(currentUser.isBlocked()) {
            model.addAttribute("message", "Your account was blocked.");
            return "error";
        }


        model.addAttribute("auction", new Auction());
        model.addAttribute("fuelTypes", FuelType.values());
        model.addAttribute("transmissions", TransmissionType.values());
        model.addAttribute("bodyTypes", BodyType.values());
        model.addAttribute("driveTypes", DriveType.values());
        model.addAttribute("euroStandards", EuroStandard.values());
        model.addAttribute("epaStandards", EPAStandard.values());
        model.addAttribute("technicalConditions", TechnicalCondition.values());
        model.addAttribute("bodyConditions", BodyCondition.values());
        model.addAttribute("auctionTypes", AuctionType.values());
        return "auctions/createAuction";
    }

    @PostMapping("/create")
    public String createAuction(@RequestParam("title") String title,
                                @RequestParam("description") String description,
                                @RequestParam(value = "carMake", required = false) String carMake,
                                @RequestParam(value = "carModel", required = false) String carModel,
                                @RequestParam(value = "customCarMake", required = false) String customCarMake,
                                @RequestParam(value = "customCarModel", required = false) String customCarModel,
                                @RequestParam("carGeneration") String carGeneration,
                                @RequestParam("year") int year,
                                @RequestParam("mileage") String mileage,
                                @RequestParam("fuelType") Auction.FuelType fuelType,
                                @RequestParam("transmission") Auction.TransmissionType transmission,
                                @RequestParam("startingPrice") String startingPrice,
                                @RequestParam("auctionType") Auction.AuctionType auctionType,
                                @RequestParam("country") String country,
                                @RequestParam("region") String region,
                                @RequestParam("cityOrVillage") String cityOrVillage,
                                @RequestParam("carColor") String carColor,
                                @RequestParam("numberOfDoors") int numberOfDoors,
                                @RequestParam("bodyType") Auction.BodyType bodyType,
                                @RequestParam(value = "engineSize", required = false) Float engineSize,
                                @RequestParam("horsepower") int horsepower,
                                @RequestParam(value = "hasAirConditioning", defaultValue = "false") boolean hasAirConditioning,
                                @RequestParam(value = "hasNavigationSystem", defaultValue = "false") boolean hasNavigationSystem,
                                @RequestParam(value = "hasBeenInAccident", defaultValue = "false") boolean hasBeenInAccident,
                                @RequestParam(value = "cityFuelConsumption", required = false) Float cityFuelConsumption,
                                @RequestParam(value = "highwayFuelConsumption", required = false) Float highwayFuelConsumption,
                                @RequestParam(value = "combinedFuelConsumption", required = false) Float combinedFuelConsumption,
                                @RequestParam("driveType") Auction.DriveType driveType,
                                @RequestParam("engineMarking") String engineMarking,
                                @RequestParam("vin") String vin,
                                @RequestParam("interiorColor") String interiorColor,
                                @RequestParam("interiorMaterial") String interiorMaterial,
                                @RequestParam(value = "hasMultimediaSystem", defaultValue = "false") boolean hasMultimediaSystem,
                                @RequestParam(value = "euroStandard", required = false) Auction.EuroStandard euroStandard,
                                @RequestParam(value = "epaStandard", required = false) Auction.EPAStandard epaStandard,
                                @RequestParam("technicalCondition") Auction.TechnicalCondition technicalCondition,
                                @RequestParam("bodyCondition") Auction.BodyCondition bodyCondition,
                                @RequestParam(value = "carImagesUrls") MultipartFile[] photos,
                                @RequestParam("startTimeHidden") String startTimeStr,
                                @RequestParam("endTimeHidden") String endTimeStr,
                                @RequestParam("userTimeZone") String userTimeZone,
                                Model model) throws IOException {

        if (startTimeStr == null || startTimeStr.isEmpty()) {
            model.addAttribute("error", "Start time is required.");
            return "auctions/createAuction";
        }

        ZoneId userZoneId = ZoneId.of(userTimeZone);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm").withZone(userZoneId);

        ZonedDateTime zonedStartTime = ZonedDateTime.parse(startTimeStr, formatter);
        ZonedDateTime zonedEndTime = null;

        if (endTimeStr != null && !endTimeStr.isEmpty()) {
            zonedEndTime = ZonedDateTime.parse(endTimeStr, formatter);
        }

        Date startTime = Date.from(zonedStartTime.withZoneSameInstant(ZoneOffset.UTC).toInstant());
        Date endTime = zonedEndTime != null ? Date.from(zonedEndTime.withZoneSameInstant(ZoneOffset.UTC).toInstant()) : null;


        User currentUser = userService.getCurrentUser();

        if(!currentUser.isVerified()) {
            model.addAttribute("message", "Check your profile settings and verify your account to create lots for auction.");
            return "error";
        }
        if(!currentUser.isActivated()) {
            model.addAttribute("message", "Check your email and activate your account.");
            return "error";
        }
        if(currentUser.isBlocked()) {
            model.addAttribute("message", "Your account was blocked.");
            return "error";
        }

        if ((carMake == null || carMake.isEmpty()) && (customCarMake == null || customCarMake.isEmpty())) {
            model.addAttribute("error", "Please provide a car brand.");
            return "auctions/createAuction";
        }

        if ((carModel == null || carModel.isEmpty()) && (customCarModel == null || customCarModel.isEmpty())) {
            model.addAttribute("error", "Please provide a car model.");
            return "auctions/createAuction";
        }

        if (customCarMake != null && !customCarMake.isEmpty()) {
            carMake = customCarMake;
        }
        if (customCarModel != null && !customCarModel.isEmpty()) {
            carModel = customCarModel;
        }

        if (fuelType != FuelType.ELECTRIC) {
            if (engineSize == null) {
                model.addAttribute("error", "Engine size is required for non-electric vehicles.");
                return "auctions/createAuction";
            }
            if (cityFuelConsumption == null || highwayFuelConsumption == null || combinedFuelConsumption == null) {
                model.addAttribute("error", "Fuel consumption values are required for non-electric vehicles.");
                return "auctions/createAuction";
            }
            if (euroStandard == null) {
                model.addAttribute("error", "Euro standard is required for non-electric vehicles.");
                return "auctions/createAuction";
            }
            if (epaStandard == null) {
                model.addAttribute("error", "EPA standard is required for non-electric vehicles.");
                return "auctions/createAuction";
            }
        }

        if (fuelType == FuelType.ELECTRIC) {
            if (engineSize != null) {
                model.addAttribute("error", "Engine size should be null for electric vehicles.");
                return "auctions/createAuction";
            }
            if (cityFuelConsumption != null || highwayFuelConsumption != null || combinedFuelConsumption != null) {
                model.addAttribute("error", "Fuel consumption should be null for electric vehicles.");
                return "auctions/createAuction";
            }
            if (euroStandard != null) {
                model.addAttribute("error", "Euro standard should be null for electric vehicles.");
                return "auctions/createAuction";
            }
            if (epaStandard != null) {
                model.addAttribute("error", "EPA standard should be null for electric vehicles.");
                return "auctions/createAuction";
            }
        }

        ZonedDateTime currentDateTime = ZonedDateTime.now(ZoneId.systemDefault());
        if (auctionType == Auction.AuctionType.STANDARD) {
            if (endTime == null) {
                model.addAttribute("error", "End time is required for standard auctions.");
                return "auctions/createAuction";
            }
        } else if (auctionType == Auction.AuctionType.LIVE_BID) {
            if (endTime == null ) {
                model.addAttribute("error", "End time is required for live bid auctions.");
                return "auctions/createAuction";
            }
        } else if (auctionType == Auction.AuctionType.USED_CAR_SALE) {
            endTime = null;
        }

        Auction auction = new Auction();
        auction.setTitle(title);
        auction.setDescription(description);
        auction.setCarMake(carMake);
        auction.setCarModel(carModel);
        auction.setCarGeneration(carGeneration);
        auction.setYear(year);
        auction.setMileage(Integer.parseInt(mileage.replace(" ", "")));
        auction.setFuelType(fuelType);
        auction.setTransmission(transmission);
        auction.setStartingPrice(Integer.parseInt(startingPrice.replace(" ", "")));
        auction.setAuctionType(auctionType);
        auction.setCountry(country);
        auction.setRegion(region);
        auction.setCityOrVillage(cityOrVillage);
        auction.setCarColor(carColor);
        auction.setNumberOfDoors(numberOfDoors);
        auction.setBodyType(bodyType);
        auction.setHorsepower(horsepower);
        auction.setHasAirConditioning(hasAirConditioning);
        auction.setHasNavigationSystem(hasNavigationSystem);
        auction.setHasBeenInAccident(hasBeenInAccident);
        auction.setDriveType(driveType);
        auction.setEngineMarking(engineMarking);

        if (auctionRepository.findByVin(vin).isPresent()) {
            model.addAttribute("error", "An auction with this VIN already exists.");
            return "auctions/createAuction";
        }

        auction.setVin(vin);

        auction.setInteriorColor(interiorColor);
        auction.setInteriorMaterial(interiorMaterial);
        auction.setHasMultimediaSystem(hasMultimediaSystem);
        auction.setTechnicalCondition(technicalCondition);
        auction.setBodyCondition(bodyCondition);
        auction.setStartTime(startTime);
        auction.setEndTime(endTime);
        auction.setSeller(currentUser);

        if (fuelType != FuelType.ELECTRIC){
            auction.setEngineSize(Double.parseDouble(String.valueOf(engineSize)));
            auction.setCityFuelConsumption(cityFuelConsumption);
            auction.setHighwayFuelConsumption(highwayFuelConsumption);
            auction.setCombinedFuelConsumption(combinedFuelConsumption);
            auction.setEuroStandard(euroStandard);
            auction.setEpaStandard(epaStandard);
        }

        if (photos.length == 0) {
            model.addAttribute("error", "Please upload at least one photo.");
            return "auctions/createAuction";
        }

        auctionService.uploadImgsAndSaveAuction(auction, photos);
        return "redirect:/auctions/myAuctions";
    }

    @GetMapping("/standard")
    public String getStandardAuctions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String carMake,
            @RequestParam(required = false) String carModel,
            @RequestParam(required = false) Integer yearFrom,
            @RequestParam(required = false) Integer yearTo,
            @RequestParam(required = false) Integer mileageFrom,
            @RequestParam(required = false) Integer mileageTo,
            @RequestParam(required = false) Integer horsepowerFrom,
            @RequestParam(required = false) Integer horsepowerTo,
            @RequestParam(required = false) Integer priceFrom,
            @RequestParam(required = false) Integer priceTo,
            @RequestParam(required = false) Double engineSizeFrom,
            @RequestParam(required = false) Double engineSizeTo,
            @RequestParam(required = false) Auction.FuelType fuelType,
            @RequestParam(required = false) Auction.TransmissionType transmission,
            @RequestParam(required = false) Auction.BodyType bodyType,
            @RequestParam(required = false) Auction.DriveType driveType,
            @RequestParam(required = false) Auction.TechnicalCondition technicalCondition,
            @RequestParam(required = false) Auction.BodyCondition bodyCondition,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) Integer numberOfDoors,
            @RequestParam(defaultValue = "year") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Model model
    ) {
        return getAuctionsByType(
                Auction.AuctionType.STANDARD,
                "auctions/standard/standardList",
                page, size, carMake, carModel, yearFrom, yearTo,
                mileageFrom, mileageTo, horsepowerFrom, horsepowerTo,
                priceFrom, priceTo, engineSizeFrom, engineSizeTo,
                fuelType, transmission, bodyType, driveType,
                technicalCondition, bodyCondition,
                country, region, numberOfDoors,
                sortBy, sortDir,
                model
        );
    }

    @GetMapping("/livebid")
    public String getLiveBidAuctions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String carMake,
            @RequestParam(required = false) String carModel,
            @RequestParam(required = false) Integer yearFrom,
            @RequestParam(required = false) Integer yearTo,
            @RequestParam(required = false) Integer mileageFrom,
            @RequestParam(required = false) Integer mileageTo,
            @RequestParam(required = false) Integer horsepowerFrom,
            @RequestParam(required = false) Integer horsepowerTo,
            @RequestParam(required = false) Integer priceFrom,
            @RequestParam(required = false) Integer priceTo,
            @RequestParam(required = false) Double engineSizeFrom,
            @RequestParam(required = false) Double engineSizeTo,
            @RequestParam(required = false) Auction.FuelType fuelType,
            @RequestParam(required = false) Auction.TransmissionType transmission,
            @RequestParam(required = false) Auction.BodyType bodyType,
            @RequestParam(required = false) Auction.DriveType driveType,
            @RequestParam(required = false) Auction.TechnicalCondition technicalCondition,
            @RequestParam(required = false) Auction.BodyCondition bodyCondition,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) Integer numberOfDoors,
            @RequestParam(defaultValue = "year") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Model model
    ) {
        return getAuctionsByType(
                Auction.AuctionType.LIVE_BID,
                "auctions/livebid/livebidList",
                page, size, carMake, carModel, yearFrom, yearTo,
                mileageFrom, mileageTo, horsepowerFrom, horsepowerTo,
                priceFrom, priceTo, engineSizeFrom, engineSizeTo,
                fuelType, transmission, bodyType, driveType,
                technicalCondition, bodyCondition,
                country, region, numberOfDoors,
                sortBy, sortDir,
                model
        );
    }

    @GetMapping("/usedcarsale")
    public String getUsedCarSaleAuctions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String carMake,
            @RequestParam(required = false) String carModel,
            @RequestParam(required = false) Integer yearFrom,
            @RequestParam(required = false) Integer yearTo,
            @RequestParam(required = false) Integer mileageFrom,
            @RequestParam(required = false) Integer mileageTo,
            @RequestParam(required = false) Integer horsepowerFrom,
            @RequestParam(required = false) Integer horsepowerTo,
            @RequestParam(required = false) Integer priceFrom,
            @RequestParam(required = false) Integer priceTo,
            @RequestParam(required = false) Double engineSizeFrom,
            @RequestParam(required = false) Double engineSizeTo,
            @RequestParam(required = false) Auction.FuelType fuelType,
            @RequestParam(required = false) Auction.TransmissionType transmission,
            @RequestParam(required = false) Auction.BodyType bodyType,
            @RequestParam(required = false) Auction.DriveType driveType,
            @RequestParam(required = false) Auction.TechnicalCondition technicalCondition,
            @RequestParam(required = false) Auction.BodyCondition bodyCondition,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) Integer numberOfDoors,
            @RequestParam(defaultValue = "year") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Model model
    ) {
        return getAuctionsByType(
                Auction.AuctionType.USED_CAR_SALE,
                "auctions/usedcar/usedcarList",
                page, size, carMake, carModel, yearFrom, yearTo,
                mileageFrom, mileageTo, horsepowerFrom, horsepowerTo,
                priceFrom, priceTo, engineSizeFrom, engineSizeTo,
                fuelType, transmission, bodyType, driveType,
                technicalCondition, bodyCondition,
                country, region, numberOfDoors,
                sortBy, sortDir,
                model
        );
    }

    @GetMapping("/view/{id}")
    public String viewAuction(@PathVariable Long id, Model model) {
        Auction auction = auctionService.findPubliclyVisibleAuctionById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Auction not found or not available"));

        User currentUser = userService.getCurrentUser();

        if (!currentUser.isVerified()) {
            model.addAttribute("message", "Check your profile settings and verify your account to create lots for auction.");
            return "error";
        }
        if (!currentUser.isActivated()) {
            model.addAttribute("message", "Check your email and activate your account.");
            return "error";
        }
        if (currentUser.isBlocked()) {
            model.addAttribute("message", "Your account was blocked.");
            return "error";
        }
        if (auction.getSeller().getId().equals(currentUser.getId())) {
            return "redirect:/auctions/myView/" + id;
        }

        model.addAttribute("auction", auction);

        List<Review> reviews = reviewRepository.findByAuctionAndType(auction, Review.ReviewType.AUCTION);
        double averageRating = reviews.isEmpty()
                ? 0.0
                : reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);


        boolean isSaved = currentUser.getSavedAuctions().stream()
                .anyMatch(saved -> saved.getId().equals(auction.getId()));

        model.addAttribute("isSaved", isSaved);
        model.addAttribute("reviews", reviews);
        model.addAttribute("averageRating", averageRating);

        if (auction.getAuctionType().name().equals("STANDARD") || auction.getAuctionType().name().equals("LIVE_BID")) {
            Timestamp start = (Timestamp) auction.getStartTime();
            Timestamp end = (Timestamp) auction.getEndTime();

            Instant startUtc = start.toLocalDateTime().atOffset(ZoneOffset.UTC).toInstant();
            Instant endUtc = end.toLocalDateTime().atOffset(ZoneOffset.UTC).toInstant();
            Instant now = Instant.now();

            boolean isStandardActive = auction.getAuctionType().name().equals("STANDARD") && now.isBefore(endUtc);
            boolean isLiveBidActive = auction.getAuctionType().name().equals("LIVE_BID")
                    && now.isAfter(startUtc) && now.isBefore(endUtc);
            boolean isLiveBidBeforeStart = auction.getAuctionType().name().equals("LIVE_BID")
                    && now.isBefore(startUtc);
            boolean isLiveBidEnded = auction.getAuctionType().name().equals("LIVE_BID")
                    && now.isAfter(endUtc);

            model.addAttribute("isStandardActive", isStandardActive);
            model.addAttribute("isLiveBidActive", isLiveBidActive);
            model.addAttribute("isLiveBidBeforeStart", isLiveBidBeforeStart);
            model.addAttribute("isLiveBidEnded", isLiveBidEnded);
        }

        return "auctions/view/auctionDetails";
    }



    @GetMapping("/myView/{id}")
    public String viewMyAuction(@PathVariable Long id, Model model) {
        Auction auction = auctionService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Auction not found"));

        User currentUser = userService.getCurrentUser();

        if (!currentUser.isVerified()) {
            model.addAttribute("message", "Check your profile settings and verify your account to create lots for auction.");
            return "error";
        }
        if (!currentUser.isActivated()) {
            model.addAttribute("message", "Check your email and activate your account.");
            return "error";
        }
        if (currentUser.isBlocked()) {
            model.addAttribute("message", "Your account was blocked.");
            return "error";
        }
        if (!auction.getSeller().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the owner of this auction");
        }

        boolean hasBids = !auction.getBids().isEmpty();
        boolean isEditableStatus =
                auction.getStatus() == Auction.AuctionStatus.NOT_SOLD ||
                        auction.getVerificationStatus() == Auction.AuctionVerificationStatus.PENDING ||
                        auction.getVerificationStatus() == Auction.AuctionVerificationStatus.REJECTED;

        boolean canEditAll = !hasBids || isEditableStatus;
        boolean canEditDescription = auction.getNewOwner() == null;

        List<Review> auctionReviews = reviewRepository.findByAuctionAndType(auction, Review.ReviewType.AUCTION);
        double averageRating = auctionReviews.isEmpty()
                ? 0.0
                : auctionReviews.stream().mapToInt(Review::getRating).average().orElse(0.0);

        model.addAttribute("canEditAll", canEditAll);
        model.addAttribute("canEditDescription", canEditDescription);
        model.addAttribute("auction", auction);
        model.addAttribute("reviews", auctionReviews);
        model.addAttribute("averageRating", averageRating);

        return "auctions/view/myAuctionDetails";
    }


    @PostMapping("/{id}/bid")
    public String placeBid(@PathVariable Long id,
                           @RequestParam int proposedPrice,
                           RedirectAttributes redirectAttributes,
                           Principal principal, Model model) {
        try {
            Auction auction = auctionService.findPubliclyVisibleAuctionById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Auction not found"));

            User currentUser = userService.getUserByUsername(principal.getName());

            if(!currentUser.isVerified()) {
                model.addAttribute("message", "Check your profile settings and verify your account to create lots for auction.");
                return "error";
            }
            if(!currentUser.isActivated()) {
                model.addAttribute("message", "Check your email and activate your account.");
                return "error";
            }
            if(currentUser.isBlocked()) {
                model.addAttribute("message", "Your account was blocked.");
                return "error";
            }

            auctionService.placeBid(auction, currentUser, proposedPrice);
            redirectAttributes.addFlashAttribute("success", "Your bid was placed successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to place bid: " + e.getMessage());
        }

        return "redirect:/auctions/view/" + id;
    }

    @PostMapping("/{auctionId}/sell-to/{userId}")
    public String sellToUser(@PathVariable Long auctionId,
                             @PathVariable Long userId,
                             RedirectAttributes redirectAttributes,
                             Principal principal,
                             Model model) {

        Auction auction = auctionService.findById(auctionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Auction not found"));
        User currentUser = userService.getCurrentUser();

        if (!currentUser.isVerified()) {
            model.addAttribute("message", "Check your profile settings and verify your account to create lots for auction.");
            return "error";
        }
        if (!currentUser.isActivated()) {
            model.addAttribute("message", "Check your email and activate your account.");
            return "error";
        }
        if (currentUser.isBlocked()) {
            model.addAttribute("message", "Your account was blocked.");
            return "error";
        }

        if (!auction.getAuctionType().name().equals("USED_CAR_SALE")) {
            model.addAttribute("error", "This operation is allowed only for used car sales.");
            return "error";
        }
        if (!auction.getSeller().getId().equals(currentUser.getId())) {
            model.addAttribute("error", "You are not authorized to sell this auction.");
            return "error";
        }

        Optional<User> buyerOpt = userRepository.findById(userId);
        if (buyerOpt.isEmpty()) {
            model.addAttribute("error", "User not found.");
            return "error";
        }

        List<User> usersWithAuctionSaved = userRepository.findAllBySavedAuctionsContains(auction);
        for (User user : usersWithAuctionSaved) {
            user.getSavedAuctions().remove(auction);
        }
        userRepository.saveAll(usersWithAuctionSaved);

        auction.setNewOwner(buyerOpt.get());
        auction.setStatus(Auction.AuctionStatus.WAITING_FOR_SHIPMENT);
        auctionService.saveAuction(auction);

        reportRepository.deleteAllByReportedAuction(auction);

        try {
            String token = urlSafeEncryptionService.encodeId(auction.getId());
            String verifyLink = appDomain + "/verifyDelivery/" + token;
            byte[] pdf = pdfGenerationService.generateAuctionDeliveryPdf(auction, verifyLink);
            emailService.sendAuctionWinEmailWithAttachment(buyerOpt.get().getEmail(), pdf);
        } catch (Exception e) {
            System.err.println("Error sending win email: " + e.getMessage());
        }

        redirectAttributes.addFlashAttribute("success", "Car has been successfully assigned to the buyer.");
        return "redirect:/auctions/myView/" + auctionId;
    }

    private String getAuctionsByType(
            Auction.AuctionType type,
            String viewPath,
            int page, int size,
            String carMake, String carModel,
            Integer yearFrom, Integer yearTo,
            Integer mileageFrom, Integer mileageTo,
            Integer horsepowerFrom, Integer horsepowerTo,
            Integer priceFrom, Integer priceTo,
            Double engineSizeFrom, Double engineSizeTo,
            Auction.FuelType fuelType, Auction.TransmissionType transmission,
            Auction.BodyType bodyType, Auction.DriveType driveType,
            Auction.TechnicalCondition technicalCondition, Auction.BodyCondition bodyCondition,
            String country, String region, Integer numberOfDoors,
            String sortBy, String sortDir,
            Model model
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Optional<String> carMakeOpt = Optional.ofNullable(carMake).filter(s -> !s.isBlank());
        Optional<String> carModelOpt = Optional.ofNullable(carModel).filter(s -> !s.isBlank());
        Optional<String> countryOpt = Optional.ofNullable(country).filter(s -> !s.isBlank());
        Optional<String> regionOpt = Optional.ofNullable(region).filter(s -> !s.isBlank());

        Page<Auction> auctions = auctionService.findFilteredAuctions(
                type,
                carMakeOpt,
                carModelOpt,
                Optional.ofNullable(yearFrom),
                Optional.ofNullable(yearTo),
                Optional.ofNullable(mileageFrom),
                Optional.ofNullable(mileageTo),
                Optional.ofNullable(horsepowerFrom),
                Optional.ofNullable(horsepowerTo),
                Optional.ofNullable(priceFrom),
                Optional.ofNullable(priceTo),
                Optional.ofNullable(engineSizeFrom),
                Optional.ofNullable(engineSizeTo),
                Optional.ofNullable(fuelType),
                Optional.ofNullable(transmission),
                Optional.ofNullable(bodyType),
                Optional.ofNullable(driveType),
                Optional.ofNullable(technicalCondition),
                Optional.ofNullable(bodyCondition),
                countryOpt,
                regionOpt,
                Optional.ofNullable(numberOfDoors),
                pageable
        );

        model.addAttribute("auctions", auctions.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", auctions.getTotalPages());

        model.addAttribute("carMake", carMake);
        model.addAttribute("carModel", carModel);
        model.addAttribute("carMakes", auctionService.getAvailableMakes());
        model.addAttribute("yearFrom", yearFrom);
        model.addAttribute("yearTo", yearTo);
        model.addAttribute("mileageFrom", mileageFrom);
        model.addAttribute("mileageTo", mileageTo);
        model.addAttribute("horsepowerFrom", horsepowerFrom);
        model.addAttribute("horsepowerTo", horsepowerTo);
        model.addAttribute("priceFrom", priceFrom);
        model.addAttribute("priceTo", priceTo);
        model.addAttribute("engineSizeFrom", engineSizeFrom);
        model.addAttribute("engineSizeTo", engineSizeTo);
        model.addAttribute("fuelType", fuelType);
        model.addAttribute("transmission", transmission);
        model.addAttribute("bodyType", bodyType);
        model.addAttribute("driveType", driveType);
        model.addAttribute("technicalCondition", technicalCondition);
        model.addAttribute("bodyCondition", bodyCondition);
        model.addAttribute("country", country);
        model.addAttribute("countries", auctionService.getAvailableCountry());
        model.addAttribute("region", region);
        model.addAttribute("numberOfDoors", numberOfDoors);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);

        return viewPath;
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Auction> optionalAuction = auctionRepository.findById(id);
        if (optionalAuction.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Auction not found.");
            return "redirect:/auctions/myAuctions";
        }

        Auction auction = optionalAuction.get();
        User currentUser = userService.getCurrentUser();

        if (!auction.getSeller().getId().equals(currentUser.getId())) {
            redirectAttributes.addFlashAttribute("error", "You are not authorized to edit this auction.");
            return "redirect:/auctions/myAuctions";
        }

        boolean hasBids = !auction.getBids().isEmpty();
        boolean isDeletableStatus =
                auction.getStatus() == Auction.AuctionStatus.NOT_SOLD ||
                        auction.getVerificationStatus() == Auction.AuctionVerificationStatus.PENDING ||
                        auction.getVerificationStatus() == Auction.AuctionVerificationStatus.REJECTED;

        boolean canDelete = !hasBids || isDeletableStatus;

        model.addAttribute("auction", auction);
        model.addAttribute("canEditAll", !hasBids);
        model.addAttribute("canDelete", canDelete);
        return "auctions/editAuction";
    }

    @PostMapping("/edit/{id}")
    public String updateAuction(@PathVariable Long id,
                                @ModelAttribute("auction") Auction updatedAuction,
                                RedirectAttributes redirectAttributes) {

        Optional<Auction> optionalAuction = auctionRepository.findById(id);
        if (optionalAuction.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Auction not found.");
            return "redirect:/auctions/myAuctions";
        }

        Auction auction = optionalAuction.get();
        User currentUser = userService.getCurrentUser();

        if (!auction.getSeller().getId().equals(currentUser.getId())) {
            redirectAttributes.addFlashAttribute("error", "You are not authorized to update this auction.");
            return "redirect:/auctions/myAuctions";
        }

        boolean hasBids = !auction.getBids().isEmpty();

        auction.setDescription(updatedAuction.getDescription());

        if (!hasBids) {
            auction.setTitle(updatedAuction.getTitle());
            auction.setStartingPrice(updatedAuction.getStartingPrice());
            auction.setCityOrVillage(updatedAuction.getCityOrVillage());
            auction.setTechnicalCondition(updatedAuction.getTechnicalCondition());
            auction.setBodyCondition(updatedAuction.getBodyCondition());
            auction.setCarColor(updatedAuction.getCarColor());
            auction.setInteriorColor(updatedAuction.getInteriorColor());
            auction.setInteriorMaterial(updatedAuction.getInteriorMaterial());
            auction.setEngineMarking(updatedAuction.getEngineMarking());
            auction.setNumberOfDoors(updatedAuction.getNumberOfDoors());
            auction.setEuroStandard(updatedAuction.getEuroStandard());
            auction.setEpaStandard(updatedAuction.getEpaStandard());
            auction.setHasAirConditioning(updatedAuction.isHasAirConditioning());
            auction.setHasMultimediaSystem(updatedAuction.isHasMultimediaSystem());
            auction.setHasNavigationSystem(updatedAuction.isHasNavigationSystem());
            auction.setDriveType(updatedAuction.getDriveType());
        }

        auctionRepository.save(auction);
        redirectAttributes.addFlashAttribute("success", "Auction updated successfully.");
        return "redirect:/auctions/myAuctions";
    }

    @PostMapping("/delete/{id}")
    public String deleteAuction(@PathVariable Long id,
                                RedirectAttributes redirectAttributes) {

        Optional<Auction> optionalAuction = auctionRepository.findById(id);
        if (optionalAuction.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Auction not found.");
            return "redirect:/auctions/myAuctions";
        }

        Auction auction = optionalAuction.get();
        User currentUser = userService.getCurrentUser();

        boolean isOwner = auction.getSeller().getId().equals(currentUser.getId());
        boolean noBids = auction.getBids().isEmpty();
        boolean isDeletableStatus = auction.getStatus() == AuctionStatus.NOT_SOLD
                || auction.getVerificationStatus() == AuctionVerificationStatus.PENDING
                || auction.getVerificationStatus() == AuctionVerificationStatus.REJECTED;

        if (!isOwner) {
            redirectAttributes.addFlashAttribute("error", "You are not authorized to delete this auction.");
        } else if (noBids || isDeletableStatus) {
            auctionService.deleteAuction(auction);
            redirectAttributes.addFlashAttribute("success", "Auction deleted successfully.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Auction cannot be deleted after receiving bids.");
        }

        return "redirect:/auctions/myAuctions";
    }

    @PostMapping("/{id}/handOver")
    public String markAsHandedOver(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Auction auction = auctionService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (auction.getStatus() == Auction.AuctionStatus.WAITING_FOR_SHIPMENT && auction.getNewOwner() != null) {
            auction.setStatus(Auction.AuctionStatus.HANDED_OVER_TO_DELIVERY);
            auctionRepository.save(auction);
            redirectAttributes.addFlashAttribute("success", "Auction marked as handed over to delivery.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Invalid action.");
        }

        return "redirect:/auctions/myView/" + id;
    }

    @PostMapping("/{id}/markReceived")
    public String markAuctionAsReceived(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Auction auction = auctionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Auction not found"));

        if (auction.getStatus() != Auction.AuctionStatus.HANDED_OVER_TO_DELIVERY) {
            redirectAttributes.addFlashAttribute("error", "You can only mark as received if the car was handed over.");
            return "redirect:/auctions/myWonAuctions";
        }

        auction.setStatus(Auction.AuctionStatus.RECEIVED);
        auctionRepository.save(auction);

        redirectAttributes.addFlashAttribute("success", "Auction marked as received successfully.");
        return "redirect:/auctions/myWonAuctions";
    }

    @GetMapping("/myAuctions")
    public String viewMyAuctions(@RequestParam(required = false) Auction.AuctionStatus status,
                                 @RequestParam(required = false) Auction.AuctionType type,
                                 @RequestParam(required = false) Auction.AuctionVerificationStatus verification,
                                 Model model) {
        User currentUser = userService.getCurrentUser();

        if (!currentUser.isVerified()) {
            model.addAttribute("message", "Check your profile settings and verify your account to create lots for auction.");
            return "error";
        }
        if (!currentUser.isActivated()) {
            model.addAttribute("message", "Check your email and activate your account.");
            return "error";
        }

        List<Auction> allAuctions = auctionRepository.findAllBySellerId(currentUser.getId());

        List<Auction> filteredAuctions = allAuctions.stream()
                .filter(a -> status == null || a.getStatus() == status)
                .filter(a -> type == null || a.getAuctionType() == type)
                .filter(a -> verification == null || a.getVerificationStatus() == verification)
                .toList();

        model.addAttribute("auctions", filteredAuctions);
        model.addAttribute("statusOptions", Auction.AuctionStatus.values());
        model.addAttribute("typeOptions", Auction.AuctionType.values());
        model.addAttribute("verificationOptions", Auction.AuctionVerificationStatus.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedType", type);
        model.addAttribute("selectedVerification", verification);

        return "auctions/myAuctions";
    }

    @GetMapping("/myWonAuctions")
    public String getWonAuctions(
            @RequestParam(required = false) Auction.AuctionStatus status,
            @RequestParam(required = false) Auction.AuctionType type,
            Model model) {

        User currentUser = userService.getCurrentUser();

        List<Auction> wonAuctions = auctionRepository.findAllByNewOwnerId(currentUser.getId());

        if (!currentUser.isVerified()) {
            model.addAttribute("message", "Check your profile settings and verify your account to create lots for auction.");
            return "error";
        }
        if (!currentUser.isActivated()) {
            model.addAttribute("message", "Check your email and activate your account.");
            return "error";
        }

        if (status != null) {
            wonAuctions = wonAuctions.stream()
                    .filter(a -> a.getStatus() == status)
                    .toList();
        }

        if (type != null) {
            wonAuctions = wonAuctions.stream()
                    .filter(a -> a.getAuctionType() == type)
                    .toList();
        }

        model.addAttribute("auctions", wonAuctions);
        model.addAttribute("statusOptions", List.of(
                Auction.AuctionStatus.WAITING_FOR_SHIPMENT,
                Auction.AuctionStatus.HANDED_OVER_TO_DELIVERY,
                Auction.AuctionStatus.RECEIVED
        ));
        model.addAttribute("typeOptions", Auction.AuctionType.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedType", type);

        return "auctions/wonAuctions";
    }

    @GetMapping("/myBids")
    public String viewMyBids(@RequestParam(required = false) Auction.AuctionType type,
                             Model model) {
        User currentUser = userService.getCurrentUser();

        if (!currentUser.isVerified()) {
            model.addAttribute("message", "Check your profile settings and verify your account to create lots for auction.");
            return "error";
        }
        if (!currentUser.isActivated()) {
            model.addAttribute("message", "Check your email and activate your account.");
            return "error";
        }
        if (currentUser.isBlocked()) {
            model.addAttribute("message", "Your account was blocked.");
            return "error";
        }

        List<Auction> activeAuctions = auctionRepository.findByStatus(Auction.AuctionStatus.ACTIVE);
        List<Auction> filtered = new ArrayList<>();

        for (Auction auction : activeAuctions) {
            boolean hasUserBid = auction.getBids().stream()
                    .anyMatch(bid -> bid.getUserId().equals(currentUser.getId()));
            if (hasUserBid && (type == null || auction.getAuctionType() == type)) {
                filtered.add(auction);
            }
        }

        model.addAttribute("auctions", filtered);
        model.addAttribute("selectedType", type);
        model.addAttribute("typeOptions", Auction.AuctionType.values());
        model.addAttribute("userId", currentUser.getId());

        return "auctions/myBids";
    }

    @GetMapping("/saved")
    public String viewSavedAuctions(Model model) {
        User currentUser = userService.getCurrentUser();

        if (!currentUser.isVerified()) {
            model.addAttribute("message", "Check your profile settings and verify your account to create lots for auction.");
            return "error";
        }
        if (!currentUser.isActivated()) {
            model.addAttribute("message", "Check your email and activate your account.");
            return "error";
        }
        if (currentUser.isBlocked()) {
            model.addAttribute("message", "Your account was blocked.");
            return "error";
        }

        Set<Auction> savedAuctions = currentUser.getSavedAuctions();
        model.addAttribute("savedAuctions", savedAuctions);

        return "auctions/savedAuctions";
    }


    @PostMapping("/save/{auctionId}")
    public String saveAuction(@PathVariable Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Auction not found"));
        User currentUser = userService.getCurrentUser();
        userService.saveAuctionForUser(currentUser, auction);
        return "redirect:/auctions/view/" + auctionId;
    }

    @PostMapping("/unsave/{auctionId}")
    public String unsaveAuction(@PathVariable Long auctionId,
                                HttpServletRequest request) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Auction not found"));
        User currentUser = userService.getCurrentUser();
        userService.removeSavedAuction(currentUser, auction);

        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/");
    }


    @Scheduled(fixedRate = 60000)
    public void finalizeAuction(){
        auctionService.assignWinnersToFinishedAuctions();
    }

    @GetMapping("/models-by-make")
    @ResponseBody
    public List<String> getModelsByMake(@RequestParam String carMake) {
        return auctionService.getModelsByMake(carMake);
    }

    @GetMapping("/regions-by-country")
    @ResponseBody
    public List<String> getRegionsByCountry(@RequestParam String country) {
        return auctionService.getRegionsByCountry(country);
    }
}

