package ci.ashamaz.languageflash.controller;

import ci.ashamaz.languageflash.dto.BillingDtos.*;
import ci.ashamaz.languageflash.security.UserPrincipal;
import ci.ashamaz.languageflash.service.BillingService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/billing")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @GetMapping("/status")
    public BillingStatus status(@AuthenticationPrincipal UserPrincipal principal) {
        return billingService.status(principal.id());
    }

    @PostMapping("/checkout")
    public CheckoutResponse checkout(@AuthenticationPrincipal UserPrincipal principal,
                                     @Valid @RequestBody CheckoutRequest request) {
        return billingService.checkout(principal.id(), request);
    }

    /** Подтверждение мок-платежа (в боевой интеграции заменяется webhook'ом шлюза). */
    @PostMapping("/mock-confirm")
    public BillingStatus mockConfirm(@AuthenticationPrincipal UserPrincipal principal,
                                     @Valid @RequestBody MockConfirmRequest request) {
        return billingService.mockConfirm(principal.id(), request.paymentId());
    }

    @PostMapping("/verify-purchase")
    public BillingStatus verifyPurchase(@AuthenticationPrincipal UserPrincipal principal,
                                        @Valid @RequestBody VerifyPurchaseRequest request) {
        return billingService.verifyPurchase(principal.id(), request);
    }

    @PostMapping("/restore-purchases")
    public BillingStatus restorePurchases(@AuthenticationPrincipal UserPrincipal principal) {
        return billingService.restorePurchases(principal.id());
    }
}
