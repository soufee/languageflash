package ci.ashamaz.languageflash.service;

import ci.ashamaz.languageflash.dto.DictionaryDtos.EntryDto;
import ci.ashamaz.languageflash.dto.PageResponse;
import ci.ashamaz.languageflash.exception.ApiException;
import ci.ashamaz.languageflash.model.*;
import ci.ashamaz.languageflash.repository.LanguageRepository;
import ci.ashamaz.languageflash.repository.StudyActivityRepository;
import ci.ashamaz.languageflash.repository.UserDictionaryRepository;
import ci.ashamaz.languageflash.repository.UserRepository;
import ci.ashamaz.languageflash.repository.WordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
public class LearnService {

    private final UserDictionaryRepository dictionaryRepository;
    private final UserRepository userRepository;
    private final WordRepository wordRepository;
    private final StudyActivityRepository activityRepository;
    private final LanguageRepository languageRepository;
    private final SrsService srsService;
    private final UserService userService;
    private final DictionaryService dictionaryService;

    public LearnService(UserDictionaryRepository dictionaryRepository,
                        UserRepository userRepository,
                        WordRepository wordRepository,
                        StudyActivityRepository activityRepository,
                        LanguageRepository languageRepository,
                        SrsService srsService,
                        UserService userService,
                        DictionaryService dictionaryService) {
        this.dictionaryRepository = dictionaryRepository;
        this.userRepository = userRepository;
        this.wordRepository = wordRepository;
        this.activityRepository = activityRepository;
        this.languageRepository = languageRepository;
        this.srsService = srsService;
        this.userService = userService;
        this.dictionaryService = dictionaryService;
    }

    public List<EntryDto> activeBatch(Long userId) {
        return dictionaryRepository.findActiveBatch(userId).stream()
                .map(EntryDto::from)
                .toList();
    }

    /** Следующее слово для проверки: приоритет у слов с истёкшим nextReviewDate и высоким factor. */
    public EntryDto next(Long userId) {
        return dictionaryRepository.findActiveBatch(userId).stream()
                .sorted(Comparator
                        .comparing((UserDictionaryEntry e) ->
                                e.getNextReviewDate() != null && e.getNextReviewDate().isAfter(LocalDateTime.now()))
                        .thenComparing(UserDictionaryEntry::getKnowledgeFactor, Comparator.reverseOrder()))
                .findFirst()
                .map(EntryDto::from)
                .orElse(null);
    }

    @Transactional
    public EntryDto answer(Long userId, Long entryId, boolean knows, boolean forceLearned) {
        UserDictionaryEntry entry = dictionaryRepository.findByIdAndUserId(entryId, userId)
                .orElseThrow(() -> ApiException.notFound("Запись словаря не найдена"));

        boolean becameLearned;
        if (forceLearned) {
            srsService.markAsLearned(entry);
            becameLearned = true;
        } else {
            becameLearned = srsService.applyAnswer(entry, knows);
        }
        dictionaryRepository.save(entry);
        recordActivity(userId, becameLearned);
        return EntryDto.from(entry);
    }

    /** Добор слов в активную порцию до целевого размера (ТЗ 3.4). */
    @Transactional
    public List<EntryDto> refill(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("Пользователь не найден"));
        var settings = userService.getSettings(userId);
        int target = ((Number) settings.getOrDefault("activeWordsCount", 50)).intValue();

        long current = dictionaryRepository.countByUserIdAndInActiveBatchTrueAndLearnedFalse(userId);
        int needed = (int) Math.max(0, target - current);
        if (needed > 0) {
            // 1) приоритет — невыученные слова из личного словаря (ТЗ 3.5)
            List<UserDictionaryEntry> backlog = dictionaryRepository.findBacklog(userId, PageRequest.of(0, needed));
            backlog.forEach(e -> e.setInActiveBatch(true));
            dictionaryRepository.saveAll(backlog);
            needed -= backlog.size();
        }

