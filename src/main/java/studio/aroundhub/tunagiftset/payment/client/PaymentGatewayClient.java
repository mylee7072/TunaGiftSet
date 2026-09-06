package studio.aroundhub.tunagiftset.payment.client;

public interface PaymentGatewayClient {

    TossPaymentResult confirm(TossPaymentConfirmCommand command);

    TossPaymentResult cancel(TossPaymentCancelCommand command);
}
