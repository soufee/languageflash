package ci.ashamaz.languageflash.service;


import ci.ashamaz.languageflash.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DashboardService {

    private final UserService userService;
    private final LanguageService languageService;
    private final WordProgressService wordProgressService;
    private final WordService wordService;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public DashboardService(UserService userService,
                            LanguageService languageService,
                            WordProgressService wordProgressService,
                            WordService wordService) {
        this.userService = userService;
        this.languageService = languageService;
        this.wordProgressService = wordProgressService;
        this.wordService = wordService;
    }

    public Map<String, Object> getDashboardData(String username) {
        User user = userService.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        Map<String, Object> settings = userService.getSettings(user.getId());
        
        // Get actual progress counts
        List<WordProgress> activeWords = wordProgressService.getActiveProgressForProgram(user.getId());
        List<WordProgress> learnedWords = wordProgressService.getLearnedProgress(user.getId());
        List<WordProgress> customWords = wordProgressService.getCustomWordsProgress(user.getId());
        
        Map<String, Object> model = new HashMap<>();
        model.put("user", user);
        model.put("settings", settings);
        model.put("progressCount", activeWords.size());
        model.put("learnedCount", learnedWords.size());
        model.put("customWordsCount", customWords.size());
        model.put("languages", languageService.getAllLanguages());
        model.put("tags", Tag.values()); // Добавляем все теги из enum
        
        // Add tag Russian names for display
        if (settings.containsKey("tags")) {
            @SuppressWarnings("unchecked")
            List<String> tagNames = (List<String>) settings.get("tags");
            List<String> tagRussianNames = tagNames.stream()
                    .map(tagName -> {
                        try {
                            return Tag.valueOf(tagName).getRussianName();
                        } catch (IllegalArgumentException e) {
                            return tagName; // fallback to original name if enum not found
                        }
                    })
                    .collect(Collectors.toList());
            model.put("tagRussianNames", tagRussianNames);
        }
        
        return model;
    }

    public ResponseEntity<Void> updateSettings(Long userId, Map<String, Object> settings) {
        try {
            String settingsJson = objectMapper.writeValueAsString(settings);
            User user = userService.getUserById(userId);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            user.setSettings(settingsJson);
            userService.save(user);
            
            // Initialize words for learning if language and minLevel are provided
            String language = (String) settings.get("language");
            String minLevel = (String) settings.get("minLevel");
            @SuppressWarnings("unchecked")
            List<String> tags = (List<String>) settings.get("tags");
            
            if (language != null && minLevel != null) {
                log.info("Initializing words for user {} with language: {}, minLevel: {}, tags: {}", 
                        userId, language, minLevel, tags);
                refillWords(userId, language, minLevel, tags);
            }
            
            return ResponseEntity.ok().build();
        } catch (JsonProcessingException e) {
            log.error("Ошибка сериализации настроек: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    public ResponseEntity<List<LanguageLevel>> getLanguageLevels(String language) {
        Language lang = languageService.getLanguageByName(language);
        if (lang == null) {
            return ResponseEntity.notFound().build();
        }
        List<LanguageLevel> levels = languageService.getLevelsForLanguage(lang.getId());
        return ResponseEntity.ok(levels);
    }

    public ResponseEntity<List<String>> getLanguageLevelsAsStrings(String language) {
        Language lang = languageService.getLanguageByName(language);
        if (lang == null) {
            return ResponseEntity.notFound().build();
        }
        List<LanguageLevel> levels = languageService.getLevelsForLanguage(lang.getId());
        List<String> levelStrings = levels.stream()
                .filter(LanguageLevel::isActive) // Фильтруем только активные уровни
                .map(level -> level.getLevel().name()) // Получаем строковое представление уровня
                .collect(Collectors.toList());
        return ResponseEntity.ok(levelStrings);
    }

    public ResponseEntity<Void> resetSettings(Long userId) {
        User user = userService.getUserById(userId);
        Map<String, Object> defaultSettings = new HashMap<>();
        defaultSettings.put("knowThreshold", 0.1);
        defaultSettings.put("flashSpeed", 1000);
        defaultSettings.put("tags", Collections.emptyList());
        try {
            String settingsJson = objectMapper.writeValueAsString(defaultSettings);
            user.setSettings(settingsJson);
            userService.save(user);
            return ResponseEntity.ok().build();
        } catch (JsonProcessingException e) {
            log.error("Ошибка сброса настроек: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    public List<WordProgress> getActiveWords(Long userId) {
        return wordProgressService.getActiveProgressForProgram(userId);
    }

    public List<WordProgress> getLearnedWords(Long userId) {
        return wordProgressService.getLearnedProgress(userId);
    }

    public Map<String, Object> getSettings(Long userId) {
        return userService.getSettings(userId);
    }

    public void refillWords(Long userId, String language, String minLevel, List<String> tags) {
        log.info("RefillWords called for userId: {}, language: {}, minLevel: {}, tags: {}", 
                userId, language, minLevel, tags);
        
        User user = userService.getUserById(userId);
        if (user == null) {
            log.error("User not found for userId: {}", userId);
            throw new IllegalArgumentException("Пользователь не найден");
        }
        
        // Get current active words count (excluding TEXT source)
        List<WordProgress> currentProgress = wordProgressService.getActiveProgressForProgram(userId);
        log.info("Current active program words count for userId {}: {}", userId, currentProgress.size());
        
        // Get target active words count from settings
        Map<String, Object> settings = userService.getSettings(userId);
        int targetActiveWordsCount = (int) settings.getOrDefault("activeWordsCount", 50);
        log.info("Target active words count for userId {}: {}", userId, targetActiveWordsCount);
        
        // Calculate how many words we need to add
        int wordsNeeded = targetActiveWordsCount - currentProgress.size();
        if (wordsNeeded <= 0) {
            log.info("No words needed for userId {}, current: {}, target: {}", 
                    userId, currentProgress.size(), targetActiveWordsCount);
            return;
        }
        
        List<Word> newWords = wordService.selectWordsForLearning(userId, language, minLevel, tags, currentProgress.size());
        log.info("Selected {} new words for userId: {}", newWords.size(), userId);
        
        if (!newWords.isEmpty()) {
            // Limit to the number of words we actually need
            List<Word> wordsToAdd = newWords.size() > wordsNeeded 
                    ? newWords.subList(0, wordsNeeded) 
                    : newWords;
            log.info("Adding {} words to progress for userId: {}", wordsToAdd.size(), userId);
            wordProgressService.initializeProgress(userId, wordsToAdd);
        } else {
            log.warn("No new words available for userId: {} with language: {}, minLevel: {}, tags: {}", 
                    userId, language, minLevel, tags);
        }
    }

    public ResponseEntity<Void> addTag(Long userId, String tag) {
        User user = userService.getUserById(userId);
        Map<String, Object> settings = userService.getSettings(userId);
        @SuppressWarnings("unchecked")
        List<String> tags = (List<String>) settings.getOrDefault("tags", new ArrayList<>());
        if (!tags.contains(tag)) {
            tags.add(tag);
            settings.put("tags", tags);
            try {
                String settingsJson = objectMapper.writeValueAsString(settings);
                user.setSettings(settingsJson);
                userService.save(user);
            } catch (JsonProcessingException e) {
                log.error("Ошибка добавления тега: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }
        return ResponseEntity.ok().build();
    }

    public void removeWord(Long userId, Long wordId) {
        WordProgress progress = wordProgressService.getProgress(userId, wordId);
        progress.setLearned(true);
        wordProgressService.save(progress);
    }

    public void updateWordProgress(Long userId, Long wordId, boolean knows) {
        wordProgressService.updateProgress(userId, wordId, knows);
    }

    public List<WordProgress> getCustomWords(Long userId) {
        return wordProgressService.getCustomWordsProgress(userId);
    }

    public void addCustomWord(String word, String translation, String example, String exampleTranslation, Long userId) {
        log.info("Adding custom word for userId: {}, word: {}, translation: {}", userId, word, translation);
        
        CustomWord newWord = new CustomWord();
        newWord.setWord(word);
        newWord.setTranslation(translation);
        newWord.setExampleSentence(example);
        newWord.setExampleTranslation(exampleTranslation);
        User user = userService.getUserById(userId);
        newWord.setUser(user);
        
        // Save the custom word first
        wordService.save(newWord);
        log.info("Custom word saved with id: {}", newWord.getId());
        
        // Create WordProgress entry for the custom word
        WordProgress progress = wordProgressService.initializeSingleProgress(
            userId, newWord.getId(), WordSource.CUSTOM, null);
        log.info("WordProgress created for custom word with id: {}", progress.getId());
    }

    public Long getTextWordsCount(Long textId) {
        List<WordProgress> textProgress = wordProgressService.getTextProgressByTextId(null, textId);
        return (long) textProgress.size();
    }

    public Long getAllTextWordsCount(Long userId) {
        List<WordProgress> allTextProgress = wordProgressService.getTextProgress(userId);
        return (long) allTextProgress.size();
    }

    public Long getLearnedWordsCount(Long userId, Long textId) {
        List<WordProgress> textProgress = wordProgressService.getTextProgressByTextId(userId, textId);
        return textProgress.stream()
                .filter(WordProgress::isLearned)
                .count();
    }

    public List<Word> getTextWords(Long textId) {
        List<WordProgress> textProgress = wordProgressService.getTextProgressByTextId(null, textId);
        return textProgress.stream()
                .map(wp -> (Word) wp.getWord())
                .collect(Collectors.toList());
    }
}