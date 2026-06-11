package ci.ashamaz.languageflash.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Проверка текста на нецензурную лексику (ТЗ 3.10.4).
 * Словарь запрещённых слов хранится в system_settings (ключ profanity_words)
 * и настраивается администратором.
 */
@Service
@Slf4j
public class ProfanityService {

    private final SystemSettingsService settings;
    private final ObjectMapper mapper = new ObjectMapper();

    public ProfanityService(SystemSettingsService settings) {
        this.settings = settings;
    }

    /** @return найденные запрещённые слова (пусто = текст чистый) */
    public List<String> findProfanity(String text) {
        Set<String> banned = bannedWords();
        if (banned.isEmpty()) {
            return List.of();
        }
        Set<String> wordsInText = Set.of(text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+"));
        return banned.stream().filter(wordsInText::contains).sorted().toList();
    }

    private Set<String> bannedWords() {
        String json = settings.get(SystemSettingsService.PROFANITY_WORDS, "[]");
        try {
            List<String> words = mapper.readValue(json, new TypeReference<>() {});
            return words.stream().map(w -> w.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        } catch (Exception e) {
            log.warn("Не удалось разобрать profanity_words: {}", e.getMessage());
            return Set.of();
        }
    }
}
