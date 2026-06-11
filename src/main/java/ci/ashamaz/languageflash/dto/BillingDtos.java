package ci.ashamaz.languageflash.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public final class BillingDtos {
    private BillingDtos() {}

    public record BillingStatus(boolean isPremium, LocalDateTime expiresAt,
                                long dictionaryUsed, Integer dictionaryLimit,
                                boolean adsEnabled, PlanPrices prices) {}

    public record PlanPrices(int month1, int month6, int month12) {}

    public record CheckoutRequest(@NotBlank String plan) {} // PREMIUM_1M / PREMIUM_6M / PREMIUM_12M

    public record CheckoutResponse(String paymentId, String confirmationUrl, String status) {}

    public record VerifyPurchaseRequest(@NotBlank String platform, @NotBlank String receipt) {}

    public record MockConfirmRequest(@NotBlank String paymentId) {}
}
