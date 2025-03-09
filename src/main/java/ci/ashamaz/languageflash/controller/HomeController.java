package ci.ashamaz.languageflash.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpSession;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(HttpSession session, Model model) {
        addAuthAttributes(session, model);
        return "index";
    }

    @GetMapping("/about")
    public String about(HttpSession session, Model model) {
        addAuthAttributes(session, model);
        return "about";
    }

    @GetMapping("/method")
    public String method(HttpSession session, Model model) {
        addAuthAttributes(session, model);
        return "method";
    }

    @GetMapping("/contacts")
    public String contacts(HttpSession session, Model model) {
        addAuthAttributes(session, model);
        return "contacts";
    }

    @GetMapping("/blog")
    public String blog(HttpSession session, Model model) {
        addAuthAttributes(session, model);
        return "blog";
    }

    private void addAuthAttributes(HttpSession session, Model model) {
        Object loginError = session.getAttribute("loginError");
        if (loginError != null) {
            model.addAttribute("loginError", loginError);
            session.removeAttribute("loginError");
        }
        Object message = session.getAttribute("message");
        if (message != null) {
            model.addAttribute("message", message);
            session.removeAttribute("message");
        }
    }
}
