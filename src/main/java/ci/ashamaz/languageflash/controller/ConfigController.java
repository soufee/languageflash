package ci.ashamaz.languageflash.controller;

import ci.ashamaz.languageflash.service.SystemSettingsService;
import ci.ashamaz.languageflash.service.oauth.OAuthVerifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Публичная конфигурация для клиентов: реклама, доступные OAuth-провайдеры, лимиты. */
@RestController
@RequestMapping("/api/v1/config")
public class ConfigController {

    private final SystemSettingsService settings;
    private final List<OAuthVerifier> verifiers;

    public ConfigController(SystemSettingsService settings, List<OAuthVerifier> verifiers) {
        this.settings = settings;
        this.verifiers = verifiers;
    }

    @GetMapping
    public Map<String, Object> config() {
        return Map.of(
                "adsEnabledWeb", settings.getBoolean(SystemSettingsService.ADS_ENABLED_WEB, true),
                "adUnitIdWebBanner", settings.get("ad_unit_id_web_banner", ""),
                "freeDictionaryLimit", settings.freeDictionaryLimit(),
                "oauthProviders", verifiers.stream()
                        .filter(OAuthVerifier::isConfigured)
                        .map(OAuthVerifier::provider)
                        .sorted()
                        .toList());
    }
}
