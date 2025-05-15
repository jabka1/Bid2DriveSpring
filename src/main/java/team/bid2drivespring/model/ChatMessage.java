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
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private User sender;

    @ManyToOne(optional = false)
    private User recipient;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    private boolean isImage;

    private boolean isRead = false;

    private Instant timestamp = Instant.now();
}
