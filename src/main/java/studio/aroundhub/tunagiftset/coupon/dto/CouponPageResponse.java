package studio.aroundhub.tunagiftset.coupon.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record CouponPageResponse(
        List<CouponResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static CouponPageResponse from(Page<CouponResponse> page) {
        return new CouponPageResponse(
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
