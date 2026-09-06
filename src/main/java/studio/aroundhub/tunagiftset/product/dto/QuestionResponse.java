package studio.aroundhub.tunagiftset.product.dto;

import java.time.Instant;
import studio.aroundhub.tunagiftset.entity.ProductQuestion;
import studio.aroundhub.tunagiftset.entity.type.QuestionStatus;

public record QuestionResponse(
        Long id,
        Long productId,
        String productName,
        String title,
        String content,
        boolean secret,
        QuestionStatus status,
        String authorName,
        boolean editable,
        QuestionAnswerResponse answer,
        Instant createdAt,
        Instant updatedAt
) {
    public static QuestionResponse from(ProductQuestion question, Long currentMemberId, boolean admin) {
        boolean owner = currentMemberId != null && question.getMember().getId().equals(currentMemberId);
        boolean canViewSecret = !question.isSecret() || owner || admin;
        return new QuestionResponse(
                question.getId(),
                question.getProduct().getId(),
                question.getProduct().getName(),
                canViewSecret ? question.getTitle() : "Secret question",
                canViewSecret ? question.getContent() : null,
                question.isSecret(),
                question.getStatus(),
                admin ? question.getMember().getEmail() : maskName(question.getMember().getName()),
                owner && question.getStatus() == QuestionStatus.WAITING,
                canViewSecret ? QuestionAnswerResponse.from(question.getAnswer()) : null,
                question.getCreatedAt(),
                question.getUpdatedAt()
        );
    }

    private static String maskName(String name) {
        if (name == null || name.isBlank()) {
            return "Member";
        }
        String trimmed = name.trim();
        if (trimmed.length() <= 1) {
            return trimmed + "*";
        }
        if (trimmed.length() == 2) {
            return trimmed.charAt(0) + "*";
        }
        return trimmed.charAt(0) + "*".repeat(trimmed.length() - 2) + trimmed.charAt(trimmed.length() - 1);
    }
}
