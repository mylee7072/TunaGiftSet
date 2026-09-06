package studio.aroundhub.tunagiftset.admin.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import studio.aroundhub.tunagiftset.entity.Delivery;
import studio.aroundhub.tunagiftset.entity.Order;
import studio.aroundhub.tunagiftset.entity.Payment;
import studio.aroundhub.tunagiftset.entity.type.DeliveryStatus;
import studio.aroundhub.tunagiftset.entity.type.DiscountType;
import studio.aroundhub.tunagiftset.entity.type.OrderStatus;
import studio.aroundhub.tunagiftset.entity.type.PaymentMethod;
import studio.aroundhub.tunagiftset.entity.type.PaymentStatus;
import studio.aroundhub.tunagiftset.order.dto.OrderItemResponse;

public record AdminOrderDetailResponse(
        String orderNumber,
        OrderStatus orderStatus,
        Instant orderedAt,
        AdminOrderMemberResponse member,
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
        PaymentStatus paymentStatus,
        PaymentMethod paymentMethod,
        BigDecimal approvedAmount,
        Instant approvedAt,
        DeliveryStatus deliveryStatus,
        String carrier,
        String trackingNumber,
        Instant shippedAt,
        Instant deliveredAt
) {
    public static AdminOrderDetailResponse from(
            Order order,
            List<OrderItemResponse> items,
            Payment payment,
            Delivery delivery
    ) {
        return new AdminOrderDetailResponse(
                order.getOrderNumber(),
                order.getOrderStatus(),
                order.getOrderedAt(),
                AdminOrderMemberResponse.from(order.getMember()),
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
                payment == null ? null : payment.getStatus(),
                payment == null ? null : payment.getPaymentMethod(),
                payment == null ? null : payment.getApprovedAmount(),
                payment == null ? null : payment.getPaidAt(),
                delivery == null ? null : delivery.getStatus(),
                delivery == null ? null : delivery.getCarrier(),
                delivery == null ? null : delivery.getTrackingNumber(),
                delivery == null ? null : delivery.getShippedAt(),
                delivery == null ? null : delivery.getDeliveredAt()
        );
    }
}
