package studio.aroundhub.tunagiftset.product.dto;

import java.time.Instant;
import studio.aroundhub.tunagiftset.entity.Review;
import studio.aroundhub.tunagiftset.entity.type.ReviewStatus;

public record ReviewResponse(
        Long id,
        Long productId,
        Long orderItemId,
        int rating,
        String content,
        String authorName,
        boolean verifiedPurchase,
        boolean editable,
        ReviewStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static ReviewResponse from(Review review, Long currentMemberId, boolean admin) {
        boolean owner = currentMemberId != null && review.getMember().getId().equals(currentMemberId);
        return new ReviewResponse(
                review.getId(),
                review.getProduct().getId(),
                review.getOrderItem().getId(),
                review.getRating(),
                review.getContent(),
                admin ? review.getMember().getEmail() : maskName(review.getMember().getName()),
                true,
                owner,
                review.getStatus(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }

    private static String maskName(String name) {
        if (name == null || name.isBlank()) {
            return "Buyer";
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
