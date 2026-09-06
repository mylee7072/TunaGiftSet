package studio.aroundhub.tunagiftset.payment.controller;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import studio.aroundhub.tunagiftset.payment.dto.PaymentConfirmRequest;
import studio.aroundhub.tunagiftset.payment.dto.PaymentConfirmResponse;
import studio.aroundhub.tunagiftset.payment.service.PaymentService;
import studio.aroundhub.tunagiftset.security.AuthMember;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/confirm")
    public PaymentConfirmResponse confirm(
            @AuthenticationPrincipal AuthMember authMember,
            @Valid @RequestBody PaymentConfirmRequest request
    ) {
        return paymentService.confirm(authMember.id(), request);
    }
}
