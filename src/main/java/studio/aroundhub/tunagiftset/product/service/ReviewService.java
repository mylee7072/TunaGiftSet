package studio.aroundhub.tunagiftset.product.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studio.aroundhub.tunagiftset.entity.OrderItem;
import studio.aroundhub.tunagiftset.entity.Product;
import studio.aroundhub.tunagiftset.entity.Review;
import studio.aroundhub.tunagiftset.entity.type.DeliveryStatus;
import studio.aroundhub.tunagiftset.entity.type.OrderStatus;
import studio.aroundhub.tunagiftset.entity.type.ReviewStatus;
import studio.aroundhub.tunagiftset.exception.InvalidRequestException;
import studio.aroundhub.tunagiftset.exception.ResourceNotFoundException;
import studio.aroundhub.tunagiftset.product.dto.ReviewCreateRequest;
import studio.aroundhub.tunagiftset.product.dto.ReviewPageResponse;
import studio.aroundhub.tunagiftset.product.dto.ReviewResponse;
import studio.aroundhub.tunagiftset.product.dto.ReviewSortType;
import studio.aroundhub.tunagiftset.product.dto.ReviewSummaryResponse;
import studio.aroundhub.tunagiftset.product.dto.ReviewUpdateRequest;
import studio.aroundhub.tunagiftset.repository.DeliveryRepository;
import studio.aroundhub.tunagiftset.repository.MemberRepository;
import studio.aroundhub.tunagiftset.repository.OrderItemRepository;
import studio.aroundhub.tunagiftset.repository.ProductRepository;
import studio.aroundhub.tunagiftset.repository.ReviewRepository.ProductReviewAggregate;
import studio.aroundhub.tunagiftset.repository.ReviewRepository.ReviewAggregate;
import studio.aroundhub.tunagiftset.repository.ReviewRepository;

