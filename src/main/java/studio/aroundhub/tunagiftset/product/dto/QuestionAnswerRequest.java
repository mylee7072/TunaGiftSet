package studio.aroundhub.tunagiftset.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record QuestionAnswerRequest(
        @NotBlank(message = "Answer content is required.")
        @Size(max = 2000, message = "Answer content must be 2000 characters or less.")
        String content
) {
}
