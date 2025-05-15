package team.bid2drivespring.dto;

import java.time.Instant;

public record ChatMessageDTO(
        Long senderId,
        Long recipientId,
        String content,
        boolean isImage,
        Instant timestamp
) {}
