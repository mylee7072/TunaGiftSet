package studio.aroundhub.tunagiftset.coupon.controller;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import studio.aroundhub.tunagiftset.coupon.dto.AvailableCouponResponse;
import studio.aroundhub.tunagiftset.coupon.dto.CheckoutPreviewRequest;
import studio.aroundhub.tunagiftset.coupon.dto.CheckoutPreviewResponse;
import studio.aroundhub.tunagiftset.coupon.dto.MemberCouponPageResponse;
import studio.aroundhub.tunagiftset.coupon.service.CouponService;
import studio.aroundhub.tunagiftset.security.AuthMember;

@RestController
@RequestMapping("/api")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @GetMapping("/members/me/coupons")
    public MemberCouponPageResponse findMyCoupons(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return couponService.findMyCoupons(authMember.id(), page, size);
    }

    @PostMapping("/checkout/preview")
    public CheckoutPreviewResponse preview(
            @AuthenticationPrincipal AuthMember authMember,
            @Valid @RequestBody CheckoutPreviewRequest request
    ) {
        return couponService.preview(authMember.id(), request);
    }
}
