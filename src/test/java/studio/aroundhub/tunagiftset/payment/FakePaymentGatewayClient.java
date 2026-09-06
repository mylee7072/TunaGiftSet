package studio.aroundhub.tunagiftset.payment;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Function;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import studio.aroundhub.tunagiftset.entity.type.PaymentMethod;
import studio.aroundhub.tunagiftset.payment.client.PaymentGatewayClient;
import studio.aroundhub.tunagiftset.payment.client.PaymentProviderException;
import studio.aroundhub.tunagiftset.payment.client.TossPaymentCancelCommand;
import studio.aroundhub.tunagiftset.payment.client.TossPaymentConfirmCommand;
import studio.aroundhub.tunagiftset.payment.client.TossPaymentResult;

/**
 * Test-only replacement for the real Toss client (never hits the network). Queue up canned
 * confirm/cancel behaviors with {@link #queueConfirm} / {@link #queueCancel}; each call consumes
 * one queued behavior (defaults to a generic success echoing the command back) so tests can script
 * success, failure, and timeout scenarios per-call.
 */
@Primary
@Component
public class FakePaymentGatewayClient implements PaymentGatewayClient {

    private final Clock clock;
    private final Deque<Function<TossPaymentConfirmCommand, TossPaymentResult>> confirmBehaviors = new ArrayDeque<>();
    private final Deque<Function<TossPaymentCancelCommand, TossPaymentResult>> cancelBehaviors = new ArrayDeque<>();
    private int confirmCallCount = 0;
    private int cancelCallCount = 0;

    public FakePaymentGatewayClient(Clock clock) {
        this.clock = clock;
    }

    public void reset() {
        confirmBehaviors.clear();
        cancelBehaviors.clear();
        confirmCallCount = 0;
        cancelCallCount = 0;
    }

    public void queueConfirm(Function<TossPaymentConfirmCommand, TossPaymentResult> behavior) {
        confirmBehaviors.addLast(behavior);
    }

    public void queueConfirmFailure(String providerCode, boolean timeout) {
        queueConfirm(command -> {
            throw new PaymentProviderException(providerCode, "fake failure: " + providerCode, timeout);
        });
    }

    public void queueCancel(Function<TossPaymentCancelCommand, TossPaymentResult> behavior) {
        cancelBehaviors.addLast(behavior);
    }

    public void queueCancelFailure(String providerCode, boolean timeout) {
        queueCancel(command -> {
            throw new PaymentProviderException(providerCode, "fake failure: " + providerCode, timeout);
        });
    }

    public int confirmCallCount() {
        return confirmCallCount;
    }

    public int cancelCallCount() {
        return cancelCallCount;
    }

    @Override
    public TossPaymentResult confirm(TossPaymentConfirmCommand command) {
        confirmCallCount++;
        Function<TossPaymentConfirmCommand, TossPaymentResult> behavior = confirmBehaviors.pollFirst();
        if (behavior == null) {
            behavior = this::defaultConfirmSuccess;
        }
        return behavior.apply(command);
    }

    @Override
    public TossPaymentResult cancel(TossPaymentCancelCommand command) {
        cancelCallCount++;
        Function<TossPaymentCancelCommand, TossPaymentResult> behavior = cancelBehaviors.pollFirst();
        if (behavior == null) {
            behavior = this::defaultCancelSuccess;
        }
        return behavior.apply(command);
    }

    private TossPaymentResult defaultConfirmSuccess(TossPaymentConfirmCommand command) {
        return new TossPaymentResult(
                command.paymentKey(),
                command.orderId(),
                command.amount(),
                "KRW",
                PaymentMethod.CARD,
                "DONE",
                clock.instant(),
                null
        );
    }

    private TossPaymentResult defaultCancelSuccess(TossPaymentCancelCommand command) {
        return new TossPaymentResult(
                command.paymentKey(),
                null,
                BigDecimal.ZERO,
                "KRW",
                PaymentMethod.CARD,
                "CANCELED",
                null,
                clock.instant()
        );
    }
}
