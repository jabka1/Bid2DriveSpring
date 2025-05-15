package team.bid2drivespring.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import team.bid2drivespring.model.ChatMessage;
import team.bid2drivespring.model.User;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("SELECT m FROM ChatMessage m WHERE " +
            "(m.sender = :user1 AND m.recipient = :user2) OR " +
            "(m.sender = :user2 AND m.recipient = :user1) " +
            "ORDER BY m.timestamp ASC")
    List<ChatMessage> findChatBetweenUsers(User user1, User user2);

    @Query("SELECT DISTINCT " +
            "CASE WHEN m.sender = :currentUser THEN m.recipient.id ELSE m.sender.id END " +
            "FROM ChatMessage m " +
            "WHERE m.sender = :currentUser OR m.recipient = :currentUser")
    List<Long> findChatPartnerIds(User currentUser);


    List<ChatMessage> findByRecipientAndIsReadFalse(User recipient);

    ChatMessage findTopBySenderAndRecipientOrRecipientAndSenderOrderByTimestampDesc(User currentUser, User partner, User currentUser1, User partner1);

    boolean existsBySenderAndRecipientAndIsReadFalse(User partner, User currentUser);
}
