package ci.ashamaz.languageflash.service;

import ci.ashamaz.languageflash.dto.DictionaryDtos.EntryDto;
import ci.ashamaz.languageflash.exception.ApiException;
import ci.ashamaz.languageflash.model.*;
import ci.ashamaz.languageflash.repository.UserDictionaryRepository;
import ci.ashamaz.languageflash.repository.UserRepository;
import ci.ashamaz.languageflash.repository.WordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Синхронизация офлайн-прогресса (ТЗ 3.9). Эндпоинты заложены для будущих
 * мобильных клиентов; веб-версия их не использует.
 * Стратегия конфликтов — server-wins: событие применяется, только если оно
 * новее последнего серверного обновления записи.
 */
@Service
@Slf4j
public class SyncService {

    public record OfflineEvent(Long entryId, boolean knows, LocalDateTime timestamp) {}
    public record SyncResult(List<EntryDto> currentState, int applied, int skipped) {}

    private final UserDictionaryRepository dictionaryRepository;
    private final UserRepository userRepository;
    private final WordRepository wordRepository;
    private final SrsService srsService;
    private final LearnService learnService;

    public SyncService(UserDictionaryRepository dictionaryRepository,
                       UserRepository userRepository,
                       WordRepository wordRepository,
                       SrsService srsService,
                       LearnService learnService) {
        this.dictionaryRepository = dictionaryRepository;
        this.userRepository = userRepository;
        this.wordRepository = wordRepository;
        this.srsService = srsService;
        this.learnService = learnService;
    }

    @Transactional
    public SyncResult applyProgress(Long userId, List<OfflineEvent> events) {
        int applied = 0;
        int skipped = 0;
        List<EntryDto> state = new ArrayList<>();

        for (OfflineEvent event : events) {
            var entryOpt = dictionaryRepository.findByIdAndUserId(event.entryId(), userId);
            if (entryOpt.isEmpty()) {
                skipped++;
                continue;
            }
            UserDictionaryEntry entry = entryOpt.get();
            // server-wins: серверное обновление новее события — событие отбрасывается
            if (entry.getLastReviewed() != null && event.timestamp() != null
                    && entry.getLastReviewed().isAfter(event.timestamp())) {
                skipped++;
            } else {
                boolean becameLearned = srsService.applyAnswer(entry, event.knows());
                dictionaryRepository.save(entry);
                learnService.recordActivity(userId, becameLearned);
                applied++;
            }
            state.add(EntryDto.from(entry));
        }
        log.info("Sync для пользователя {}: применено {}, пропущено {}", userId, applied, skipped);
        return new SyncResult(state, applied, skipped);
    }

    /** Снимок словаря для офлайн-предзагрузки. Только Premium (ТЗ 3.9). */
    public List<EntryDto> dictionarySnapshot(Long userId, Long languageId, List<String> levels) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("Пользователь не найден"));
        if (!user.hasActivePremium() && !user.getRoles().contains("ADMIN")) {
            throw ApiException.premiumRequired();
        }
        List<EntryDto> result = new ArrayList<>();
        for (String levelName : levels) {
            Level level = Level.fromString(levelName);
            wordRepository.findForBrowse(languageId, level.order(), level.order(), null,
                            PageRequest.of(0, 10000))
                    .forEach(w -> result.add(new EntryDto(null, w.getId(), w.getWord(), w.getTranslation(),
                            w.getExampleSentence(), w.getExampleTranslation(),
                            DictionarySource.SYSTEM, 1.0, false, false, null, null, null)));
        }
        return result;
    }
}
