package ci.ashamaz.languageflash.dto;

import ci.ashamaz.languageflash.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 100) String password,
            @Size(max = 100) String firstName,
            @Size(max = 100) String lastName) {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record ConfirmEmailRequest(@NotBlank @Email String email, @NotBlank String code) {}

    public record ResetPasswordRequest(@NotBlank @Email String email) {}

    public record ResetPasswordConfirmRequest(
            @NotBlank @Email String email,
            @NotBlank String code,
            @NotBlank @Size(min = 8, max = 100) String newPassword) {}

    public record OAuth2Request(@NotBlank String provider, @NotBlank String identityToken) {}

    public record ResendConfirmationRequest(@NotBlank @Email String email) {}

    public record UserDto(Long id, String email, String firstName, String lastName,
                          boolean emailConfirmed, boolean premium, LocalDateTime premiumExpiresAt,
                          String interfaceLanguage, java.util.Set<String> roles, LocalDateTime createdAt) {
        public static UserDto from(User u) {
            return new UserDto(u.getId(), u.getEmail(), u.getFirstName(), u.getLastName(),
                    u.isEmailConfirmed(), u.hasActivePremium(), u.getPremiumExpiresAt(),
                    u.getInterfaceLanguage(), u.getRoles(), u.getCreatedAt());
        }
    }

    public record AuthResponse(String accessToken, String refreshToken, UserDto user) {}
}
