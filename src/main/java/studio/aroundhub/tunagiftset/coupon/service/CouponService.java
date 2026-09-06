package studio.aroundhub.tunagiftset.coupon.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studio.aroundhub.tunagiftset.cart.service.ShippingFeePolicy;
import studio.aroundhub.tunagiftset.coupon.dto.AvailableCouponResponse;
import studio.aroundhub.tunagiftset.coupon.dto.CheckoutPreviewRequest;
import studio.aroundhub.tunagiftset.coupon.dto.CheckoutPreviewResponse;
import studio.aroundhub.tunagiftset.coupon.dto.CouponCreateRequest;
import studio.aroundhub.tunagiftset.coupon.dto.CouponPageResponse;
import studio.aroundhub.tunagiftset.coupon.dto.CouponResponse;
import studio.aroundhub.tunagiftset.coupon.dto.MemberCouponPageResponse;
import studio.aroundhub.tunagiftset.coupon.dto.MemberCouponResponse;
import studio.aroundhub.tunagiftset.entity.Cart;
import studio.aroundhub.tunagiftset.entity.CartItem;
import studio.aroundhub.tunagiftset.entity.Coupon;
import studio.aroundhub.tunagiftset.entity.Member;
import studio.aroundhub.tunagiftset.entity.MemberCoupon;
import studio.aroundhub.tunagiftset.entity.Order;
import studio.aroundhub.tunagiftset.entity.Product;
import studio.aroundhub.tunagiftset.entity.type.CouponStatus;
import studio.aroundhub.tunagiftset.entity.type.DiscountType;
import studio.aroundhub.tunagiftset.entity.type.MemberCouponStatus;
import studio.aroundhub.tunagiftset.entity.type.ProductStatus;
import studio.aroundhub.tunagiftset.exception.DuplicateResourceException;
import studio.aroundhub.tunagiftset.exception.InvalidRequestException;
import studio.aroundhub.tunagiftset.exception.ResourceNotFoundException;
import studio.aroundhub.tunagiftset.repository.CartItemRepository;
import studio.aroundhub.tunagiftset.repository.CartRepository;
import studio.aroundhub.tunagiftset.repository.CouponRepository;
import studio.aroundhub.tunagiftset.repository.MemberCouponRepository;
import studio.aroundhub.tunagiftset.repository.MemberRepository;

@Service
@Transactional(readOnly = true)
public class CouponService {

    private static final int MAX_PAGE_SIZE = 100;

    private final CouponRepository couponRepository;
    private final MemberCouponRepository memberCouponRepository;
    private final MemberRepository memberRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ShippingFeePolicy shippingFeePolicy;
    private final CouponDiscountCalculator couponDiscountCalculator;
    private final PromotionDiscountService promotionDiscountService;
    private final Clock clock;

    public CouponService(
            CouponRepository couponRepository,
            MemberCouponRepository memberCouponRepository,
            MemberRepository memberRepository,
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            ShippingFeePolicy shippingFeePolicy,
            CouponDiscountCalculator couponDiscountCalculator,
            PromotionDiscountService promotionDiscountService,
            Clock clock
    ) {
        this.couponRepository = couponRepository;
        this.memberCouponRepository = memberCouponRepository;
        this.memberRepository = memberRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.shippingFeePolicy = shippingFeePolicy;
        this.couponDiscountCalculator = couponDiscountCalculator;
        this.promotionDiscountService = promotionDiscountService;
        this.clock = clock;
    }

    @Transactional
    public CouponResponse create(CouponCreateRequest request) {
        validateCouponRequest(request);
        String code = normalizeCode(request.code());
        if (couponRepository.existsByCode(code)) {
            throw new DuplicateResourceException("COUPON_CODE_DUPLICATED", "Coupon code already exists.");
        }

        Coupon coupon = couponRepository.save(new Coupon(
                request.name().trim(),
                code,
                request.discountType(),
                request.discountValue(),
                request.minimumOrderAmount(),
                request.maximumDiscountAmount(),
                request.validFrom(),
                request.validUntil(),
                request.status() == null ? CouponStatus.ACTIVE : request.status(),
                request.totalIssueLimit(),
                request.perMemberLimit() == null ? 1 : request.perMemberLimit()
        ));
        return CouponResponse.from(coupon);
    }

    public CouponPageResponse findAdminCoupons(CouponStatus status, int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        var coupons = status == null ? couponRepository.findAll(pageable) : couponRepository.findAllByStatus(status, pageable);
        return CouponPageResponse.from(coupons.map(CouponResponse::from));
    }

    @Transactional
    public MemberCouponResponse issue(Long couponId, Long memberId) {
        Coupon coupon = couponRepository.findByIdForUpdate(couponId)
                .orElseThrow(() -> new ResourceNotFoundException("COUPON_NOT_FOUND", "Coupon was not found."));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("MEMBER_NOT_FOUND", "Member was not found."));
        validateIssueable(coupon);

