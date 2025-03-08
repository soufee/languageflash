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
            return "redirect:/";
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
            String token = jwtUtil.generateToken(email);
            session.setAttribute("token", token);
            session.setAttribute("user", userOptional.get());
            return "redirect:/dashboard";
        } else {
            model.addAttribute("loginError", "Неверный email или пароль");
            model.addAttribute("message", "Добро пожаловать в Language Flash!");
            return "index"; // Возвращаем index с ошибкой
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}