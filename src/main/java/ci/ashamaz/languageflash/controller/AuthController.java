package ci.ashamaz.languageflash.controller;

import ci.ashamaz.languageflash.dto.RegisterRequest;
import ci.ashamaz.languageflash.model.User;
import ci.ashamaz.languageflash.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.Optional;

@Controller
@RequestMapping("/auth")
@Slf4j
public class AuthController {

    @Autowired
    private UserService userService;

    @GetMapping("/register")
    public String showRegistrationForm(Model model, HttpSession session) {
        model.addAttribute("registerRequest", new RegisterRequest());
        addAuthAttributes(session, model);
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("registerRequest") RegisterRequest request,
                               Model model, HttpSession session) {
        try {
            userService.registerUser(request);
            session.setAttribute("message", "Регистрация успешна! Проверьте email для подтверждения.");
            return "redirect:/";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            addAuthAttributes(session, model);
            return "register";
        }
    }

    @GetMapping("/login")
    public String showLoginForm(HttpSession session, Model model) {
        // Получаем и добавляем информацию об ошибке аутентификации, если есть
        AuthenticationException exception = (AuthenticationException) session
                .getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        if (exception != null) {
            String errorMessage = exception.getMessage();
            model.addAttribute("loginError", errorMessage);
            session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        }

        addAuthAttributes(session, model);
        return "login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(HttpSession session, Model model) {
        addAuthAttributes(session, model);
        return "reset-password";
    }

    @PostMapping("/reset-password/request")
    public String requestResetPassword(@RequestParam("email") String email, Model model, HttpSession session) {
        try {
            userService.sendResetCode(email);
            model.addAttribute("email", email);
            model.addAttribute("message", "Код отправлен на ваш email");
            Optional<User> userOptional = userService.findByEmail(email);
            userOptional.ifPresent(user -> model.addAttribute("expiryTime", user.getResetCodeExpiry()));
            return "reset-password-code";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            addAuthAttributes(session, model);
            return "reset-password";
        }
    }

    @PostMapping("/reset-password/verify")
    public String verifyResetCode(@RequestParam("email") String email,
                                  @RequestParam("code") String code,
                                  @RequestParam("newPassword") String newPassword,
                                  Model model,
                                  HttpSession session) {
        try {
            userService.resetPassword(email, code, newPassword);
            session.setAttribute("message", "Пароль успешно изменён. Войдите с новым паролем.");
            return "redirect:/";
        } catch (IllegalArgumentException e) {
            model.addAttribute("email", email);
            model.addAttribute("error", e.getMessage());
            addAuthAttributes(session, model);
            return "reset-password-code";
        }
    }

    @GetMapping("/confirm-email")
    public String confirmEmail(@RequestParam("email") String email,
                               @RequestParam("code") String code,
                               Model model,
                               HttpSession session) {
        if (userService.confirmEmail(email, code)) {
            session.setAttribute("message", "Email успешно подтверждён. Теперь вы можете войти.");
            return "redirect:/";
        } else {
            session.setAttribute("error", "Неверный или просроченный код подтверждения");
            return "redirect:/";
        }
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