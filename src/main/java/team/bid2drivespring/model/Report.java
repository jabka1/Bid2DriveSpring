package team.bid2drivespring.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 2000)
    private String description;

    private String imageUrl;

    @Enumerated(EnumType.STRING)
    private ReportType type;

    @Enumerated(EnumType.STRING)
    private ReportStatus status;

    @ManyToOne
    private User reporter;

    @ManyToOne
    private User reportedUser;

    @ManyToOne
    private Auction reportedAuction;

    @Column(length = 2000)
    private String adminResponse;

    private Instant createdAt;

    public enum ReportType {
        AUCTION,
        USER
    }

    public enum ReportStatus {
        PENDING,
        APPROVED,
        REJECTED
    }
}
