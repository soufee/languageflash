package ci.ashamaz.languageflash.controller;

import ci.ashamaz.languageflash.model.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpSession;

@Controller
public class DashboardController {

    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/"; // Перенаправляем на главную, если пользователь не авторизован
        }
        model.addAttribute("user", user);
        return "dashboard";
    }
}