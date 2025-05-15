package team.bid2drivespring.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import team.bid2drivespring.dto.ChatMessageDTO;
import team.bid2drivespring.model.ChatMessage;
import team.bid2drivespring.model.User;
import team.bid2drivespring.repository.ChatMessageRepository;
import team.bid2drivespring.repository.UserRepository;
import team.bid2drivespring.util.EncryptionUtil;

import java.time.Instant;

@RequiredArgsConstructor
@Controller
public class WebSocketChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final EncryptionUtil encryptionUtil;

    @MessageMapping("/chat.send")
    public void send(ChatMessageDTO incoming) {
        User sender = userRepository.findById(incoming.senderId())
                .orElseThrow(() -> new IllegalArgumentException("Sender not found"));
        User recipient = userRepository.findById(incoming.recipientId())
                .orElseThrow(() -> new IllegalArgumentException("Recipient not found"));

        String content = incoming.isImage()
                ? incoming.content()
                : encryptionUtil.encrypt(incoming.content());

        ChatMessage msg = ChatMessage.builder()
                .sender(sender)
                .recipient(recipient)
                .content(content)
                .isImage(incoming.isImage())
                .isRead(false)
                .timestamp(Instant.now())
                .build();

        chatMessageRepository.save(msg);

        ChatMessageDTO outgoing = new ChatMessageDTO(
                sender.getId(),
                recipient.getId(),
                incoming.isImage() ? content : encryptionUtil.decrypt(content),
                incoming.isImage(),
                msg.getTimestamp()
        );

        messagingTemplate.convertAndSend("/topic/messages." + recipient.getId(), outgoing);
        messagingTemplate.convertAndSend("/topic/messages." + sender.getId(), outgoing);
    }

}

