package ci.ashamaz.languageflash.service;

import ci.ashamaz.languageflash.model.Language;
import ci.ashamaz.languageflash.model.Tag;
import ci.ashamaz.languageflash.model.Text;
import ci.ashamaz.languageflash.repository.TextRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
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
        if (text.getTags() != null && !text.getTags().isEmpty()) {
            Set<Tag> tags = Arrays.stream(text.getTags().split(","))
                    .map(String::trim)
                    .map(Tag::valueOf)
                    .collect(Collectors.toSet());
            text.setTagsAsSet(tags);
        }
        Language language = languageService.getLanguageByName(text.getLanguage().getName());
        if (language == null) {
            throw new IllegalArgumentException("Язык " + text.getLanguage().getName() + " не найден");
        }
        text.setLanguage(language); // Устанавливаем объект Language
        if (text.getWords() != null) {
            text.getWords().forEach(word -> {
                word.setText(text);
                word.setLevel(text.getLevel());
                word.setLanguage(language); // Устанавливаем тот же Language для слов
                word.setTagsAsSet(text.getTagsAsSet());
            });
        }
        textRepository.save(text);
        log.debug("Text saved with content: {}, translation: {}", text.getContent(), text.getTranslation());
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
        return textRepository.findById(textId)
                .orElseThrow(() -> new IllegalArgumentException("Text with ID " + textId + " not found"));
    }
}