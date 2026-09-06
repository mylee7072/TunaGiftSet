package studio.aroundhub.tunagiftset.admin.dashboard.dto;

import java.math.BigDecimal;

public record AdminDashboardSummaryResponse(
        long todayOrderCount,
        BigDecimal todayPaidAmount,
        long preparingOrderCount,
        long shippingOrderCount,
        long lowStockProductCount
) {
}
