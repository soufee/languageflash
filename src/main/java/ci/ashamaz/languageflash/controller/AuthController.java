package ci.ashamaz.languageflash.controller;

import ci.ashamaz.languageflash.dto.RegisterRequest;
import ci.ashamaz.languageflash.model.User;
import ci.ashamaz.languageflash.service.UserService;
import ci.ashamaz.languageflash.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.Optional;

@Controller
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/register")
    public String showRegistrationForm(Model model, HttpSession session) {
        model.addAttribute("registerRequest", new RegisterRequest());
        addAuthAttributes(session, model); // Передаём уведомления
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("registerRequest") RegisterRequest request, Model model, HttpSession session) {
        try {
            userService.registerUser(request);
            session.setAttribute("message", "Регистрация успешна! Проверьте email для подтверждения.");
            return "redirect:/";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            addAuthAttributes(session, model); // Передаём уведомления даже при ошибке
            return "register";
        }
    }

    @PostMapping("/login")
    public String login(@RequestParam("email") String email,
                        @RequestParam("password") String password,
                        Model model,
                        HttpSession session) {
        Optional<User> userOptional = userService.findByEmail(email);
        if (userOptional.isPresent() && userService.checkPassword(userOptional.get(), password)) {
            User user = userOptional.get();
            if (!user.isEmailConfirmed()) {
                session.setAttribute("loginError", "Подтвердите email перед входом");
                return "redirect:/";
            }
            String token = jwtUtil.generateToken(email, user.getRoles());
            session.setAttribute("token", token);
            session.setAttribute("user", user);
            return "redirect:/dashboard";
        } else {
            session.setAttribute("loginError", "Неверный email или пароль");
            return "redirect:/";
        }
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
            addAuthAttributes(session, model); // Передаём уведомления
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
            addAuthAttributes(session, model); // Передаём уведомления
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