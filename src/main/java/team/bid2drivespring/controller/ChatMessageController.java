package team.bid2drivespring.controller;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import team.bid2drivespring.model.ChatMessage;
import team.bid2drivespring.model.User;
import team.bid2drivespring.repository.ChatMessageRepository;
import team.bid2drivespring.repository.UserRepository;
import team.bid2drivespring.service.ChatMessageService;
import team.bid2drivespring.service.UserService;
import team.bid2drivespring.util.EncryptionUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatMessageController {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final EncryptionUtil encryptionUtil;
    private final ChatMessageService chatMessageService;

    @GetMapping("/{userId}")
    @Transactional
    public String chatWithUser(@PathVariable Long userId, Model model) {
        User currentUser = userService.getCurrentUser();
        User otherUser = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (currentUser.getId().equals(otherUser.getId())) {
            model.addAttribute("message", "You cannot chat with yourself.");
            return "error";
        }

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

        if (otherUser.getRole() == User.Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot start this chat");
        }

        List<ChatMessage> originalMessages = chatMessageRepository.findChatBetweenUsers(currentUser, otherUser);

        originalMessages.stream()
                .filter(msg -> msg.getRecipient().equals(currentUser) && !msg.isRead())
                .forEach(msg -> msg.setRead(true));

        List<ChatMessage> decryptedMessages = originalMessages.stream()
                .map(msg -> {
                    ChatMessage copy = new ChatMessage();
                    copy.setId(msg.getId());
                    copy.setSender(msg.getSender());
                    copy.setRecipient(msg.getRecipient());
                    copy.setTimestamp(msg.getTimestamp());
                    copy.setImage(msg.isImage());
                    copy.setRead(msg.isRead());

                    if (!msg.isImage()) {
                        copy.setContent(encryptionUtil.decrypt(msg.getContent()));
                    } else {
                        copy.setContent(msg.getContent());
                    }

                    return copy;
                }).toList();

        model.addAttribute("messages", decryptedMessages);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("otherUser", otherUser);

        return "chat/chatView";
    }

    @GetMapping
    public String chatList(Model model) {
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

        List<Long> partnerIds = chatMessageRepository.findChatPartnerIds(currentUser);
        List<User> chatPartners = userRepository.findAllById(partnerIds);

        List<Map<String, Object>> chatPreviews = chatPartners.stream().map(partner -> {
            ChatMessage lastMessage = chatMessageRepository.findTopBySenderAndRecipientOrRecipientAndSenderOrderByTimestampDesc(
                    currentUser, partner, currentUser, partner
            );

            boolean hasUnread = chatMessageRepository.existsBySenderAndRecipientAndIsReadFalse(partner, currentUser);

            if (lastMessage != null && !lastMessage.isImage()) {
                lastMessage.setContent(encryptionUtil.decrypt(lastMessage.getContent()));
            }

            Map<String, Object> preview = new HashMap<>();
            preview.put("user", partner);
            preview.put("lastMessage", lastMessage);
            preview.put("unread", hasUnread);
            return preview;
        }).toList();


        model.addAttribute("chatPreviews", chatPreviews);
        return "chat/chatList";
    }


    @PostMapping("/uploadImage")
    @ResponseBody
    public ResponseEntity<String> uploadChatImage(@RequestParam("image") MultipartFile file) throws IOException {
        String url = chatMessageService.uploadChatImage(file);
        return ResponseEntity.ok(url);
    }
}
