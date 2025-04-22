package team.bid2drivespring.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
public class Auction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private String carMake;

    @Column(nullable = false)
    private String carModel;

    @Column(nullable = false)
    private int year;

    @Column(nullable = false)
    private int mileage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FuelType fuelType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransmissionType transmission;

    @Column(nullable = false)
    private BigDecimal startingPrice;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = true)
    private LocalDateTime endTime;

    @Column(nullable = false)
    private boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuctionType auctionType;

    @ManyToOne
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne
    @JoinColumn(name = "buyer_id")
    private User newOwner;

    @Column(nullable = false)
    private String country;

    @Column(nullable = false)
    private String region;

    @Column(nullable = false)
    private String cityOrVillage;

    @Column(nullable = false)
    private String carColor;

    @Column(nullable = false)
    private int numberOfDoors;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BodyType bodyType;

    @Column(nullable = false)
    private Float engineSize;

    @Column(nullable = false)
    private int horsepower;

    @Column(nullable = false)
    private boolean hasAirConditioning;

    @Column(nullable = false)
    private boolean hasNavigationSystem;

    @Column(nullable = true)
    private String carImagesUrls;

    @Column(nullable = false)
    private boolean hasBeenInAccident;

    @Column(nullable = false)
    private Float cityFuelConsumption;

    @Column(nullable = false)
    private Float highwayFuelConsumption;

    @Column(nullable = false)
    private Float combinedFuelConsumption;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DriveType driveType;

    @Column(nullable = true)
    private String engineMarking;

    @Column(nullable = false, unique = true)
    private String vin;

    @Column(nullable = true)
    private String carGeneration;

    @Column(nullable = true)
    private String interiorColor;

    @Column(nullable = true)
    private String interiorMaterial;

    @Column(nullable = false)
    private boolean hasMultimediaSystem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private EuroStandard euroStandard;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private EPAStandard epaStandard;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private TechnicalCondition technicalCondition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private BodyCondition bodyCondition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuctionVerificationStatus verificationStatus = AuctionVerificationStatus.PENDING;

    public enum AuctionType {
        STANDARD,
        LIVE_BID,
        USED_CAR_SALE
    }

    public enum FuelType {
        GASOLINE,
        DIESEL,
        ELECTRIC,
        HYBRID,
        LPG,
        CNG
    }

    public enum TransmissionType {
        MANUAL,
        AUTOMATIC,
        SEMI_AUTOMATIC,
        CVT,
        DCT
    }

    public enum BodyType {
        SEDAN,
        HATCHBACK,
        COUPE,
        CONVERTIBLE,
        SUV,
        PICKUP,
        MINIVAN,
        WAGON,
        ROADSTER,
        CROSSOVER,
        VAN,
        LIFTBACK,
        LIMOUZINE
    }

    public enum DriveType {
        FRONT_WHEEL,
        REAR_WHEEL,
        ALL_WHEEL
    }

    public enum EuroStandard {
        EURO_1,
        EURO_2,
        EURO_3,
        EURO_4,
        EURO_5,
        EURO_6,
        EURO_6D,
        EURO_6D_TEMP
    }

    public enum EPAStandard {
        TIER_1,
        TIER_2,
        TIER_3,
        TIER_4,
        TIER_5
    }

    public enum TechnicalCondition {
        EXCELLENT,
        GOOD,
        FAIR,
        POOR
    }

    public enum BodyCondition {
        NEW,
        GOOD,
        FAIR,
        DAMAGED
    }

    public enum AuctionVerificationStatus {
        PENDING,
        APPROVED,
        REJECTED
    }
}
