package ci.ashamaz.languageflash.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpSession;

@Controller
public class HomeController extends CommonControllerUtil {

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

}
