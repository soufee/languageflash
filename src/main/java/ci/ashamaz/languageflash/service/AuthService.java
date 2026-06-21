package ci.ashamaz.languageflash.service;

import ci.ashamaz.languageflash.dto.AuthDtos.*;
import ci.ashamaz.languageflash.exception.ApiException;
import ci.ashamaz.languageflash.model.OAuthAccount;
import ci.ashamaz.languageflash.model.User;
import ci.ashamaz.languageflash.repository.OAuthAccountRepository;
import ci.ashamaz.languageflash.repository.UserRepository;
import ci.ashamaz.languageflash.security.JwtService;
import ci.ashamaz.languageflash.service.oauth.OAuthUserInfo;
import ci.ashamaz.languageflash.service.oauth.OAuthVerifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final OAuthAccountRepository oauthRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final EmailService emailService;
    private final Map<String, OAuthVerifier> verifiers;
    private final String baseUrl;
    private final SecureRandom random = new SecureRandom();

    public AuthService(UserRepository userRepository,
                       OAuthAccountRepository oauthRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService,
                       EmailService emailService,
                       List<OAuthVerifier> verifierList,
                       @Value("${app.base-url}") String baseUrl) {
        this.userRepository = userRepository;
        this.oauthRepository = oauthRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.emailService = emailService;
        this.verifiers = verifierList.stream()
                .collect(Collectors.toMap(OAuthVerifier::provider, Function.identity()));
        this.baseUrl = baseUrl;
    }

    @Transactional
    public UserDto register(RegisterRequest request) {
        if (userRepository.findByEmail(request.email().toLowerCase()).isPresent()) {
            throw ApiException.conflict("EMAIL_TAKEN", "Email уже зарегистрирован");
        }
        User user = new User();
        user.setEmail(request.email().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setRoles(Set.of("USER"));
        String code = numericCode(6);
        user.setConfirmationCode(code);
        userRepository.save(user);

        emailService.sendConfirmation(user.getEmail(), user.getFirstName(), code, baseUrl);
        return UserDto.from(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> ApiException.unauthorized("Неверный email или пароль"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw ApiException.unauthorized("Неверный email или пароль");
        }
        if (user.isBlocked()) {
            throw ApiException.forbidden("USER_BLOCKED", "Аккаунт заблокирован");
        }
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        User user = refreshTokenService.rotate(request.refreshToken());
        if (user.isBlocked()) {
            throw ApiException.forbidden("USER_BLOCKED", "Аккаунт заблокирован");
        }
        return buildAuthResponse(user);
    }

    @Transactional
    public boolean confirmEmail(ConfirmEmailRequest request) {
        User user = userRepository.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> ApiException.notFound("Пользователь не найден"));
        if (user.getConfirmationCode() != null && user.getConfirmationCode().equals(request.code())) {
            user.setEmailConfirmed(true);
            user.setConfirmationCode(null);
            userRepository.save(user);
            return true;
        }
        throw ApiException.badRequest("Неверный код подтверждения");
    }

    @Transactional
    public void requestPasswordReset(ResetPasswordRequest request) {
        // Не раскрываем существование email — отвечаем одинаково в любом случае
        userRepository.findByEmail(request.email().toLowerCase()).ifPresent(user -> {
            String code = numericCode(6);
            user.setResetCode(code);
            user.setResetCodeExpiry(LocalDateTime.now().plusMinutes(15));
            userRepository.save(user);
            emailService.sendResetCode(user.getEmail(), user.getFirstName(), code);
        });
    }

    @Transactional
    public void confirmPasswordReset(ResetPasswordConfirmRequest request) {
        User user = userRepository.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> ApiException.badRequest("Неверный или просроченный код"));
        boolean valid = user.getResetCode() != null && user.getResetCode().equals(request.code())
                && user.getResetCodeExpiry() != null && user.getResetCodeExpiry().isAfter(LocalDateTime.now());
        if (!valid) {
            throw ApiException.badRequest("Неверный или просроченный код");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setResetCode(null);
        user.setResetCodeExpiry(null);
        userRepository.save(user);
        refreshTokenService.revokeAllForUser(user.getId());
    }

    @Transactional
    public AuthResponse oauth2(OAuth2Request request) {
        OAuthVerifier verifier = verifiers.get(request.provider().toUpperCase());
        if (verifier == null) {
            throw ApiException.badRequest("Неизвестный провайдер: " + request.provider());
        }
        if (!verifier.isConfigured()) {
            throw new ApiException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                    "PROVIDER_NOT_CONFIGURED", "Провайдер " + verifier.provider() + " не настроен");
        }
        OAuthUserInfo info = verifier.verify(request.identityToken());

        User user = oauthRepository.findByProviderAndProviderUserId(verifier.provider(), info.providerUserId())
                .map(OAuthAccount::getUser)
                .orElseGet(() -> linkOrCreateUser(verifier.provider(), info));

        if (user.isBlocked()) {
            throw ApiException.forbidden("USER_BLOCKED", "Аккаунт заблокирован");
        }
        return buildAuthResponse(user);
    }

    private User linkOrCreateUser(String provider, OAuthUserInfo info) {
        User user = (info.email() != null)
                ? userRepository.findByEmail(info.email().toLowerCase()).orElse(null)
                : null;

        if (user == null) {
            user = new User();
            user.setEmail(info.email() != null ? info.email().toLowerCase()
                    : provider.toLowerCase() + "_" + info.providerUserId() + "@oauth.local");
            user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setFirstName(info.firstName());
            user.setLastName(info.lastName());
            user.setRoles(Set.of("USER"));
            user.setEmailConfirmed(info.email() != null); // email подтверждён провайдером
            userRepository.save(user);
        }

        OAuthAccount account = new OAuthAccount();
        account.setUser(user);
        account.setProvider(provider);
        account.setProviderUserId(info.providerUserId());
        oauthRepository.save(account);
        return user;
    }

    private AuthResponse buildAuthResponse(User user) {
        String access = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRoles());
        String refresh = refreshTokenService.issue(user);
        return new AuthResponse(access, refresh, UserDto.from(user));
    }

    @Transactional
    public void resendConfirmation(ResendConfirmationRequest request) {
        User user = userRepository.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> ApiException.notFound("Пользователь не найден"));
        if (user.isEmailConfirmed()) {
            throw ApiException.badRequest("Email уже подтверждён");
        }
        String code = numericCode(6);
        user.setConfirmationCode(code);
        userRepository.save(user);

        emailService.sendConfirmation(user.getEmail(), user.getFirstName(), code, baseUrl);
    }

    private String numericCode(int length) {
        return String.format("%0" + length + "d", random.nextInt((int) Math.pow(10, length)));
    }
}
