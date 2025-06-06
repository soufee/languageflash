package ci.ashamaz.languageflash.service;

import ci.ashamaz.languageflash.model.Language;
import ci.ashamaz.languageflash.model.Tag;
import ci.ashamaz.languageflash.model.Text;
import ci.ashamaz.languageflash.model.TextWord;
import ci.ashamaz.languageflash.repository.TextRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TextService {

    @Autowired
    private TextRepository textRepository;

    @Autowired
    private LanguageService languageService;

    public Page<Text> getActiveTextsByLanguage(String language, Pageable pageable) {
        log.info("Retrieving active texts for language: {} with pagination", language);
        return textRepository.findByLanguageAndIsActiveTrueOrderByCreatedDateDesc(language, pageable);
    }

    public Page<Text> getActiveTextsByLanguageAndTag(String language, String tag, Pageable pageable) {
        log.info("Retrieving active texts for language: {} and tag: {} with pagination", language, tag);
        return textRepository.findByLanguageAndIsActiveTrueAndTagContains(language, tag, pageable);
    }

    @Transactional
    public void saveText(Text text) {
        log.info("Saving text: {}", text.getTitle());
        
        // Обработка тегов
        if (text.getTags() != null && !text.getTags().isEmpty()) {
            try {
                log.debug("Processing tags: {}", text.getTags());
                // Разделяем строку тегов по запятым
                String[] tagStrings = text.getTags().split(",");
                Set<Tag> tags = Arrays.stream(tagStrings)
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(s -> {
                            try {
                                return Tag.valueOf(s);
                            } catch (IllegalArgumentException e) {
                                log.warn("Invalid tag found: '{}', it will be skipped", s);
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
                
                log.debug("Processed tags: {}", tags);
                text.setTagsAsSet(tags);
            } catch (Exception e) {
                log.error("Error processing tags: {}", e.getMessage(), e);
                // Вместо выброса исключения устанавливаем пустые теги
                text.setTags(null);
            }
        }
        
        // Проверка и установка языка
        Language language = languageService.getLanguageByName(text.getLanguage().getName());
        if (language == null) {
            throw new IllegalArgumentException("Язык " + text.getLanguage().getName() + " не найден");
        }
        text.setLanguage(language);
        
        // Обработка слов
        if (text.getWords() != null) {
            text.getWords().forEach(word -> {
                word.setText(text);
                word.setLevel(text.getLevel());
                word.setLanguage(language);
                // Если есть теги, копируем их также в слова
                if (text.getTagsAsSet() != null && !text.getTagsAsSet().isEmpty()) {
                    word.setTagsAsSet(text.getTagsAsSet());
                }
            });
        }
        
        // Фильтруем неактивные слова перед сохранением
        // Это позволит исключить слова с isActive=false из списка слов текста
        // при отображении в пользовательском интерфейсе
        filterActiveWordsOnly(text);
        
        textRepository.save(text);
        log.info("Text saved successfully with ID: {}", text.getId());
    }

    @Transactional
    public void softDeleteText(Long textId) {
        log.info("Soft deleting text with id: {}", textId);
        Text text = textRepository.findById(textId)
                .orElseThrow(() -> new IllegalArgumentException("Text with ID " + textId + " not found"));
        text.setActive(false);
        textRepository.save(text);
    }

    public Text getTextById(Long textId) {
        log.info("Retrieving text by id: {}", textId);
        Text text = textRepository.findById(textId)
                .orElseThrow(() -> new IllegalArgumentException("Text with ID " + textId + " not found"));
        
        // Фильтруем список слов, оставляя только активные
        filterActiveWordsOnly(text);
        
        return text;
    }

    /**
     * Фильтрует список слов в тексте, оставляя только активные (isActive = true)
     * @param text Текст для фильтрации слов
     */
    private void filterActiveWordsOnly(Text text) {
        if (text != null && text.getWords() != null && !text.getWords().isEmpty()) {
            // Создаем новый список, содержащий только активные слова
            List<TextWord> activeWords = text.getWords().stream()
                    .filter(TextWord::isActive)
                    .collect(Collectors.toList());
            
            // Заменяем оригинальный список только активными словами
            text.getWords().clear();
            text.getWords().addAll(activeWords);
            
            log.debug("Filtered active words for text id {}: {} active words out of original set", 
                    text.getId(), text.getWords().size());
        }
    }

    public List<String> getAllTags() {
        return textRepository.findAll().stream()
                .map(Text::getTags)
                .filter(tags -> tags != null && !tags.isEmpty())
                .flatMap(tags -> Arrays.stream(tags.split(",")))
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
    }
}