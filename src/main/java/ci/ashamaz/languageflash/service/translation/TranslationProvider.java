package ci.ashamaz.languageflash.service.translation;

import java.util.Optional;

/** Контракт внешнего API перевода (ТЗ 3.2). */
public interface TranslationProvider {
    String name();
    boolean isConfigured();
    /** @return перевод или empty при недоступности провайдера (fallback на клиенте) */
    Optional<String> translate(String text, String sourceLanguage, String targetLanguage);
}
