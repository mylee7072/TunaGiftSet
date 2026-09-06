package studio.aroundhub.tunagiftset.payment.client;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriUtils;
import studio.aroundhub.tunagiftset.entity.type.PaymentMethod;
import studio.aroundhub.tunagiftset.payment.config.TossPaymentProperties;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Talks to the Toss Payments REST API (docs.tosspayments.com/reference). Auth is HTTP Basic
 * with the secret key as the username and an empty password; every mutating call carries a
 * caller-supplied Idempotency-Key so retried confirm/cancel requests are deduplicated by Toss.
 */
@Component
public class TossPaymentGatewayClient implements PaymentGatewayClient {

    private static final Logger log = LoggerFactory.getLogger(TossPaymentGatewayClient.class);
    private static final String CONFIRM_PATH = "/v1/payments/confirm";
    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private final RestClient restClient;
    private final TossPaymentProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TossPaymentGatewayClient(TossPaymentProperties properties) {
        this.properties = properties;

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.connectTimeoutMillis()))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(properties.readTimeoutMillis()));

        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public TossPaymentResult confirm(TossPaymentConfirmCommand command) {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("paymentKey", command.paymentKey());
        requestBody.put("orderId", command.orderId());
        requestBody.put("amount", command.amount());
        return execute(CONFIRM_PATH, requestBody, command.idempotencyKey(), "confirm");
    }

    @Override
    public TossPaymentResult cancel(TossPaymentCancelCommand command) {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("cancelReason", command.cancelReason());
        if (command.cancelAmount() != null) {
            requestBody.put("cancelAmount", command.cancelAmount());
        }
        String encodedPaymentKey = UriUtils.encodePathSegment(command.paymentKey(), StandardCharsets.UTF_8);
        String path = "/v1/payments/" + encodedPaymentKey + "/cancel";
        return execute(path, requestBody, command.idempotencyKey(), "cancel");
    }

    private TossPaymentResult execute(String path, Map<String, Object> requestBody, String idempotencyKey, String operation) {
        requireSecretKey();
        String responseBody;
        try {
            responseBody = restClient.post()
                    .uri(path)
                    .header(HttpHeaders.AUTHORIZATION, basicAuthHeader())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header(IDEMPOTENCY_HEADER, idempotencyKey)
                    .body(writeJson(requestBody))
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException exception) {
            String providerCode = readErrorCode(exception.getResponseBodyAsString());
            log.warn("Toss {} request rejected. httpStatus={} providerCode={}",
                    operation, exception.getStatusCode().value(), providerCode);
            throw new PaymentProviderException(providerCode, "Toss " + operation + " request was rejected.", false);
        } catch (ResourceAccessException exception) {
            boolean timeout = isTimeout(exception);
            log.warn("Toss {} request did not complete. timeout={}", operation, timeout);
            throw new PaymentProviderException(
                    timeout ? "PROVIDER_TIMEOUT" : "NETWORK_ERROR",
                    "Toss " + operation + " request did not complete.",
                    timeout
            );
        }
        return parseResult(responseBody, operation);
    }

    private TossPaymentResult parseResult(String responseBody, String operation) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            BigDecimal totalAmount = root.hasNonNull("totalAmount")
                    ? new BigDecimal(root.get("totalAmount").asString())
                    : BigDecimal.ZERO;
            Instant approvedAt = parseInstant(root.path("approvedAt").asString(null));
            Instant canceledAt = null;
            JsonNode cancels = root.path("cancels");
            if (cancels.isArray() && !cancels.isEmpty()) {
                canceledAt = parseInstant(cancels.get(cancels.size() - 1).path("canceledAt").asString(null));
            }

            return new TossPaymentResult(
                    root.path("paymentKey").asString(null),
                    root.path("orderId").asString(null),
                    totalAmount,
                    root.path("currency").asString("KRW"),
                    mapMethod(root.path("method").asString(null)),
                    root.path("status").asString(null),
                    approvedAt,
                    canceledAt
            );
        } catch (Exception exception) {
            log.warn("Toss {} response could not be parsed.", operation);
            throw new PaymentProviderException("INVALID_PROVIDER_RESPONSE", "Toss response could not be parsed.", false);
        }
    }

    private void requireSecretKey() {
        if (properties.secretKey() == null || properties.secretKey().isBlank()) {
            throw new PaymentProviderException("SECRET_KEY_NOT_CONFIGURED", "Toss secret key is not configured.", false);
        }
    }

    private String basicAuthHeader() {
        String credentials = properties.secretKey() + ":";
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    private String writeJson(Map<String, Object> requestBody) {
        try {
            return objectMapper.writeValueAsString(requestBody);
        } catch (Exception exception) {
            throw new PaymentProviderException("REQUEST_SERIALIZATION_FAILED", "Toss request could not be serialized.", false);
        }
    }

    private String readErrorCode(String errorBody) {
        try {
            JsonNode node = objectMapper.readTree(errorBody);
            String code = node.path("code").asString(null);
            return code == null || code.isBlank() ? "UNKNOWN_ERROR" : code;
        } catch (Exception exception) {
            return "UNKNOWN_ERROR";
        }
    }

    private boolean isTimeout(ResourceAccessException exception) {
        Throwable cause = exception.getCause();
        while (cause != null) {
            if (cause instanceof java.net.http.HttpTimeoutException || cause instanceof java.net.SocketTimeoutException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return OffsetDateTime.parse(value).toInstant();
    }

    private PaymentMethod mapMethod(String method) {
        if (method == null) {
            return PaymentMethod.UNKNOWN;
        }
        return switch (method) {
            case "카드" -> PaymentMethod.CARD;
            case "가상계좌" -> PaymentMethod.VIRTUAL_ACCOUNT;
            case "계좌이체" -> PaymentMethod.BANK_TRANSFER;
            case "간편결제" -> PaymentMethod.EASY_PAY;
            default -> PaymentMethod.UNKNOWN;
        };
    }
}
