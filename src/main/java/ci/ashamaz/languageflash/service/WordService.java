package ci.ashamaz.languageflash.service;

import ci.ashamaz.languageflash.model.*;
import ci.ashamaz.languageflash.repository.WordProgressRepository;
import ci.ashamaz.languageflash.repository.WordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
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

    public AbstractWord getWordById(@NotNull Long id) {
        log.info("Retrieving word by id: {}", id);
        return wordRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Word with id {} not found", id);
                    return new IllegalArgumentException("Слово с ID " + id + " не найдено");
                });
    }

    public Word getWordByIdAsWord(@NotNull Long id) {
        log.info("Retrieving Word by id: {}", id);
        AbstractWord abstractWord = wordRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Word with id {} not found", id);
                    return new IllegalArgumentException("Слово с ID " + id + " не найдено");
                });
        if (!(abstractWord instanceof Word)) {
            log.error("Word with id {} is not a Word type, but {}", id, abstractWord.getClass().getSimpleName());
            throw new IllegalArgumentException("Слово с ID " + id + " не является общим словом и не может быть отредактировано здесь");
        }
        return (Word) abstractWord;
    }

    public Page<Word> getFilteredWords(String wordFilter, String translationFilter, @NotNull Pageable pageable) {
        log.info("Retrieving filtered words with wordFilter={}, translationFilter={}", wordFilter, translationFilter);
        if (wordFilter != null && !wordFilter.isEmpty() && translationFilter != null && !translationFilter.isEmpty()) {
            return wordRepository.findByWordStartingWithAndTranslationStartingWith(wordFilter, translationFilter, pageable);
        } else if (wordFilter != null && !wordFilter.isEmpty()) {
            return wordRepository.findByWordStartingWith(wordFilter, pageable);
        } else if (translationFilter != null && !translationFilter.isEmpty()) {
            return wordRepository.findByTranslationStartingWith(translationFilter, pageable);
        } else {
            return wordRepository.findAllWords(pageable); // Используем новый метод для Word
        }
    }

    @Transactional
    public void save(@NotNull AbstractWord word) {
        log.info("Saving word: {}", word);
        wordRepository.save(word);
    }

    @Transactional
    public Word addWord(@NotEmpty @Size(min = 1, max = 100) String word,
                        @NotEmpty @Size(min = 1, max = 100) String translation,
                        @Size(max = 500) String exampleSentence,
                        @Size(max = 500) String exampleTranslation,
                        @NotNull Long languageId,
                        @NotEmpty String level,
                        List<@NotEmpty String> tags) {
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
        Word savedWord = (Word) wordRepository.save(newWord);
        log.info("Word added: {}", savedWord);
        return savedWord;
    }

    public List<Word> selectWordsForLearning(@NotNull Long userId,
                                             @NotEmpty String languageName,
                                             @NotEmpty String minLevel,
                                             List<@NotEmpty String> tags,
                                             int currentActiveCount) {
        log.info("Selecting words for user: {}, language: {}, minLevel: {}, tags: {}, currentActiveCount: {}",
                userId, languageName, minLevel, tags, currentActiveCount);

        Language language = languageService.getLanguageByName(languageName);
        if (language == null) {
            throw new IllegalArgumentException("Язык " + languageName + " не найден");
        }

        Map<String, Object> settings = userService.getSettings(userId);
        int activeWordsCount = (int) settings.getOrDefault("activeWordsCount", 50);

        int wordsNeeded = Math.max(0, activeWordsCount - currentActiveCount);
        if (wordsNeeded == 0) {
            log.info("No additional words needed for user: {}", userId);
            return Collections.emptyList();
        }

        List<String> levelOrder = Arrays.asList("A1", "A2", "B1", "B2", "C1", "C2");
        int minLevelIndex = levelOrder.indexOf(minLevel);
        if (minLevelIndex == -1) {
            log.error("Invalid minLevel: {}", minLevel);
            throw new IllegalArgumentException("Недопустимый уровень: " + minLevel);
        }

        List<Long> existingWordIds = wordProgressRepository.findByUserId(userId).stream()
                .map(progress -> progress.getWord().getId())
                .collect(Collectors.toList());
        log.debug("Existing word IDs for user {}: {}", userId, existingWordIds);

        List<Word> selectedWords = new ArrayList<>();
        Set<String> tagSet = tags != null ? new HashSet<>(tags) : Collections.emptySet();

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

            List<Word> filteredWords = levelWords.stream()
                    .filter(word -> !existingWordIds.contains(word.getId()))
                    .collect(Collectors.toList());

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