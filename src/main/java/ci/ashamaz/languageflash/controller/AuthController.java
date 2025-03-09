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
    public String showRegistrationForm(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("registerRequest") RegisterRequest request, Model model) {
        try {
            userService.registerUser(request);
            model.addAttribute("message", "Регистрация успешна! Проверьте email для подтверждения.");
            return "index"; // Перенаправляем на главную страницу
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
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
                model.addAttribute("loginError", "Подтвердите email перед входом");
                return "index";
            }
            String token = jwtUtil.generateToken(email);
            session.setAttribute("token", token);
            session.setAttribute("user", user);
            return "redirect:/dashboard";
        } else {
            model.addAttribute("loginError", "Неверный email или пароль");
            model.addAttribute("message", "Добро пожаловать в Language Flash!");
            return "index";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(Model model) {
        return "reset-password"; // Должно соответствовать имени файла reset-password.html
    }

    @PostMapping("/reset-password/request")
    public String requestResetPassword(@RequestParam("email") String email, Model model) {
        try {
            userService.sendResetCode(email);
            Optional<User> userOptional = userService.findByEmail(email);
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                model.addAttribute("email", email);
                model.addAttribute("expiryTime", user.getResetCodeExpiry()); // Передаём время истечения
                model.addAttribute("message", "Код отправлен на ваш email");
            }
            return "reset-password-code";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "reset-password";
        }
    }

    @PostMapping("/reset-password/verify")
    public String verifyResetCode(@RequestParam("email") String email,
                                  @RequestParam("code") String code,
                                  @RequestParam("newPassword") String newPassword,
                                  Model model) {
        try {
            userService.resetPassword(email, code, newPassword);
            model.addAttribute("message", "Пароль успешно изменён. Войдите с новым паролем.");
            return "index";
        } catch (IllegalArgumentException e) {
            model.addAttribute("email", email);
            model.addAttribute("error", e.getMessage());
            return "reset-password-code";
        }
    }

    @GetMapping("/confirm-email")
    public String confirmEmail(@RequestParam("email") String email,
                               @RequestParam("code") String code,
                               Model model) {
        if (userService.confirmEmail(email, code)) {
            model.addAttribute("message", "Email успешно подтверждён. Теперь вы можете войти.");
            return "index";
        } else {
            model.addAttribute("error", "Неверный или просроченный код подтверждения");
            return "index";
        }
    }
}