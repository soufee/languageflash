package ci.ashamaz.languageflash.controller;

import ci.ashamaz.languageflash.dto.AuthDtos.*;
import ci.ashamaz.languageflash.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> register(@Valid @RequestBody RegisterRequest request) {
        UserDto user = authService.register(request);
        return Map.of("user", user, "message", "Письмо с подтверждением отправлено на " + user.email());
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/confirm-email")
    public Map<String, Object> confirmEmail(@Valid @RequestBody ConfirmEmailRequest request) {
        authService.confirmEmail(request);
        return Map.of("confirmed", true);
    }

    @PostMapping("/reset-password/request")
    public Map<String, Object> requestReset(@Valid @RequestBody ResetPasswordRequest request) {
        authService.requestPasswordReset(request);
        return Map.of("message", "Если email зарегистрирован, на него отправлен код сброса");
    }

    @PostMapping("/reset-password/confirm")
    public Map<String, Object> confirmReset(@Valid @RequestBody ResetPasswordConfirmRequest request) {
        authService.confirmPasswordReset(request);
        return Map.of("message", "Пароль успешно изменён");
    }

    @PostMapping("/oauth2")
    public AuthResponse oauth2(@Valid @RequestBody OAuth2Request request) {
        return authService.oauth2(request);
    }
}
