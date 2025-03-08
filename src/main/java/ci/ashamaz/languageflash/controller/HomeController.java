package ci.ashamaz.languageflash.controller;

import ci.ashamaz.languageflash.model.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpSession;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model, HttpSession session) {
        model.addAttribute("message", "Добро пожаловать в Language Flash!");
        User user = (User) session.getAttribute("user");
        model.addAttribute("user", user);
        return "index";
    }
}