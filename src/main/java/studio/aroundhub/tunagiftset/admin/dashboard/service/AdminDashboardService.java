package studio.aroundhub.tunagiftset.admin.dashboard.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studio.aroundhub.tunagiftset.admin.dashboard.dto.AdminDashboardSummaryResponse;
import studio.aroundhub.tunagiftset.entity.type.OrderStatus;
import studio.aroundhub.tunagiftset.entity.type.ProductStatus;
import studio.aroundhub.tunagiftset.repository.OrderRepository;
import studio.aroundhub.tunagiftset.repository.PaymentRepository;
import studio.aroundhub.tunagiftset.repository.ProductRepository;

@Service
@Transactional(readOnly = true)
public class AdminDashboardService {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
    private static final int LOW_STOCK_THRESHOLD = 5;

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;
    private final Clock clock;

    public AdminDashboardService(
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            ProductRepository productRepository,
            Clock clock
    ) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.productRepository = productRepository;
        this.clock = clock;
    }

    public AdminDashboardSummaryResponse getSummary() {
        // "Today" is a KST calendar day regardless of the server's own OS time zone — this is a
        // Korean storefront's admin view, so "오늘" should mean the Korean calendar day.
        LocalDate today = clock.instant().atZone(SEOUL_ZONE).toLocalDate();
        Instant todayStart = today.atStartOfDay(SEOUL_ZONE).toInstant();
        Instant todayEnd = today.plusDays(1).atStartOfDay(SEOUL_ZONE).toInstant();

        long todayOrderCount = orderRepository.countByOrderedAtBetween(todayStart, todayEnd);
        // Revenue counts only approved (PAID) payments — pending/canceled orders are never revenue.
        BigDecimal todayPaidAmount = paymentRepository.sumApprovedAmountByPaidAtBetween(todayStart, todayEnd);
        long preparingOrderCount = orderRepository.countByOrderStatus(OrderStatus.PREPARING);
        long shippingOrderCount = orderRepository.countByOrderStatus(OrderStatus.SHIPPING);
        long lowStockProductCount = productRepository.countByStatusAndStockQuantityLessThanEqual(ProductStatus.ACTIVE, LOW_STOCK_THRESHOLD);

        return new AdminDashboardSummaryResponse(
                todayOrderCount, todayPaidAmount, preparingOrderCount, shippingOrderCount, lowStockProductCount
        );
    }
}
