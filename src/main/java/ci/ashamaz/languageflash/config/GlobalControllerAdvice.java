package ci.ashamaz.languageflash.config;

import ci.ashamaz.languageflash.model.User;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import javax.servlet.http.HttpSession;

@ControllerAdvice
public class GlobalControllerAdvice {

    @ModelAttribute
    public void addUserToModel(HttpSession session, Model model) {
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser != null) {
            model.addAttribute("user", sessionUser); // Явно добавляем user в модель
        }
    }
}