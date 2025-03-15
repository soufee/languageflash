package ci.ashamaz.languageflash.service;

import ci.ashamaz.languageflash.model.Language;
import ci.ashamaz.languageflash.model.Tag;
import ci.ashamaz.languageflash.model.Word;
import ci.ashamaz.languageflash.repository.WordProgressRepository;
import ci.ashamaz.languageflash.repository.WordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class WordService {

    @Autowired
    private WordRepository wordRepository;

    @Autowired
    private WordProgressRepository wordProgressRepository;

    @Autowired
    private LanguageService languageService;

    @Autowired
    private UserService userService;

    public List<Word> getWordsByLanguage(Long languageId) {
        log.info("Retrieving words for languageId: {}", languageId);
        List<Word> words = wordRepository.findByLanguageId(languageId);
        log.debug("Found {} words for languageId: {}", words.size(), languageId);
        return words;
    }

    public List<Word> getWordsByLanguageAndMinLevel(Long languageId, String minLevel) {
        log.info("Retrieving words for languageId: {} with minLevel: {}", languageId, minLevel);
        List<Word> words = wordRepository.findByLanguageIdAndMinLevel(languageId, minLevel);
        log.debug("Found {} words for languageId: {} and minLevel: {}", words.size(), languageId, minLevel);
        return words;
    }

    public List<Word> getWordsByLanguageLevelAndTag(Long languageId, String minLevel, String tag) {
        log.info("Retrieving words for languageId: {}, minLevel: {}, tag: {}", languageId, minLevel, tag);
        List<Word> words = wordRepository.findByLanguageIdAndMinLevelAndTag(languageId, minLevel, tag);
        log.debug("Found {} words for languageId: {}, minLevel: {}, tag: {}", words.size(), languageId, minLevel, tag);
        return words;
    }

    public Word getWordById(Long id) {
        log.info("Retrieving word by id: {}", id);
        return wordRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Word with id {} not found", id);
                    return new IllegalArgumentException("Слово с ID " + id + " не найдено");
                });
    }

    public List<Word> getAllWords() {
        log.info("Retrieving all words");
        return wordRepository.findAll();
    }

    @Transactional
    public void save(Word word) {
        log.info("Saving word: {}", word);
        wordRepository.save(word);
    }

    @Transactional
    public Word addWord(String word, String translation, String exampleSentence, String exampleTranslation,
                        Long languageId, String level, List<String> tags) {
        log.info("Adding word: {} for languageId: {}, level: {}, tags: {}", word, languageId, level, tags);
        Language language = languageService.getLanguageById(languageId);
        Word newWord = new Word();
        newWord.setWord(word);
        newWord.setTranslation(translation);
        newWord.setExampleSentence(exampleSentence);
        newWord.setExampleTranslation(exampleTranslation);
        newWord.setLanguage(language);
        newWord.setLevel(level);
        Set<Tag> tagSet = tags != null
                ? tags.stream().map(Tag::valueOf).collect(Collectors.toSet())
                : null;
        newWord.setTagsAsSet(tagSet);
        Word savedWord = wordRepository.save(newWord);
        log.info("Word added: {}", savedWord);
        return savedWord;
    }

    public List<Word> selectWordsForLearning(Long userId, String languageName, String minLevel, List<String> tags, int currentActiveCount) {
        log.info("Selecting words for user: {}, language: {}, minLevel: {}, tags: {}, currentActiveCount: {}",
                userId, languageName, minLevel, tags, currentActiveCount);

        Language language = languageService.getLanguageByName(languageName);
        if (language == null) {
            throw new IllegalArgumentException("Язык " + languageName + " не найден");
        }

        Map<String, Object> settings = userService.getSettings(userId);
        int activeWordsCount = (int) settings.getOrDefault("activeWordsCount", 50);

        int wordsNeeded = Math.max(0, activeWordsCount - currentActiveCount); // Сколько слов нужно добавить
        if (wordsNeeded == 0) {
            log.info("No additional words needed for user: {}", userId);
            return Collections.emptyList();
        }

        // Строгий порядок уровней
        List<String> levelOrder = Arrays.asList("A1", "A2", "B1", "B2", "C1", "C2");
        int minLevelIndex = levelOrder.indexOf(minLevel);
        if (minLevelIndex == -1) {
            log.error("Invalid minLevel: {}", minLevel);
            throw new IllegalArgumentException("Недопустимый уровень: " + minLevel);
        }

        // Получаем все существующие слова в прогрессе пользователя (активные и выученные)
        List<Long> existingWordIds = wordProgressRepository.findByUserId(userId).stream()
                .map(progress -> progress.getWord().getId())
                .collect(Collectors.toList());
        log.debug("Existing word IDs for user {}: {}", userId, existingWordIds);

        List<Word> selectedWords = new ArrayList<>();
        Set<String> tagSet = tags != null ? new HashSet<>(tags) : Collections.emptySet();

        // Проходим по уровням от minLevel до C2
        for (int i = minLevelIndex; i < levelOrder.size() && selectedWords.size() < wordsNeeded; i++) {
            String currentLevel = levelOrder.get(i);
            log.debug("Processing level: {}", currentLevel);

            List<Word> levelWords;
            if (!tagSet.isEmpty()) {
                levelWords = new ArrayList<>();
                for (String tag : tagSet) {
                    List<Word> taggedWords = wordRepository.findByLanguageIdAndMinLevelAndTag(language.getId(), currentLevel, tag);
                    levelWords.addAll(taggedWords);
                }
            } else {
                levelWords = wordRepository.findByLanguageIdAndMinLevel(language.getId(), currentLevel);
            }

            // Фильтруем слова, исключая уже существующие
            List<Word> filteredWords = levelWords.stream()
                    .filter(word -> !existingWordIds.contains(word.getId()))
                    .collect(Collectors.toList());

            // Добавляем отфильтрованные слова до нужного количества
            selectedWords.addAll(filteredWords);
            if (selectedWords.size() >= wordsNeeded) {
                selectedWords = selectedWords.subList(0, wordsNeeded);
                break;
            }
        }

        log.info("Selected {} words for learning for user: {}", selectedWords.size(), userId);
        return selectedWords;
    }
}