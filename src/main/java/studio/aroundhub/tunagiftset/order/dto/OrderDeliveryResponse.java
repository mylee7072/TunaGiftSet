package studio.aroundhub.tunagiftset.order.dto;

import java.time.Instant;
import studio.aroundhub.tunagiftset.entity.Delivery;
import studio.aroundhub.tunagiftset.entity.type.DeliveryStatus;

public record OrderDeliveryResponse(
        DeliveryStatus status,
        String carrier,
        String trackingNumber,
        Instant shippedAt,
        Instant deliveredAt
) {
    public static OrderDeliveryResponse from(Delivery delivery) {
        if (delivery == null) {
            return null;
        }
        return new OrderDeliveryResponse(
                delivery.getStatus(),
                delivery.getCarrier(),
                delivery.getTrackingNumber(),
                delivery.getShippedAt(),
                delivery.getDeliveredAt()
        );
    }
}
