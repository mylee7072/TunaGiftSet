package studio.aroundhub.tunagiftset.order.service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
public class OrderNumberGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private static final char[] ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final int RANDOM_LENGTH = 8;

    private final SecureRandom secureRandom = new SecureRandom();
    private final Clock clock;

    public OrderNumberGenerator() {
        this(Clock.systemDefaultZone());
    }

    OrderNumberGenerator(Clock clock) {
        this.clock = clock;
    }

    public String generate() {
        StringBuilder randomPart = new StringBuilder(RANDOM_LENGTH);
        for (int i = 0; i < RANDOM_LENGTH; i++) {
            randomPart.append(ALPHABET[secureRandom.nextInt(ALPHABET.length)]);
        }
        return "TG" + LocalDate.now(clock).format(DATE_FORMATTER) + "-" + randomPart;
    }
}
