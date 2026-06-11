package ci.ashamaz.languageflash.service;

import ci.ashamaz.languageflash.model.TranslationCacheEntry;
import ci.ashamaz.languageflash.repository.TranslationCacheRepository;
import ci.ashamaz.languageflash.service.translation.TranslationProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Перевод с кэшированием (ТЗ 3.2): результаты внешнего API сохраняются
 * в translation_cache и переиспользуются для всех пользователей. TTL — 30 дней.
 */
@Service
@Slf4j
public class TranslationService {

    private final TranslationCacheRepository cacheRepository;
    private final TranslationProvider provider;
    private final int cacheTtlDays;

    public TranslationService(TranslationCacheRepository cacheRepository,
                              List<TranslationProvider> providers,
                              @Value("${app.translation.provider}") String providerName,
                              @Value("${app.translation.cache-ttl-days}") int cacheTtlDays) {
        this.cacheRepository = cacheRepository;
        this.cacheTtlDays = cacheTtlDays;
        this.provider = providers.stream()
                .filter(p -> p.name().equalsIgnoreCase(providerName) && p.isConfigured())
                .findFirst()
                .orElseGet(() -> providers.stream()
                        .filter(TranslationProvider::isConfigured)
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Нет настроенного провайдера перевода")));
        log.info("Провайдер перевода: {}", provider.name());
    }

    @Transactional
    public Optional<String> translate(String text, String sourceLanguage, String targetLanguage) {
        String normalized = text.trim();
        if (normalized.isEmpty() || normalized.length() > 1000) {
            return Optional.empty();
        }
        String src = sourceLanguage.toLowerCase(Locale.ROOT);
        String dst = targetLanguage.toLowerCase(Locale.ROOT);
        String cacheKey = normalized.toLowerCase(Locale.ROOT);

        Optional<TranslationCacheEntry> cached =
                cacheRepository.findBySourceTextAndSourceLanguageAndTargetLanguage(cacheKey, src, dst);
        if (cached.isPresent()) {
            return Optional.of(cached.get().getTranslation());
        }

        Optional<String> result = provider.translate(normalized, src, dst);
        result.ifPresent(translation -> {
            try {
                TranslationCacheEntry entry = new TranslationCacheEntry();
                entry.setSourceText(cacheKey);
                entry.setSourceLanguage(src);
                entry.setTargetLanguage(dst);
                entry.setTranslation(translation);
                entry.setProvider(provider.name());
                cacheRepository.save(entry);
            } catch (DataIntegrityViolationException ignored) {
                // конкурентная вставка того же ключа — кэш уже наполнен
            }
        });
        return result;
    }

    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void evictExpired() {
        cacheRepository.deleteOlderThan(LocalDateTime.now().minusDays(cacheTtlDays));
    }
}
