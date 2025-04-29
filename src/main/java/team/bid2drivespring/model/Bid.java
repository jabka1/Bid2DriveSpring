package team.bid2drivespring.model;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@Embeddable
public class Bid {

    private BigDecimal proposedPrice;
    private Long userId;

    public Bid() {
    }

    public Bid(BigDecimal proposedPrice, Long userId) {
        this.proposedPrice = proposedPrice;
        this.userId = userId;
    }

}
