package studio.aroundhub.tunagiftset.product.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import studio.aroundhub.tunagiftset.entity.type.ReviewStatus;
import studio.aroundhub.tunagiftset.product.dto.ReviewCreateRequest;
import studio.aroundhub.tunagiftset.product.dto.ReviewPageResponse;
import studio.aroundhub.tunagiftset.product.dto.ReviewResponse;
import studio.aroundhub.tunagiftset.product.dto.ReviewSortType;
import studio.aroundhub.tunagiftset.product.dto.ReviewUpdateRequest;
import studio.aroundhub.tunagiftset.product.service.ReviewService;
import studio.aroundhub.tunagiftset.security.AuthMember;

@RestController
@RequestMapping("/api")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/products/{productId}/reviews")
    public ReviewPageResponse findProductReviews(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long productId,
            @RequestParam(defaultValue = "LATEST") ReviewSortType sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long currentMemberId = authMember == null ? null : authMember.id();
        return reviewService.findProductReviews(productId, currentMemberId, sort, page, size);
    }

    @PostMapping("/products/{productId}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse create(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long productId,
            @Valid @RequestBody ReviewCreateRequest request
    ) {
        return reviewService.create(authMember.id(), productId, request);
    }

    @GetMapping("/members/me/reviews")
    public ReviewPageResponse findMyReviews(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return reviewService.findMyReviews(authMember.id(), page, size);
    }

    @PutMapping("/reviews/{reviewId}")
    public ReviewResponse update(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewUpdateRequest request
    ) {
        return reviewService.update(authMember.id(), reviewId, request);
    }

    @DeleteMapping("/reviews/{reviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long reviewId
    ) {
        reviewService.delete(authMember.id(), reviewId);
    }

    @GetMapping("/admin/reviews")
    public ReviewPageResponse adminFindReviews(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) ReviewStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return reviewService.adminFindReviews(productId, rating, status, page, size);
    }

    @PatchMapping("/admin/reviews/{reviewId}/hide")
    public ReviewResponse hide(@PathVariable Long reviewId) {
        return reviewService.hide(reviewId);
    }

    @PatchMapping("/admin/reviews/{reviewId}/show")
    public ReviewResponse show(@PathVariable Long reviewId) {
        return reviewService.show(reviewId);
    }
}
