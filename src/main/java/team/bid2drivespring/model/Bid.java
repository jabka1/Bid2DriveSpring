package team.bid2drivespring.model;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Embeddable
public class Bid {

    private int proposedPrice;
    private Long userId;
    private String username;

    public Bid() {
    }

    public Bid(int proposedPrice, Long userId, String username) {
        this.proposedPrice = proposedPrice;
        this.userId = userId;
        this.username = username;
    }
}