        if (needed > 0) {
            // 2) автодобор системных слов по настройкам пользователя
            autoFillFromSystemWords(user, settings, needed);
        }
        return activeBatch(userId);
    }

    private void autoFillFromSystemWords(User user, java.util.Map<String, Object> settings, int needed) {
        Object langName = settings.get("language");
        if (langName == null) {
            return; // язык изучения не выбран — добираем только из личного словаря
        }
        String minLevel = (String) settings.getOrDefault("minLevel", "A1");
        int minOrder;
        try {
            minOrder = Level.fromString(minLevel).order();
        } catch (IllegalArgumentException e) {
            minOrder = 1;
        }
        // бесплатным пользователям системные слова добираются только до B2 (ТЗ 3.7)
        int maxOrder = user.hasActivePremium() || user.getRoles().contains("ADMIN")
                ? Level.C2.order() : Level.B2.order();

        @SuppressWarnings("unchecked")
        List<String> tags = (List<String>) settings.getOrDefault("tags", List.of());
        Tag tag = null;
        if (!tags.isEmpty()) {
            try {
                tag = Tag.valueOf(tags.get(0));
            } catch (IllegalArgumentException ignored) {
            }
        }

        Long languageId = languageRepository.findByName(langName.toString())
                .map(Language::getId)
                .orElse(null);
        if (languageId == null) {
            return;
        }

        List<Word> candidates = wordRepository.findCandidatesForUser(
                user.getId(), languageId, minOrder, maxOrder, tag, PageRequest.of(0, needed));

        for (Word word : candidates) {
            UserDictionaryEntry entry = new UserDictionaryEntry();
            entry.setUser(user);
            entry.setWord(word);
            entry.setSource(DictionarySource.SYSTEM);
            entry.setInActiveBatch(true);
            dictionaryRepository.save(entry);
        }
    }

    public PageResponse<EntryDto> learned(Long userId, int page, int size) {
        return PageResponse.of(
                dictionaryRepository.findLearned(userId, PageRequest.of(page, Math.min(size, 100))),
                EntryDto::from);
    }

    /**
     * Слова для режима 25-кадра (RSVP, ТЗ 3.3): источник — активная порция,
     * либо системные слова по уровню/тегу.
     */
    public List<EntryDto> flashWords(Long userId, String source, Long languageId, String level, String tagName, int limit) {
        int capped = Math.min(limit, 500);
        if ("ACTIVE".equalsIgnoreCase(source) || source == null) {
            List<UserDictionaryEntry> batch = dictionaryRepository.findActiveBatch(userId);
            // приоритет — слова с высоким knowledgeFactor (хуже запомненные)
            return batch.stream()
                    .sorted(Comparator.comparing(UserDictionaryEntry::getKnowledgeFactor).reversed())
                    .limit(capped)
                    .map(EntryDto::from)
                    .toList();
        }
        // SYSTEM: по уровню/тегу
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("Пользователь не найден"));
        Level lvl = level != null ? Level.fromString(level) : Level.A1;
        dictionaryService.checkLevelAccess(user, lvl);
        Tag tag = null;
        if (tagName != null && !tagName.isBlank()) {
            tag = Tag.valueOf(tagName);
        }
        return wordRepository.findForBrowse(languageId, lvl.order(), lvl.order(), tag,
                        PageRequest.of(0, capped)).getContent().stream()
                .map(w -> new EntryDto(null, w.getId(), w.getWord(), w.getTranslation(),
                        w.getExampleSentence(), w.getExampleTranslation(),
                        DictionarySource.SYSTEM, 1.0, false, false, null, null, null))
                .toList();
    }

    @Transactional
    public void recordActivity(Long userId, boolean learned) {
        User user = userRepository.getReferenceById(userId);
        StudyActivity activity = activityRepository
                .findByUserIdAndActivityDate(userId, LocalDate.now())
                .orElseGet(() -> {
                    StudyActivity a = new StudyActivity();
                    a.setUser(user);
                    a.setActivityDate(LocalDate.now());
                    return a;
                });
        activity.setAnswersCount(activity.getAnswersCount() + 1);
        if (learned) {
            activity.setLearnedCount(activity.getLearnedCount() + 1);
        }
        activityRepository.save(activity);
    }
}
