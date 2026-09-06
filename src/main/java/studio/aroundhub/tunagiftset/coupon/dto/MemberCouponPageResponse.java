package studio.aroundhub.tunagiftset.coupon.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record MemberCouponPageResponse(
        List<MemberCouponResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static MemberCouponPageResponse from(Page<MemberCouponResponse> page) {
        return new MemberCouponPageResponse(
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
