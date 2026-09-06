package studio.aroundhub.tunagiftset.payment.client;

public class PaymentProviderException extends RuntimeException {

    private final String providerCode;
    private final boolean timeout;

    public PaymentProviderException(String providerCode, String message) {
        this(providerCode, message, false);
    }

    public PaymentProviderException(String providerCode, String message, boolean timeout) {
        super(message);
        this.providerCode = providerCode;
        this.timeout = timeout;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public boolean isTimeout() {
        return timeout;
    }

    /** A transport failure has an unknown provider outcome and must use the same key on retry. */
    public boolean hasUnknownOutcome() {
        return timeout || "NETWORK_ERROR".equals(providerCode);
    }
}
