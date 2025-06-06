package ci.ashamaz.languageflash.controller;

import ci.ashamaz.languageflash.model.User;
import ci.ashamaz.languageflash.service.DashboardService;
import ci.ashamaz.languageflash.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/dashboard")
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model, HttpSession session) {
        log.info("Handling GET /dashboard");
        model.addAllAttributes(dashboardService.getDashboardData(userDetails.getUsername()));
        return "dashboard";
    }
}