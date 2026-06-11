package ci.ashamaz.languageflash.controller;

import ci.ashamaz.languageflash.dto.AuthDtos.UserDto;
import ci.ashamaz.languageflash.exception.ApiException;
import ci.ashamaz.languageflash.security.UserPrincipal;
import ci.ashamaz.languageflash.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    public record UpdateProfileRequest(@Size(max = 100) String firstName,
                                       @Size(max = 100) String lastName,
                                       String interfaceLanguage) {}

    public record ChangePasswordRequest(@NotBlank String oldPassword,
                                        @NotBlank @Size(min = 8, max = 100) String newPassword) {}

    public record DeleteAccountRequest(@NotBlank String confirmation) {}

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal UserPrincipal principal) {
        return UserDto.from(userService.getById(principal.id()));
    }

    @PatchMapping("/me")
    public UserDto updateProfile(@AuthenticationPrincipal UserPrincipal principal,
                                 @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(principal.id(),
                request.firstName(), request.lastName(), request.interfaceLanguage());
    }

    @PutMapping("/me/password")
    public Map<String, Object> changePassword(@AuthenticationPrincipal UserPrincipal principal,
                                              @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(principal.id(), request.oldPassword(), request.newPassword());
        return Map.of("message", "Пароль изменён");
    }

    @DeleteMapping("/me")
    public Map<String, Object> deleteAccount(@AuthenticationPrincipal UserPrincipal principal,
                                             @Valid @RequestBody DeleteAccountRequest request) {
        if (!"DELETE".equals(request.confirmation())) {
            throw ApiException.badRequest("Для удаления аккаунта передайте confirmation=DELETE");
        }
        userService.deleteAccount(principal.id());
        return Map.of("message", "Аккаунт и все персональные данные удалены");
    }

    @GetMapping("/me/settings")
    public Map<String, Object> getSettings(@AuthenticationPrincipal UserPrincipal principal) {
        return userService.getSettings(principal.id());
    }

    @PutMapping("/me/settings")
    public Map<String, Object> updateSettings(@AuthenticationPrincipal UserPrincipal principal,
                                              @RequestBody Map<String, Object> updates) {
        return userService.updateSettings(principal.id(), updates);
    }
}
