package studio.aroundhub.tunagiftset.payment;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import studio.aroundhub.tunagiftset.entity.Brand;
import studio.aroundhub.tunagiftset.entity.Cart;
import studio.aroundhub.tunagiftset.entity.CartItem;
import studio.aroundhub.tunagiftset.entity.Category;
import studio.aroundhub.tunagiftset.entity.Member;
import studio.aroundhub.tunagiftset.entity.Order;
import studio.aroundhub.tunagiftset.entity.Payment;
import studio.aroundhub.tunagiftset.entity.Product;
import studio.aroundhub.tunagiftset.entity.type.OrderStatus;
import studio.aroundhub.tunagiftset.entity.type.PaymentStatus;
import studio.aroundhub.tunagiftset.entity.type.ProductStatus;
import studio.aroundhub.tunagiftset.order.dto.OrderCreateRequest;
import studio.aroundhub.tunagiftset.order.service.OrderService;
import studio.aroundhub.tunagiftset.payment.scheduler.OrderExpirationScheduler;
import studio.aroundhub.tunagiftset.repository.BrandRepository;
import studio.aroundhub.tunagiftset.repository.CartItemRepository;
import studio.aroundhub.tunagiftset.repository.CartRepository;
import studio.aroundhub.tunagiftset.repository.CategoryRepository;
import studio.aroundhub.tunagiftset.repository.DeliveryRepository;
import studio.aroundhub.tunagiftset.repository.InventoryHistoryRepository;
import studio.aroundhub.tunagiftset.repository.MemberRepository;
import studio.aroundhub.tunagiftset.repository.OrderItemRepository;
import studio.aroundhub.tunagiftset.repository.OrderRepository;
import studio.aroundhub.tunagiftset.repository.PaymentRepository;
import studio.aroundhub.tunagiftset.repository.ProductImageRepository;
import studio.aroundhub.tunagiftset.repository.ProductRepository;

/**
 * Enables the scheduler only for this test class (its own Spring context) so the rest of the
 * suite never has it running in the background. The scheduler's business method is called
 * directly rather than waiting on {@code @Scheduled} timing, for determinism and speed.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "payment.expiration.scheduler.enabled=true")
class PaymentExpirationSchedulerTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderExpirationScheduler scheduler;

    @Autowired
    private Clock clock;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private InventoryHistoryRepository inventoryHistoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Member member;
    private Brand brand;
    private Category category;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        deliveryRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        productImageRepository.deleteAll();
        inventoryHistoryRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        brandRepository.deleteAll();
        memberRepository.deleteAll();

        member = memberRepository.save(new Member(
                "expire-user@example.com",
                passwordEncoder.encode("SamplePassword123!"),
                "SAMPLE USER",
                "01012345678"
        ));
        brand = brandRepository.save(new Brand("EXPIRE-SAMPLE", "EXPIRE SAMPLE BRAND", true));
        category = categoryRepository.save(new Category(null, "EXPIRE SAMPLE CATEGORY", 1, true));
    }

    @Test
    void expiresOverdueOrderAndRestoresStock() {
        Product product = saveProduct(10, BigDecimal.valueOf(10000));
        String orderNumber = createOrder(product, 4).orderNumber();
        setExpiresAtInPast(orderNumber);

        scheduler.expirePendingOrders();

        Order order = orderRepository.findByOrderNumber(orderNumber).orElseThrow();
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.EXPIRED);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQuantity()).isEqualTo(10);
    }

    @Test
    void doesNotTouchOrdersThatHaveNotExpiredYet() {
        Product product = saveProduct(10, BigDecimal.valueOf(10000));
        String orderNumber = createOrder(product, 2).orderNumber();

        scheduler.expirePendingOrders();

        Order order = orderRepository.findByOrderNumber(orderNumber).orElseThrow();
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQuantity()).isEqualTo(8);
    }

    @Test
    void runningTheSweepTwiceDoesNotDoubleRestoreStock() {
        Product product = saveProduct(10, BigDecimal.valueOf(10000));
        String orderNumber = createOrder(product, 3).orderNumber();
        setExpiresAtInPast(orderNumber);

        scheduler.expirePendingOrders();
        scheduler.expirePendingOrders();

        assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQuantity()).isEqualTo(10);
    }

    @Test
    void doesNotExpireAnOrderWhoseConfirmationIsInFlight() {
        Product product = saveProduct(10, BigDecimal.valueOf(10000));
        String orderNumber = createOrder(product, 2).orderNumber();
        Order order = orderRepository.findByOrderNumber(orderNumber).orElseThrow();
        Payment payment = paymentRepository.findByOrderId(order.getId()).orElseThrow();
        payment.startConfirm("in_flight_payment_key");
        paymentRepository.save(payment);
        setExpiresAtInPast(orderNumber);

        scheduler.expirePendingOrders();

        Order stillPending = orderRepository.findByOrderNumber(orderNumber).orElseThrow();
        assertThat(stillPending.getOrderStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQuantity()).isEqualTo(8);
    }

    @Test
    void doesNotExpireAnAlreadyPaidOrderEvenIfExpiresAtHasPassed() {
        Product product = saveProduct(10, BigDecimal.valueOf(10000));
        String orderNumber = createOrder(product, 1).orderNumber();
        Order order = orderRepository.findByOrderNumber(orderNumber).orElseThrow();
        order.markPaid();
        orderRepository.save(order);
        Payment payment = paymentRepository.findByOrderId(order.getId()).orElseThrow();
        payment.startConfirm("paid_before_expiry_key");
        payment.markPaid("paid_before_expiry_key", null, BigDecimal.valueOf(10000), "KRW", clock.instant());
        paymentRepository.save(payment);
        setExpiresAtInPast(orderNumber);

        scheduler.expirePendingOrders();

        Order stillPaid = orderRepository.findByOrderNumber(orderNumber).orElseThrow();
        assertThat(stillPaid.getOrderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStockQuantity()).isEqualTo(9);
    }

    private Product saveProduct(int stockQuantity, BigDecimal salePrice) {
        return productRepository.save(new Product(
                brand,
                category,
                "EXPIRE-SAMPLE-" + System.nanoTime(),
                "SAMPLE EXPIRE PRODUCT",
                "SAMPLE",
                "SAMPLE EXPIRE PRODUCT DATA",
                salePrice,
                salePrice,
                stockQuantity,
                ProductStatus.ACTIVE,
                false
        ));
    }

    private studio.aroundhub.tunagiftset.order.dto.OrderDetailResponse createOrder(Product product, int quantity) {
        Cart cart = cartRepository.findByMemberId(member.getId())
                .orElseGet(() -> cartRepository.save(new Cart(member)));
        CartItem cartItem = cartItemRepository.save(new CartItem(cart, product, quantity));
        return orderService.createOrder(member.getId(), new OrderCreateRequest(
                List.of(cartItem.getId()),
                "SAMPLE RECIPIENT",
                "01012345678",
                "12345",
                "SEOUL SAMPLE ADDRESS",
                "101",
                "LEAVE AT DOOR"
        ));
    }

    private void setExpiresAtInPast(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber).orElseThrow();
        ReflectionTestUtils.setField(order, "expiresAt", clock.instant().minusSeconds(60));
        orderRepository.save(order);
    }
}
