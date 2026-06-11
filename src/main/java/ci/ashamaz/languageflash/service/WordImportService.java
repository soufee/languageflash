package ci.ashamaz.languageflash.service;

import ci.ashamaz.languageflash.exception.ApiException;
import ci.ashamaz.languageflash.model.*;
import ci.ashamaz.languageflash.repository.LanguageRepository;
import ci.ashamaz.languageflash.repository.WordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Импорт словарей из JSON (ТЗ 3.10.3) с валидацией структуры,
 * длин полей и тегов; пакетная вставка через saveAll.
 */
@Service
@Slf4j
public class WordImportService {

    public record ImportWord(String word, String translation,
                             String exampleSentence, String exampleTranslation,
                             List<String> tags) {}

    public record ImportFile(String language, String level, List<ImportWord> words) {}

    public record ImportResult(int imported, int skipped, List<String> errors) {}

    private final WordRepository wordRepository;
    private final LanguageRepository languageRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    public WordImportService(WordRepository wordRepository, LanguageRepository languageRepository) {
        this.wordRepository = wordRepository;
        this.languageRepository = languageRepository;
    }

    @Transactional
    public ImportResult importJson(byte[] fileContent) {
        ImportFile file;
        try {
            file = mapper.readValue(fileContent, ImportFile.class);
        } catch (Exception e) {
            throw ApiException.badRequest("Некорректная структура JSON-файла: " + e.getMessage());
        }
        if (file.language() == null || file.level() == null || file.words() == null || file.words().isEmpty()) {
            throw ApiException.badRequest("Файл должен содержать поля language, level и непустой массив words");
        }

        Language language = languageRepository.findByName(file.language())
                .orElseThrow(() -> ApiException.badRequest(
                        "Язык '" + file.language() + "' не создан. Сначала добавьте язык в админ-панели."));
        Level level = Level.fromString(file.level());

        List<String> errors = new ArrayList<>();
        List<Word> toInsert = new ArrayList<>();
        int skipped = 0;

        for (int i = 0; i < file.words().size(); i++) {
            ImportWord iw = file.words().get(i);
            String err = validate(iw, i);
            if (err != null) {
                errors.add(err);
                continue;
            }
            // дубликаты слов в рамках языка пропускаются
            if (!wordRepository.findByLanguageIdAndWord(language.getId(), iw.word().trim()).isEmpty()) {
                skipped++;
                continue;
            }
            Word word = new Word();
            word.setWord(iw.word().trim());
            word.setTranslation(iw.translation().trim());
            word.setExampleSentence(iw.exampleSentence());
            word.setExampleTranslation(iw.exampleTranslation());
            word.setLanguage(language);
            word.setLevel(level);
            if (iw.tags() != null) {
                Set<Tag> tags = iw.tags().stream().map(Tag::valueOf).collect(Collectors.toSet());
                word.setTags(tags);
            }
            toInsert.add(word);
        }

        if (!errors.isEmpty()) {
            throw ApiException.badRequest("Ошибки валидации: " + String.join("; ", errors));
        }

        wordRepository.saveAll(toInsert); // пакетная вставка
        log.info("Импортировано {} слов ({} дубликатов пропущено) для языка {} уровня {}",
                toInsert.size(), skipped, language.getName(), level);
        return new ImportResult(toInsert.size(), skipped, List.of());
    }

    private String validate(ImportWord iw, int index) {
        String prefix = "words[" + index + "]: ";
        if (iw.word() == null || iw.word().isBlank()) return prefix + "пустое поле word";
        if (iw.word().length() > 100) return prefix + "word длиннее 100 символов";
        if (iw.translation() == null || iw.translation().isBlank()) return prefix + "пустое поле translation";
        if (iw.translation().length() > 200) return prefix + "translation длиннее 200 символов";
        if (iw.exampleSentence() != null && iw.exampleSentence().length() > 500)
            return prefix + "exampleSentence длиннее 500 символов";
        if (iw.exampleTranslation() != null && iw.exampleTranslation().length() > 500)
            return prefix + "exampleTranslation длиннее 500 символов";
        if (iw.tags() != null) {
            for (String tag : iw.tags()) {
                try {
                    Tag.valueOf(tag);
                } catch (IllegalArgumentException e) {
                    return prefix + "недопустимый тег '" + tag + "'";
                }
            }
        }
        return null;
    }
}
