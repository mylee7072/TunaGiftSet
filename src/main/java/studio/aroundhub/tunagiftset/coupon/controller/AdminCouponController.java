package studio.aroundhub.tunagiftset.coupon.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import studio.aroundhub.tunagiftset.coupon.dto.CouponCreateRequest;
import studio.aroundhub.tunagiftset.coupon.dto.CouponIssueRequest;
import studio.aroundhub.tunagiftset.coupon.dto.CouponPageResponse;
import studio.aroundhub.tunagiftset.coupon.dto.CouponResponse;
import studio.aroundhub.tunagiftset.coupon.dto.MemberCouponResponse;
import studio.aroundhub.tunagiftset.coupon.service.CouponService;
import studio.aroundhub.tunagiftset.entity.type.CouponStatus;

@RestController
@RequestMapping("/api/admin/coupons")
public class AdminCouponController {

    private final CouponService couponService;

    public AdminCouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CouponResponse create(@Valid @RequestBody CouponCreateRequest request) {
        return couponService.create(request);
    }

    @GetMapping
    public CouponPageResponse findCoupons(
            @RequestParam(required = false) CouponStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return couponService.findAdminCoupons(status, page, size);
    }

    @PostMapping("/{couponId}/issue")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberCouponResponse issue(
            @PathVariable Long couponId,
            @Valid @RequestBody CouponIssueRequest request
    ) {
        return couponService.issue(couponId, request.memberId());
    }
}