@Service
@Transactional(readOnly = true)
public class ReviewService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MIN_CONTENT_LENGTH = 10;
    private static final int MAX_CONTENT_LENGTH = 2000;

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final MemberRepository memberRepository;
    private final DeliveryRepository deliveryRepository;

    public ReviewService(
            ReviewRepository reviewRepository,
            ProductRepository productRepository,
            OrderItemRepository orderItemRepository,
            MemberRepository memberRepository,
            DeliveryRepository deliveryRepository
    ) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.orderItemRepository = orderItemRepository;
        this.memberRepository = memberRepository;
        this.deliveryRepository = deliveryRepository;
    }

    @Transactional
    public ReviewResponse create(Long memberId, Long productId, ReviewCreateRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("PRODUCT_NOT_FOUND", "Product was not found."));
        OrderItem orderItem = orderItemRepository.findReviewTargetById(request.orderItemId())
                .orElseThrow(() -> new ResourceNotFoundException("ORDER_ITEM_NOT_FOUND", "Order item was not found."));

        validateReviewTarget(memberId, productId, orderItem);
        if (reviewRepository.existsByOrderItemId(orderItem.getId())) {
            throw new InvalidRequestException("REVIEW_ALREADY_EXISTS", "Review already exists for this order item.");
        }

        Review review = new Review(
                memberRepository.findById(memberId)
                        .orElseThrow(() -> new ResourceNotFoundException("MEMBER_NOT_FOUND", "Member was not found.")),
                product,
                orderItem,
                request.rating(),
                normalizeContent(request.content())
        );

        try {
            return ReviewResponse.from(reviewRepository.saveAndFlush(review), memberId, false);
        } catch (DataIntegrityViolationException exception) {
            throw new InvalidRequestException("REVIEW_ALREADY_EXISTS", "Review already exists for this order item.");
        }
    }

    public ReviewPageResponse findProductReviews(Long productId, Long currentMemberId, ReviewSortType sort, int page, int size) {
        ensureProductExists(productId);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), toSort(sort));
        return ReviewPageResponse.from(reviewRepository
                .findAllByProductIdAndStatus(productId, ReviewStatus.VISIBLE, pageable)
                .map(review -> ReviewResponse.from(review, currentMemberId, false)));
    }

    public ReviewPageResponse findMyReviews(Long memberId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ReviewPageResponse.from(reviewRepository
                .findAllByMemberIdAndStatusNot(memberId, ReviewStatus.DELETED, pageable)
                .map(review -> ReviewResponse.from(review, memberId, false)));
    }

    @Transactional
    public ReviewResponse update(Long memberId, Long reviewId, ReviewUpdateRequest request) {
        Review review = getActiveReview(reviewId);
        if (!review.getMember().getId().equals(memberId)) {
            throw new ResourceNotFoundException("REVIEW_NOT_FOUND", "Review was not found.");
        }
        review.update(request.rating(), normalizeContent(request.content()));
        return ReviewResponse.from(review, memberId, false);
    }

    @Transactional
    public void delete(Long memberId, Long reviewId) {
        Review review = getActiveReview(reviewId);
        if (!review.getMember().getId().equals(memberId)) {
            throw new ResourceNotFoundException("REVIEW_NOT_FOUND", "Review was not found.");
        }
        review.delete();
    }

    public ReviewPageResponse adminFindReviews(Long productId, Integer rating, ReviewStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<Review> specification = productEquals(productId)
                .and(ratingEquals(rating))
                .and(statusEquals(status));
        return ReviewPageResponse.from(reviewRepository.findAll(specification, pageable)
                .map(review -> ReviewResponse.from(review, null, true)));
    }

    @Transactional
    public ReviewResponse hide(Long reviewId) {
        Review review = getActiveReview(reviewId);
        review.hide();
        return ReviewResponse.from(review, null, true);
    }

    @Transactional
    public ReviewResponse show(Long reviewId) {
        Review review = getActiveReview(reviewId);
        review.show();
        return ReviewResponse.from(review, null, true);
    }

    public ReviewSummaryResponse summarize(Long productId) {
        return toSummary(reviewRepository.aggregateByProductId(productId));
    }

    public Map<Long, ReviewSummaryResponse> summarize(Collection<Long> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, ReviewSummaryResponse> result = new HashMap<>();
        for (ProductReviewAggregate aggregate : reviewRepository.aggregateByProductIds(productIds)) {
            result.put(aggregate.getProductId(), toSummary(aggregate));
        }
        return result;
    }

    private void validateReviewTarget(Long memberId, Long productId, OrderItem orderItem) {
        if (!orderItem.getProduct().getId().equals(productId)) {
            throw new InvalidRequestException("REVIEW_NOT_ALLOWED", "Order item does not match the product.");
        }
        if (!orderItem.getOrder().getMember().getId().equals(memberId)) {
            throw new ResourceNotFoundException("ORDER_ITEM_NOT_FOUND", "Order item was not found.");
        }
        if (orderItem.getOrder().getOrderStatus() != OrderStatus.DELIVERED) {
            throw new InvalidRequestException("ORDER_NOT_DELIVERED", "Review can be written only after delivery is completed.");
        }
        boolean delivered = deliveryRepository.findByOrderId(orderItem.getOrder().getId())
                .map(delivery -> delivery.getStatus() == DeliveryStatus.DELIVERED)
                .orElse(false);
        if (!delivered) {
            throw new InvalidRequestException("ORDER_NOT_DELIVERED", "Review can be written only after delivery is completed.");
        }
    }

    private Review getActiveReview(Long reviewId) {
        return reviewRepository.findByIdAndStatusNot(reviewId, ReviewStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("REVIEW_NOT_FOUND", "Review was not found."));
    }

    private void ensureProductExists(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("PRODUCT_NOT_FOUND", "Product was not found.");
        }
    }

    private String normalizeContent(String content) {
        String normalized = content == null ? "" : content.trim();
        if (normalized.length() < MIN_CONTENT_LENGTH || normalized.length() > MAX_CONTENT_LENGTH) {
            throw new InvalidRequestException("INVALID_REVIEW_CONTENT", "Review content must be between 10 and 2000 characters.");
        }
        return normalized;
    }

    private ReviewSummaryResponse toSummary(ReviewAggregate aggregate) {
        if (aggregate == null) {
            return ReviewSummaryResponse.empty();
        }
        BigDecimal average = aggregate.getAverageRating() == null
                ? BigDecimal.ZERO
                : new BigDecimal(aggregate.getAverageRating().toString()).setScale(1, RoundingMode.HALF_UP);
        long count = aggregate.getReviewCount();
        return new ReviewSummaryResponse(average, count);
    }

    private ReviewSummaryResponse toSummary(ProductReviewAggregate aggregate) {
        if (aggregate == null) {
            return ReviewSummaryResponse.empty();
        }
        BigDecimal average = aggregate.getAverageRating() == null
                ? BigDecimal.ZERO
                : new BigDecimal(aggregate.getAverageRating().toString()).setScale(1, RoundingMode.HALF_UP);
        long count = aggregate.getReviewCount();
        return new ReviewSummaryResponse(average, count);
    }

    private Sort toSort(ReviewSortType sort) {
        ReviewSortType sortType = sort == null ? ReviewSortType.LATEST : sort;
        return switch (sortType) {
            case RATING_DESC -> Sort.by(Sort.Direction.DESC, "rating").and(Sort.by(Sort.Direction.DESC, "createdAt"));
            case RATING_ASC -> Sort.by(Sort.Direction.ASC, "rating").and(Sort.by(Sort.Direction.DESC, "createdAt"));
            case LATEST -> Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"));
        };
    }

    private Specification<Review> productEquals(Long productId) {
        return (root, query, cb) -> productId == null ? cb.conjunction() : cb.equal(root.get("product").get("id"), productId);
    }

    private Specification<Review> ratingEquals(Integer rating) {
        return (root, query, cb) -> rating == null ? cb.conjunction() : cb.equal(root.get("rating"), rating);
    }

    private Specification<Review> statusEquals(ReviewStatus status) {
        return (root, query, cb) -> status == null ? cb.notEqual(root.get("status"), ReviewStatus.DELETED) : cb.equal(root.get("status"), status);
    }
}
