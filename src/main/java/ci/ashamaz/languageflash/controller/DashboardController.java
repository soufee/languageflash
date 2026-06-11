package ci.ashamaz.languageflash.controller;

import ci.ashamaz.languageflash.security.UserPrincipal;
import ci.ashamaz.languageflash.service.DashboardService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public DashboardService.DashboardStats stats(@AuthenticationPrincipal UserPrincipal principal,
                                                 @RequestParam(defaultValue = "7") int days) {
        return dashboardService.stats(principal.id(), days);
    }
}
