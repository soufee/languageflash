package ci.ashamaz.languageflash.service;

import ci.ashamaz.languageflash.dto.DictionaryDtos.*;
import ci.ashamaz.languageflash.dto.PageResponse;
import ci.ashamaz.languageflash.exception.ApiException;
import ci.ashamaz.languageflash.model.*;
import ci.ashamaz.languageflash.repository.UserDictionaryRepository;
import ci.ashamaz.languageflash.repository.UserRepository;
import ci.ashamaz.languageflash.repository.WordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class DictionaryService {

    private final UserDictionaryRepository dictionaryRepository;
    private final UserRepository userRepository;
    private final WordRepository wordRepository;
    private final SystemSettingsService settings;

    public DictionaryService(UserDictionaryRepository dictionaryRepository,
                             UserRepository userRepository,
                             WordRepository wordRepository,
                             SystemSettingsService settings) {
        this.dictionaryRepository = dictionaryRepository;
        this.userRepository = userRepository;
        this.wordRepository = wordRepository;
        this.settings = settings;
    }

    public PageResponse<EntryDto> list(Long userId, DictionarySource source, Boolean learned, int page, int size) {
        var result = dictionaryRepository.findFiltered(userId, source, learned,
                PageRequest.of(page, Math.min(size, 100)));
        return PageResponse.of(result, EntryDto::from);
    }

    public DictionaryStatus status(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("Пользователь не найден"));
        long used = dictionaryRepository.countByUserId(userId);
        boolean unlimited = user.hasActivePremium();
        return new DictionaryStatus(used, unlimited ? null : settings.freeDictionaryLimit(), unlimited);
    }

    /**
     * Добавление слова с контролем лимита (ТЗ 3.5): бесплатный лимит из system_settings,
     * Premium — безлимит. При превышении — 403 DICTIONARY_LIMIT_REACHED (клиент показывает Paywall).
     */
    @Transactional
    public EntryDto add(Long userId, AddWordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("Пользователь не найден"));
        if (!user.isEmailConfirmed()) {
            throw ApiException.forbidden("EMAIL_NOT_CONFIRMED", "Подтвердите email, чтобы добавлять слова");
        }

        if (!user.hasActivePremium()) {
            long used = dictionaryRepository.countByUserId(userId);
            int limit = settings.freeDictionaryLimit();
            if (used >= limit) {
                throw ApiException.forbidden("DICTIONARY_LIMIT_REACHED",
                        "Достигнут лимит бесплатного словаря (" + limit + " слов). Оформите Premium для безлимита.");
            }
        }

        UserDictionaryEntry entry = new UserDictionaryEntry();
        entry.setUser(user);

        if (request.wordId() != null) {
            Word word = wordRepository.findById(request.wordId())
                    .orElseThrow(() -> ApiException.notFound("Системное слово не найдено"));
            checkLevelAccess(user, word.getLevel());
            if (dictionaryRepository.findByUserIdAndWordId(userId, word.getId()).isPresent()) {
                throw ApiException.conflict("ALREADY_IN_DICTIONARY", "Слово уже в личном словаре");
            }
            entry.setWord(word);
            entry.setSource(request.source() != null ? request.source() : DictionarySource.SYSTEM);
        } else {
            if (isBlank(request.customWord()) || isBlank(request.customTranslation())) {
                throw ApiException.badRequest("Для пользовательского слова обязательны слово и перевод");
            }
            entry.setCustomWord(request.customWord().trim());
            entry.setCustomTranslation(request.customTranslation().trim());
            entry.setCustomExample(request.customExample());
            entry.setCustomExampleTranslation(request.customExampleTranslation());
            entry.setSource(request.source() != null ? request.source() : DictionarySource.MANUAL);
        }
        entry.setInActiveBatch(false);
        return EntryDto.from(dictionaryRepository.save(entry));
    }

    @Transactional
    public void delete(Long userId, Long entryId) {
        UserDictionaryEntry entry = dictionaryRepository.findByIdAndUserId(entryId, userId)
                .orElseThrow(() -> ApiException.notFound("Запись словаря не найдена"));
        dictionaryRepository.delete(entry);
    }

    /** Доступ к уровням C1/C2 — только Premium (ТЗ 3.7). */
    public void checkLevelAccess(User user, Level level) {
        if (level.isPremium() && !user.hasActivePremium() && !user.getRoles().contains("ADMIN")) {
            throw ApiException.premiumRequired();
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
