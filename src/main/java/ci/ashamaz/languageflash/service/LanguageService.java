package ci.ashamaz.languageflash.service;

import ci.ashamaz.languageflash.model.Language;
import ci.ashamaz.languageflash.model.LanguageLevel;
import ci.ashamaz.languageflash.model.Level;
import ci.ashamaz.languageflash.repository.LanguageRepository;
import ci.ashamaz.languageflash.repository.LanguageLevelRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class LanguageService {

    @Autowired
    private LanguageRepository languageRepository;

    @Autowired
    private LanguageLevelRepository languageLevelRepository;

    public List<Language> getAllLanguages() {
        List<Language> languages = languageRepository.findAll();
        log.info("Retrieved {} languages from database: {}", languages.size(), languages);
        return languages;
    }

    public Language getLanguageById(Long id) {
        Language language = languageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Язык с ID " + id + " не найден"));
        log.debug("Retrieved language by ID {}: {}", id, language);
        return language;
    }

    @Transactional
    public Language addLanguage(String name) {
        log.debug("Attempting to add language: {}", name);
        if (languageRepository.findByName(name).isPresent()) {
            log.warn("Language with name '{}' already exists", name);
            throw new IllegalArgumentException("Язык с именем '" + name + "' уже существует");
        }
        Language language = new Language();
        language.setName(name);
        language.setActive(true);
        Language savedLanguage = languageRepository.save(language);
        log.info("Saved new language: {}", savedLanguage);

        Arrays.stream(Level.values()).forEach(level -> {
            LanguageLevel languageLevel = new LanguageLevel();
            languageLevel.setLanguage(savedLanguage);
            languageLevel.setLevel(level);
            languageLevel.setActive(true);
            languageLevelRepository.save(languageLevel);
            log.debug("Added level {} for language {}", level, savedLanguage.getId());
        });

        return savedLanguage;
    }

    @Transactional
    public void updateLanguage(Long id, String name, boolean active) {
        Language language = getLanguageById(id);
        language.setName(name);
        language.setActive(active);
        languageRepository.save(language);
        log.info("Updated language: {}", language);
    }

    public List<LanguageLevel> getLevelsForLanguage(Long languageId) {
        List<LanguageLevel> levels = languageLevelRepository.findByLanguageId(languageId);
        log.debug("Retrieved {} levels for language ID {}: {}", levels.size(), languageId, levels);
        return levels;
    }

    @Transactional
    public void updateLanguageLevel(Long languageId, Level level, boolean active) {
        LanguageLevel languageLevel = languageLevelRepository.findByLanguageIdAndLevel(languageId, level)
                .orElseGet(() -> {
                    log.info("Level {} for language {} not found, creating new", level, languageId);
                    Language language = getLanguageById(languageId);
                    LanguageLevel newLanguageLevel = new LanguageLevel();
                    newLanguageLevel.setLanguage(language);
                    newLanguageLevel.setLevel(level);
                    return newLanguageLevel;
                });
        languageLevel.setActive(active);
        languageLevelRepository.save(languageLevel);
        log.info("Updated level {} for language {}: active = {}", level, languageId, active);
    }

    public List<LanguageLevel> getAllLevels() {
        log.info("Retrieving all language levels");
        return languageLevelRepository.findAll();
    }
    public Language getLanguageByName(String name) {
        return languageRepository.findByName(name)
                .orElse(null);
    }
}