package ci.ashamaz.languageflash.service;

import ci.ashamaz.languageflash.dto.BillingDtos.*;
import ci.ashamaz.languageflash.exception.ApiException;
import ci.ashamaz.languageflash.model.User;
import ci.ashamaz.languageflash.repository.UserDictionaryRepository;
import ci.ashamaz.languageflash.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Монетизация (ТЗ 3.7). Провайдер MOCK эмулирует платёжный шлюз для локальной
 * разработки: checkout создаёт «платёж», mock-confirm активирует Premium.
 * Архитектура готова к замене на ЮKassa/Stripe (provider=YOOKASSA) без изменения API.
 * Эндпоинты verify-purchase/restore-purchases заложены для мобильных клиентов (IAP).
 */
@Service
@Slf4j
public class BillingService {

    private final UserRepository userRepository;
    private final UserDictionaryRepository dictionaryRepository;
    private final SystemSettingsService settings;
    private final String provider;

    /** paymentId -> (userId, plan) — мок-хранилище незавершённых платежей */
    private final Map<String, PendingPayment> pendingPayments = new ConcurrentHashMap<>();

    private record PendingPayment(Long userId, String plan) {}

    public BillingService(UserRepository userRepository,
                          UserDictionaryRepository dictionaryRepository,
                          SystemSettingsService settings,
                          @Value("${app.billing.provider}") String provider) {
        this.userRepository = userRepository;
        this.dictionaryRepository = dictionaryRepository;
        this.settings = settings;
        this.provider = provider;
    }

    public BillingStatus status(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("Пользователь не найден"));
        boolean premium = user.hasActivePremium();
        long used = dictionaryRepository.countByUserId(userId);
        return new BillingStatus(
                premium,
                user.getPremiumExpiresAt(),
                used,
                premium ? null : settings.freeDictionaryLimit(),
                !premium && settings.getBoolean(SystemSettingsService.ADS_ENABLED_WEB, true),
                new PlanPrices(
                        settings.getInt("price_premium_1m", 299),
                        settings.getInt("price_premium_6m", 1490),
                        settings.getInt("price_premium_12m", 2490)));
    }

    public CheckoutResponse checkout(Long userId, CheckoutRequest request) {
        validatePlan(request.plan());
        if (!"MOCK".equalsIgnoreCase(provider)) {
            // Точка интеграции боевого шлюза (ЮKassa и т.п.)
            throw new ApiException(HttpStatus.NOT_IMPLEMENTED, "PROVIDER_NOT_IMPLEMENTED",
                    "Платёжный шлюз " + provider + " не подключён");
        }
        String paymentId = UUID.randomUUID().toString();
        pendingPayments.put(paymentId, new PendingPayment(userId, request.plan()));
        log.info("MOCK checkout: payment={} user={} plan={}", paymentId, userId, request.plan());
        return new CheckoutResponse(paymentId, "/billing/mock-pay?paymentId=" + paymentId, "PENDING");
    }

    /** Подтверждение мок-платежа (в реальной интеграции — webhook от шлюза). */
    @Transactional
    public BillingStatus mockConfirm(Long userId, String paymentId) {
        PendingPayment payment = pendingPayments.remove(paymentId);
        if (payment == null || !payment.userId().equals(userId)) {
            throw ApiException.notFound("Платёж не найден");
        }
        activatePremium(payment.userId(), monthsForPlan(payment.plan()));
        return status(userId);
    }

    /** Верификация чека App Store / Google Play — заложено для мобильных клиентов. */
    public BillingStatus verifyPurchase(Long userId, VerifyPurchaseRequest request) {
        throw new ApiException(HttpStatus.NOT_IMPLEMENTED, "MOBILE_NOT_IMPLEMENTED",
                "Верификация мобильных покупок будет добавлена вместе с мобильным приложением");
    }

    public BillingStatus restorePurchases(Long userId) {
        // для веба «восстановление» — это просто актуальный статус с сервера
        return status(userId);
    }

    @Transactional
    public void activatePremium(Long userId, int months) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("Пользователь не найден"));
        LocalDateTime base = user.hasActivePremium() && user.getPremiumExpiresAt() != null
                ? user.getPremiumExpiresAt() : LocalDateTime.now();
        user.setPremium(true);
        user.setPremiumExpiresAt(base.plusMonths(months));
        userRepository.save(user);
        log.info("Premium активирован для пользователя {} до {}", userId, user.getPremiumExpiresAt());
    }

    private void validatePlan(String plan) {
        if (!plan.matches("PREMIUM_(1|6|12)M")) {
            throw ApiException.badRequest("Неизвестный тариф: " + plan);
        }
    }

    private int monthsForPlan(String plan) {
        return switch (plan) {
            case "PREMIUM_1M" -> 1;
            case "PREMIUM_6M" -> 6;
            case "PREMIUM_12M" -> 12;
            default -> throw ApiException.badRequest("Неизвестный тариф: " + plan);
        };
    }
}
