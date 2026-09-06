package studio.aroundhub.tunagiftset.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import studio.aroundhub.tunagiftset.entity.Order;
import studio.aroundhub.tunagiftset.entity.type.DiscountType;
import studio.aroundhub.tunagiftset.entity.type.OrderStatus;

public record OrderDetailResponse(
        String orderNumber,
        String orderName,
        OrderStatus orderStatus,
        Instant orderedAt,
        Instant expiresAt,
        String recipientName,
        String recipientPhone,
        String zipCode,
        String address1,
        String address2,
        String roadAddress,
        String detailAddress,
        String jibunAddress,
        String extraAddress,
        String deliveryMessage,
        List<OrderItemResponse> items,
        BigDecimal productAmount,
        BigDecimal shippingFee,
        BigDecimal promotionDiscountAmount,
        BigDecimal couponDiscountAmount,
        String couponNameSnapshot,
        String couponCodeSnapshot,
        DiscountType couponDiscountTypeSnapshot,
        BigDecimal couponDiscountValueSnapshot,
        BigDecimal totalAmount,
        OrderPaymentResponse payment,
        OrderDeliveryResponse delivery
) {
    public static OrderDetailResponse from(
            Order order,
            List<OrderItemResponse> items,
            OrderPaymentResponse payment,
            OrderDeliveryResponse delivery
    ) {
        return new OrderDetailResponse(
                order.getOrderNumber(),
                buildOrderName(items),
                order.getOrderStatus(),
                order.getOrderedAt(),
                order.getExpiresAt(),
                order.getRecipientName(),
                order.getRecipientPhone(),
                order.getZipCode(),
                order.getAddress1(),
                order.getAddress2(),
                order.getAddress1(),
                order.getAddress2(),
                order.getJibunAddress(),
                order.getExtraAddress(),
                order.getDeliveryMessage(),
                items,
                order.getProductAmount(),
                order.getShippingFee(),
                order.getPromotionDiscountAmount(),
                order.getCouponDiscountAmount(),
                order.getCouponNameSnapshot(),
                order.getCouponCodeSnapshot(),
                order.getCouponDiscountTypeSnapshot(),
                order.getCouponDiscountValueSnapshot(),
                order.getTotalAmount(),
                payment,
                delivery
        );
    }

    /** Built from the OrderItem snapshot (not the live Product name) so it stays stable for the Toss checkout widget. */
    private static String buildOrderName(List<OrderItemResponse> items) {
        if (items.isEmpty()) {
            return "";
        }
        if (items.size() == 1) {
            return items.get(0).productName();
        }
        return items.get(0).productName() + " 외 " + (items.size() - 1) + "건";
    }
}
