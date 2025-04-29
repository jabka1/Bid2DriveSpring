package team.bid2drivespring.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import team.bid2drivespring.model.Auction;
import team.bid2drivespring.model.Auction.*;
import team.bid2drivespring.model.User;
import team.bid2drivespring.repository.AuctionRepository;
import team.bid2drivespring.service.AuctionService;
import team.bid2drivespring.service.UserService;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

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

        ZonedDateTime startTime = ZonedDateTime.parse(startTimeStr, formatter);
        ZonedDateTime endTime = null;

        if (endTimeStr != null && !endTimeStr.isEmpty()) {
            endTime = ZonedDateTime.parse(endTimeStr, formatter);
        }

        User currentUser = userService.getCurrentUser();
        
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
}

