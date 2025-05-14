package team.bid2drivespring.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import lombok.Data;

@Entity
@Data
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"author_id", "targetUser_id", "targetAuction_id"}))
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "author_id")
    private User author;

    @ManyToOne(optional = true)
    @JoinColumn(name = "targetUser_id", nullable = true)
    private User target;

    @ManyToOne(optional = true)
    @JoinColumn(name = "targetAuction_id", nullable = true)
    private Auction auction;

    @Column(nullable = false, length = 2000)
    private String comment;

    @Min(1)
    @Max(5)
    @Column(nullable = false)
    private int rating;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewType type;

    public enum ReviewType {
        USER,
        AUCTION
    }
}