        if (coupon.getTotalIssueLimit() != null && memberCouponRepository.countByCouponId(couponId) >= coupon.getTotalIssueLimit()) {
            throw new InvalidRequestException("COUPON_ISSUE_LIMIT_EXCEEDED", "Coupon issue limit was exceeded.");
        }
        if (memberCouponRepository.countByMemberIdAndCouponId(memberId, couponId) >= coupon.getPerMemberLimit()) {
            throw new InvalidRequestException("COUPON_MEMBER_LIMIT_EXCEEDED", "Member coupon issue limit was exceeded.");
        }

        MemberCoupon memberCoupon = memberCouponRepository.save(new MemberCoupon(member, coupon, clock.instant()));
        return MemberCouponResponse.from(memberCoupon, isUsableNow(memberCoupon, clock.instant()));
    }

    public MemberCouponPageResponse findMyCoupons(Long memberId, int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        Instant now = clock.instant();
        return MemberCouponPageResponse.from(memberCouponRepository.findPageByMemberId(memberId, pageable)
                .map(memberCoupon -> MemberCouponResponse.from(memberCoupon, isUsableNow(memberCoupon, now))));
    }

    public CheckoutPreviewResponse preview(Long memberId, CheckoutPreviewRequest request) {
        DiscountCalculation calculation = calculate(memberId, request.cartItemIds(), request.memberCouponId(), false);
        List<AvailableCouponResponse> coupons = availableCoupons(memberId, calculation.productAmount());
        return new CheckoutPreviewResponse(
                calculation.productAmount(),
                calculation.promotionDiscountAmount(),
                calculation.couponDiscountAmount(),
                calculation.shippingFee(),
                calculation.totalAmount(),
                request.memberCouponId(),
                coupons
        );
    }

    public List<AvailableCouponResponse> availableCoupons(Long memberId, List<Long> cartItemIds) {
        BigDecimal productAmount = calculateProductAmount(memberId, cartItemIds);
        return availableCoupons(memberId, productAmount);
    }

    public DiscountCalculation calculate(Long memberId, List<Long> cartItemIds, Long memberCouponId, boolean lockCoupon) {
        BigDecimal productAmount = calculateProductAmount(memberId, cartItemIds);
        return calculateForProductAmount(memberId, memberCouponId, productAmount, lockCoupon);
    }

    public DiscountCalculation calculateForProductAmount(Long memberId, Long memberCouponId, BigDecimal productAmount, boolean lockCoupon) {
        BigDecimal promotionDiscountAmount = promotionDiscountService.calculateOrderDiscount(productAmount);
        BigDecimal couponBaseAmount = productAmount.subtract(promotionDiscountAmount).max(BigDecimal.ZERO);
        MemberCoupon memberCoupon = null;
        BigDecimal couponDiscountAmount = BigDecimal.ZERO;

        if (memberCouponId != null) {
            memberCoupon = lockCoupon
                    ? memberCouponRepository.findByIdAndMemberIdForUpdate(memberCouponId, memberId)
                    .orElseThrow(() -> new ResourceNotFoundException("COUPON_NOT_FOUND", "Member coupon was not found."))
                    : memberCouponRepository.findById(memberCouponId)
                    .filter(mc -> mc.getMember().getId().equals(memberId))
                    .orElseThrow(() -> new ResourceNotFoundException("COUPON_NOT_FOUND", "Member coupon was not found."));
            validateUsableForOrder(memberCoupon, productAmount, clock.instant());
            couponDiscountAmount = couponDiscountCalculator.calculate(memberCoupon.getCoupon(), couponBaseAmount);
        }

        BigDecimal shippingFee = shippingFeePolicy.calculate(productAmount);
        BigDecimal totalAmount = productAmount
                .subtract(promotionDiscountAmount)
                .subtract(couponDiscountAmount)
                .add(shippingFee);
        if (totalAmount.signum() <= 0) {
            throw new InvalidRequestException("ZERO_AMOUNT_ORDER_NOT_SUPPORTED", "Total payment amount must be greater than zero.");
        }

        return new DiscountCalculation(productAmount, promotionDiscountAmount, couponDiscountAmount, shippingFee, totalAmount, memberCoupon);
    }

    @Transactional
    public void markReserved(MemberCoupon memberCoupon, Order order) {
        if (memberCoupon != null) {
            memberCoupon.reserve(order, clock.instant());
        }
    }

    @Transactional
    public void markUsedForOrder(Order order) {
        memberCouponRepository.findByOrderIdForUpdate(order.getId())
                .ifPresent(memberCoupon -> memberCoupon.markUsed(order, clock.instant()));
    }

    @Transactional
    public void restoreForOrder(Order order) {
        memberCouponRepository.findByOrderIdForUpdate(order.getId())
                .ifPresent(memberCoupon -> memberCoupon.restoreFromOrder(order));
    }

    private List<AvailableCouponResponse> availableCoupons(Long memberId, BigDecimal productAmount) {
        Instant now = clock.instant();
        return memberCouponRepository.findByMemberIdAndStatus(memberId, MemberCouponStatus.AVAILABLE).stream()
                .map(memberCoupon -> {
                    String reason = unavailableReason(memberCoupon, productAmount, now);
                    BigDecimal discountAmount = reason == null
                            ? couponDiscountCalculator.calculate(memberCoupon.getCoupon(), productAmount)
                            : BigDecimal.ZERO;
                    return AvailableCouponResponse.of(memberCoupon, reason == null, reason, discountAmount);
                })
                .toList();
    }

    private BigDecimal calculateProductAmount(Long memberId, List<Long> cartItemIds) {
        validateCartItemIds(cartItemIds);
        Cart cart = cartRepository.findByMemberId(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("CART_ITEM_NOT_FOUND", "Orderable cart items were not found."));
        List<CartItem> cartItems = cartItemRepository.findByIdInAndCartId(cartItemIds, cart.getId());
        if (cartItems.size() != cartItemIds.size()) {
            throw new ResourceNotFoundException("CART_ITEM_NOT_FOUND", "Orderable cart items were not found.");
        }
        BigDecimal productAmount = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            if (product.getStatus() == ProductStatus.SOLD_OUT || !product.isPurchasable() || !product.hasEnoughStock(cartItem.getQuantity())) {
                throw new InvalidRequestException("PRODUCT_NOT_AVAILABLE", "Product is not available for purchase.");
            }
            productAmount = productAmount.add(product.getSalePrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }
        return productAmount;
    }

    private void validateCouponRequest(CouponCreateRequest request) {
        if (request.validUntil().isBefore(request.validFrom())) {
            throw new InvalidRequestException("INVALID_COUPON_PERIOD", "Coupon validUntil must be after validFrom.");
        }
        if (request.discountType() == DiscountType.PERCENTAGE && request.discountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new InvalidRequestException("INVALID_COUPON_DISCOUNT", "Percentage coupon must be 100 or less.");
        }
    }

    private void validateIssueable(Coupon coupon) {
        Instant now = clock.instant();
        if (coupon.getStatus() != CouponStatus.ACTIVE) {
            throw new InvalidRequestException("COUPON_NOT_AVAILABLE", "Coupon is not active.");
        }
        if (now.isBefore(coupon.getValidFrom())) {
            throw new InvalidRequestException("COUPON_NOT_STARTED", "Coupon has not started.");
        }
        if (now.isAfter(coupon.getValidUntil())) {
            throw new InvalidRequestException("COUPON_EXPIRED", "Coupon has expired.");
        }
    }

    private void validateUsableForOrder(MemberCoupon memberCoupon, BigDecimal productAmount, Instant now) {
        String reason = unavailableReason(memberCoupon, productAmount, now);
        if (reason != null) {
            throw switch (reason) {
                case "COUPON_NOT_STARTED" -> new InvalidRequestException("COUPON_NOT_STARTED", "Coupon has not started.");
                case "COUPON_EXPIRED" -> new InvalidRequestException("COUPON_EXPIRED", "Coupon has expired.");
                case "COUPON_MINIMUM_ORDER_NOT_MET" -> new InvalidRequestException("COUPON_MINIMUM_ORDER_NOT_MET", "Minimum order amount was not met.");
                case "COUPON_ALREADY_RESERVED" -> new InvalidRequestException("COUPON_ALREADY_RESERVED", "Coupon is already reserved.");
                case "COUPON_ALREADY_USED" -> new InvalidRequestException("COUPON_ALREADY_USED", "Coupon is already used.");
                default -> new InvalidRequestException("COUPON_NOT_AVAILABLE", "Coupon is not available.");
            };
        }
    }

    private String unavailableReason(MemberCoupon memberCoupon, BigDecimal productAmount, Instant now) {
        Coupon coupon = memberCoupon.getCoupon();
        if (memberCoupon.getStatus() == MemberCouponStatus.RESERVED) {
            return "COUPON_ALREADY_RESERVED";
        }
        if (memberCoupon.getStatus() == MemberCouponStatus.USED) {
            return "COUPON_ALREADY_USED";
        }
        if (coupon.getStatus() != CouponStatus.ACTIVE) {
            return "COUPON_NOT_AVAILABLE";
        }
        if (now.isBefore(coupon.getValidFrom())) {
            return "COUPON_NOT_STARTED";
        }
        if (now.isAfter(coupon.getValidUntil())) {
            return "COUPON_EXPIRED";
        }
        if (productAmount.compareTo(coupon.getMinimumOrderAmount()) < 0) {
            return "COUPON_MINIMUM_ORDER_NOT_MET";
        }
        return null;
    }

    private boolean isUsableNow(MemberCoupon memberCoupon, Instant now) {
        return unavailableReason(memberCoupon, memberCoupon.getCoupon().getMinimumOrderAmount(), now) == null;
    }

    private void validateCartItemIds(List<Long> cartItemIds) {
        if (cartItemIds == null || cartItemIds.isEmpty()) {
            throw new InvalidRequestException("EMPTY_ORDER_ITEMS", "At least one cart item must be selected.");
        }
        if (new HashSet<>(cartItemIds).size() != cartItemIds.size()) {
            throw new InvalidRequestException("INVALID_ORDER_ITEM", "Duplicate cart item IDs are not allowed.");
        }
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
