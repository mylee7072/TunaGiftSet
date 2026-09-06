package studio.aroundhub.tunagiftset.product.dto;

import java.time.Instant;
import studio.aroundhub.tunagiftset.entity.ProductQuestionAnswer;

public record QuestionAnswerResponse(
        Long id,
        String content,
        String adminName,
        Instant createdAt,
        Instant updatedAt
) {
    public static QuestionAnswerResponse from(ProductQuestionAnswer answer) {
        if (answer == null) {
            return null;
        }
        return new QuestionAnswerResponse(
                answer.getId(),
                answer.getContent(),
                "Seller",
                answer.getCreatedAt(),
                answer.getUpdatedAt()
        );
    }
}
