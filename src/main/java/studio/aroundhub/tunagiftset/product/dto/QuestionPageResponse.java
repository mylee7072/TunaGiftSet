package studio.aroundhub.tunagiftset.product.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record QuestionPageResponse(
        List<QuestionResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static QuestionPageResponse from(Page<QuestionResponse> page) {
        return new QuestionPageResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
