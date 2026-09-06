package studio.aroundhub.tunagiftset.admin.dashboard.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import studio.aroundhub.tunagiftset.admin.dashboard.dto.AdminDashboardSummaryResponse;
import studio.aroundhub.tunagiftset.admin.dashboard.service.AdminDashboardService;

/** Gated to ROLE_ADMIN by SecurityConfig's /api/admin/** rule. */
@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/summary")
    public AdminDashboardSummaryResponse getSummary() {
        return adminDashboardService.getSummary();
    }
}
