package ci.ashamaz.languageflash.controller;

import org.springframework.ui.Model;

import javax.servlet.http.HttpSession;

public class CommonControllerUtil {
    protected void addAuthAttributes(HttpSession session, Model model) {
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
