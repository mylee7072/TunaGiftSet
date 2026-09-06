package studio.aroundhub.tunagiftset.order.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import studio.aroundhub.tunagiftset.order.dto.OrderCancelResponse;
import studio.aroundhub.tunagiftset.order.dto.OrderCreateRequest;
import studio.aroundhub.tunagiftset.order.dto.OrderDetailResponse;
import studio.aroundhub.tunagiftset.order.dto.OrderPageResponse;
import studio.aroundhub.tunagiftset.order.service.OrderService;
import studio.aroundhub.tunagiftset.security.AuthMember;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public OrderDetailResponse createOrder(
            @AuthenticationPrincipal AuthMember authMember,
            @Valid @RequestBody OrderCreateRequest request
    ) {
        return orderService.createOrder(authMember.id(), request);
    }

    @GetMapping
    public OrderPageResponse findOrders(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return orderService.findOrders(authMember.id(), page, size);
    }

    @GetMapping("/{orderNumber}")
    public OrderDetailResponse findOrder(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable String orderNumber
    ) {
        return orderService.findOrder(authMember.id(), orderNumber);
    }

    @PostMapping("/{orderNumber}/cancel")
    public OrderCancelResponse cancel(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable String orderNumber
    ) {
        return orderService.cancel(authMember.id(), orderNumber);
    }
}
