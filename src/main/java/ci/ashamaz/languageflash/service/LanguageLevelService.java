package ci.ashamaz.languageflash.service;

import ci.ashamaz.languageflash.model.Language;
import ci.ashamaz.languageflash.model.LanguageLevel;
import ci.ashamaz.languageflash.model.Level;
import ci.ashamaz.languageflash.repository.LanguageLevelRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class LanguageLevelService {

    @Autowired
    private LanguageLevelRepository languageLevelRepository;

    @Autowired
    private LanguageService languageService;

    @Transactional
    public LanguageLevel addLanguageLevel(Long languageId, Level level, boolean active) {
        log.info("Adding language level: {} for languageId: {}, active: {}", level, languageId, active);
        Language language = languageService.getLanguageById(languageId);
        LanguageLevel languageLevel = new LanguageLevel();
        languageLevel.setLanguage(language);
        languageLevel.setLevel(level);
        languageLevel.setActive(active);
        LanguageLevel savedLanguageLevel = languageLevelRepository.save(languageLevel);
        log.info("Language level added: {}", savedLanguageLevel);
        return savedLanguageLevel;
    }

    public List<LanguageLevel> getLanguageLevelsByLanguage(Long languageId) {
        log.info("Retrieving language levels for languageId: {}", languageId);
        List<LanguageLevel> levels = languageLevelRepository.findByLanguageId(languageId);
        log.debug("Found {} language levels for languageId: {}", levels.size(), languageId);
        return levels;
    }

    public LanguageLevel getLanguageLevelById(Long id) {
        log.info("Retrieving language level by id: {}", id);
        return languageLevelRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Language level with id {} not found", id);
                    return new IllegalArgumentException("Уровень языка с ID " + id + " не найден");
                });
    }

    public LanguageLevel getLanguageLevelByLanguageAndLevel(Long languageId, Level level) {
        log.info("Retrieving language level for languageId: {}, level: {}", languageId, level);
        return languageLevelRepository.findByLanguageIdAndLevel(languageId, level)
                .orElseThrow(() -> {
                    log.error("Language level {} for languageId {} not found", level, languageId);
                    return new IllegalArgumentException("Уровень " + level + " для языка с ID " + languageId + " не найден");
                });
    }

    @Transactional
    public void updateLanguageLevel(Long id, boolean active) {
        log.info("Updating language level id: {}, active: {}", id, active);
        LanguageLevel languageLevel = getLanguageLevelById(id);
        languageLevel.setActive(active);
        languageLevelRepository.save(languageLevel);
        log.info("Language level updated: {}", languageLevel);
    }

    public List<LanguageLevel> getLevelsForLanguage(Long languageId) { // Переименован для совместимости
        return languageLevelRepository.findByLanguageId(languageId);
    }

    public List<LanguageLevel> getAllLanguageLevels() {
        log.info("Retrieving all language levels");
        return languageLevelRepository.findAll();
    }
}