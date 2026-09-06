package studio.aroundhub.tunagiftset.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record QuestionCreateRequest(
        @NotBlank(message = "Question title is required.")
        @Size(max = 150, message = "Question title must be 150 characters or less.")
        String title,

        @NotBlank(message = "Question content is required.")
        @Size(max = 2000, message = "Question content must be 2000 characters or less.")
        String content,

        boolean secret
) {
}
