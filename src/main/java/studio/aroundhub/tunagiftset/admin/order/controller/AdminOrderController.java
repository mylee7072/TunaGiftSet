package studio.aroundhub.tunagiftset.admin.order.controller;

import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import studio.aroundhub.tunagiftset.admin.order.dto.AdminDeliveryUpdateRequest;
import studio.aroundhub.tunagiftset.admin.order.dto.AdminOrderCancelRequest;
import studio.aroundhub.tunagiftset.admin.order.dto.AdminOrderDetailResponse;
import studio.aroundhub.tunagiftset.admin.order.dto.AdminOrderPageResponse;
import studio.aroundhub.tunagiftset.admin.order.service.AdminOrderService;
import studio.aroundhub.tunagiftset.entity.type.DeliveryStatus;
import studio.aroundhub.tunagiftset.entity.type.OrderStatus;
import studio.aroundhub.tunagiftset.entity.type.PaymentStatus;
import studio.aroundhub.tunagiftset.order.dto.OrderCancelResponse;
import studio.aroundhub.tunagiftset.security.AuthMember;

/** Every endpoint here is gated to ROLE_ADMIN by SecurityConfig's /api/admin/** rule. */
@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    public AdminOrderController(AdminOrderService adminOrderService) {
        this.adminOrderService = adminOrderService;
    }

    @GetMapping
    public AdminOrderPageResponse findOrders(
            @RequestParam(required = false) String orderNumber,
            @RequestParam(required = false) String memberEmail,
            @RequestParam(required = false) String recipientName,
            @RequestParam(required = false) String recipientPhone,
            @RequestParam(required = false) OrderStatus orderStatus,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(required = false) DeliveryStatus deliveryStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return adminOrderService.findOrders(
                orderNumber, memberEmail, recipientName, recipientPhone,
                orderStatus, paymentStatus, deliveryStatus, startDate, endDate, page, size
        );
    }

    @GetMapping("/{orderNumber}")
    public AdminOrderDetailResponse findOrder(@PathVariable String orderNumber) {
        return adminOrderService.findOrder(orderNumber);
    }

    @PostMapping("/{orderNumber}/prepare")
    public AdminOrderDetailResponse prepare(@PathVariable String orderNumber) {
        return adminOrderService.prepare(orderNumber);
    }

    @PutMapping("/{orderNumber}/delivery")
    public AdminOrderDetailResponse registerDelivery(
            @PathVariable String orderNumber,
            @Valid @RequestBody AdminDeliveryUpdateRequest request
    ) {
        return adminOrderService.registerDelivery(orderNumber, request);
    }

    @PostMapping("/{orderNumber}/ship")
    public AdminOrderDetailResponse ship(@PathVariable String orderNumber) {
        return adminOrderService.ship(orderNumber);
    }

    @PostMapping("/{orderNumber}/deliver")
    public AdminOrderDetailResponse deliver(@PathVariable String orderNumber) {
        return adminOrderService.deliver(orderNumber);
    }

    @PostMapping("/{orderNumber}/cancel")
    public OrderCancelResponse cancel(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable String orderNumber,
            @Valid @RequestBody(required = false) AdminOrderCancelRequest request
    ) {
        String reason = request == null ? null : request.reason();
        return adminOrderService.cancel(orderNumber, authMember.id(), reason);
    }
}
