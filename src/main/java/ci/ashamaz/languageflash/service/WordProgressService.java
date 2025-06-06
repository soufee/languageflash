package ci.ashamaz.languageflash.service;

import ci.ashamaz.languageflash.model.*;
import ci.ashamaz.languageflash.repository.WordProgressRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Objects;

@Service
@Slf4j
public class WordProgressService {

    @Autowired
    private WordProgressRepository wordProgressRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private WordService wordService;

    @Autowired
    private TextService textService;

    @Transactional
    public void initializeProgress(Long userId, List<Word> words) { // Оставляем List<Word>, так как это для основной программы
        log.info("Initializing progress for userId: {}, words count: {}", userId, words != null ? words.size() : 0);
        if (words == null || words.isEmpty()) {
            log.warn("No words provided for initializing progress for userId: {}", userId);
            return;
        }

        User user = userService.getUserById(userId);
        if (user == null) {
            log.error("User not found for userId: {}", userId);
            throw new IllegalArgumentException("Пользователь с ID " + userId + " не найден");
        }

        List<Long> existingActiveWordIds = wordProgressRepository.findActiveByUserId(userId).stream()
                .map(progress -> progress.getWord().getId())
                .collect(Collectors.toList());
        log.debug("Existing active word IDs for userId {}: {}", userId, existingActiveWordIds);

        List<WordProgress> newProgressList = new ArrayList<>();
        for (Word word : words) {
            if (word == null || word.getId() == null) {
                log.warn("Skipping null word or word without ID for userId: {}", userId);
                continue;
            }
            if (!existingActiveWordIds.contains(word.getId())) {
                WordProgress progress = new WordProgress();
                progress.setUser(user);
                progress.setWord(word);
                progress.setKnowledgeFactor(1.0f);
                progress.setLearned(false);
                progress.setLastReviewed(LocalDateTime.now());
                progress.setNextReviewDate(LocalDateTime.now());
                progress.setSource(WordSource.PROGRAM);
                newProgressList.add(progress);
                log.debug("Prepared new progress for userId: {}, word: {}, id: {}", userId, word.getWord(), word.getId());
            } else {
                log.debug("Word {} (id: {}) already exists in active progress for userId: {}", word.getWord(), word.getId(), userId);
            }
        }

        if (!newProgressList.isEmpty()) {
            try {
                List<WordProgress> savedProgress = wordProgressRepository.saveAll(newProgressList);
                log.info("Saved {} new progress records for userId: {}", savedProgress.size(), userId);
                savedProgress.forEach(p -> log.debug("Saved progress for userId: {}, wordId: {}, knowledgeFactor: {}", userId, p.getWord().getId(), p.getKnowledgeFactor()));
            } catch (Exception e) {
                log.error("Failed to save progress for userId: {}, error: {}", userId, e.getMessage(), e);
                throw e;
            }
        } else {
            log.info("No new progress records needed for userId: {}", userId);
        }
    }

    @Transactional
    public WordProgress initializeSingleProgress(Long userId, Long wordId, WordSource source, Long textId) {
        log.info("Initializing single progress for userId: {}, wordId: {}, source: {}, textId: {}", userId, wordId, source, textId);
        User user = userService.getUserById(userId);
        AbstractWord word = wordService.getWordById(wordId);

        Optional<WordProgress> existingProgress = wordProgressRepository.findByUserIdAndWordId(userId, wordId);
        if (existingProgress.isPresent()) {
            log.info("Progress already exists for userId: {}, wordId: {}", userId, wordId);
            return existingProgress.get();
        }

        WordProgress progress = new WordProgress();
        progress.setUser(user);
        progress.setWord(word);
        progress.setKnowledgeFactor(1.0f);
        progress.setLearned(false);
        progress.setLastReviewed(LocalDateTime.now());
        progress.setNextReviewDate(LocalDateTime.now());
        progress.setSource(source);
        
        if (textId != null && source == WordSource.TEXT) {
            Text text = textService.getTextById(textId);
            progress.setText(text);
        }
        
        WordProgress savedProgress = wordProgressRepository.save(progress);
        log.info("Progress initialized for userId: {}, wordId: {}, source: {}, knowledgeFactor: {}", 
                userId, wordId, source, savedProgress.getKnowledgeFactor());
        return savedProgress;
    }

    @Transactional
    public void updateProgress(Long userId, Long wordId, boolean knows) {
        log.info("Updating progress for userId: {}, wordId: {}, knows: {}", userId, wordId, knows);
        WordProgress progress = wordProgressRepository.findByUserIdAndWordId(userId, wordId)
                .orElseGet(() -> initializeSingleProgress(userId, wordId, WordSource.PROGRAM, null));

        float currentFactor = progress.getKnowledgeFactor();
        if (knows) {
            progress.setKnowledgeFactor(currentFactor * 0.75f);
            if (progress.getKnowledgeFactor() <= 0.1f) {
                progress.setLearned(true);
                progress.setKnowledgeFactor(0.0f);
            }
        } else {
            progress.setKnowledgeFactor(Math.min(currentFactor * 1.3f, 10.0f));
        }
        progress.setLastReviewed(LocalDateTime.now());
        progress.setNextReviewDate(progress.getLastReviewed().plusDays((long) (progress.getKnowledgeFactor() * 2)));

        wordProgressRepository.save(progress);
        log.info("Progress updated for userId: {}, wordId: {}, newKnowledgeFactor: {}, learned: {}",
                userId, wordId, progress.getKnowledgeFactor(), progress.isLearned());
    }

    public List<WordProgress> getActiveProgress(Long userId) {
        log.info("Retrieving active progress for userId: {}", userId);
        List<WordProgress> progress = wordProgressRepository.findActiveByUserId(userId).stream()
                .filter(wp -> wp.getSource() != WordSource.TEXT) // Исключаем слова из текстов
                .collect(Collectors.toList());
        log.debug("Found {} active progress entries for userId: {} (excluding TEXT source)", progress.size(), userId);
        return progress;
    }

    public List<WordProgress> getLearnedProgress(Long userId) {
        log.info("Retrieving learned progress for userId: {}", userId);
        List<WordProgress> progress = wordProgressRepository.findLearnedByUserId(userId);
        log.debug("Found {} learned progress entries for userId: {}", progress.size(), userId);
        return progress;
    }

    public WordProgress getProgress(Long userId, Long wordId) {
        log.info("Retrieving progress for userId: {}, wordId: {}", userId, wordId);
        return wordProgressRepository.findByUserIdAndWordId(userId, wordId)
                .orElseThrow(() -> {
                    log.error("Progress not found for userId: {}, wordId: {}", userId, wordId);
                    return new IllegalArgumentException("Прогресс для слова " + wordId + " и пользователя " + userId + " не найден");
                });
    }

    @Transactional
    public void resetProgress(Long userId) {
        log.info("Resetting progress for userId: {}", userId);
        List<WordProgress> progress = wordProgressRepository.findByUserId(userId);
        log.debug("Found {} progress entries to reset for userId: {}", progress.size(), userId);
        wordProgressRepository.deleteAll(progress);
        log.info("Progress reset completed for userId: {}", userId);
    }

    @Transactional
    public void save(WordProgress progress) {
        log.info("Saving progress for userId: {}, wordId: {}",
                progress.getUser().getId(), progress.getWord().getId());
        wordProgressRepository.save(progress);
        log.debug("Progress saved: knowledgeFactor: {}, learned: {}",
                progress.getKnowledgeFactor(), progress.isLearned());
    }

    public List<WordProgress> getCustomWordsProgress(Long userId) {
        log.info("Retrieving custom words progress for userId: {}", userId);
        List<WordProgress> progress = wordProgressRepository.findByUserId(userId).stream()
                .filter(wp -> wp.getWord() instanceof CustomWord)
                .collect(Collectors.toList());
        log.debug("Found {} custom words progress entries for userId: {}", progress.size(), userId);
        return progress;
    }

    @Transactional
    public void initializeTextProgress(Long userId, Long textId) {
        log.info("Initializing text progress for userId: {}, textId: {}", userId, textId);
        
        User user = userService.getUserById(userId);
        if (user == null) {
            log.error("User not found for userId: {}", userId);
            throw new IllegalArgumentException("Пользователь не найден");
        }
        
        Text text = textService.getTextById(textId);
        if (text == null) {
            log.error("Text not found for textId: {}", textId);
            throw new IllegalArgumentException("Текст не найден");
        }
        
        // Проверяем, есть ли уже слова из этого текста у пользователя
        List<WordProgress> existingProgress = wordProgressRepository.findByUserIdAndSourceAndTextId(userId, WordSource.TEXT, textId);
        if (!existingProgress.isEmpty()) {
            log.info("Text words are already in progress for userId: {}, textId: {}", userId, textId);
            return;
        }
        
        // Получаем только активные слова из текста
        List<TextWord> activeTextWords = text.getWords().stream()
                .filter(TextWord::isActive)
                .collect(Collectors.toList());
        
        if (activeTextWords.isEmpty()) {
            log.warn("No active words found in text with id: {}", textId);
            throw new IllegalArgumentException("В тексте нет активных слов для изучения");
        }
        
        log.info("Found {} active words in text with id: {}", activeTextWords.size(), textId);
        
        List<WordProgress> newProgressList = new ArrayList<>();
        for (TextWord word : activeTextWords) {
            WordProgress progress = new WordProgress();
            progress.setUser(user);
            progress.setWord(word);
            progress.setKnowledgeFactor(1.0f);
            progress.setLearned(false);
            progress.setLastReviewed(LocalDateTime.now());
            progress.setNextReviewDate(LocalDateTime.now());
            progress.setSource(WordSource.TEXT);
            progress.setText(text);
            newProgressList.add(progress);
            log.debug("Prepared new progress for userId: {}, word: {}, id: {}, from text: {}", 
                    userId, word.getWord(), word.getId(), text.getTitle());
        }
        
        if (!newProgressList.isEmpty()) {
            try {
                List<WordProgress> savedProgress = wordProgressRepository.saveAll(newProgressList);
                log.info("Saved {} new progress records for userId: {} from text: {}", 
                        savedProgress.size(), userId, text.getTitle());
            } catch (Exception e) {
                log.error("Failed to save progress for userId: {} from text: {}, error: {}", 
                        userId, text.getTitle(), e.getMessage(), e);
                throw e;
            }
        }
    }

    /**
     * Получить прогресс по словам из текстов
     */
    public List<WordProgress> getTextProgress(Long userId) {
        log.info("Getting text progress for userId: {}", userId);
        try {
            List<WordProgress> progress = wordProgressRepository.findByUserIdAndSource(userId, WordSource.TEXT);
            log.info("Found {} text progress entries for userId: {}", progress != null ? progress.size() : 0, userId);
            
            if (progress == null) {
                log.warn("getTextProgress returned null for userId: {}", userId);
                return Collections.emptyList();
            }
            
            // Проверяем корректность данных
            for (WordProgress wp : progress) {
                if (wp.getWord() == null) {
                    log.warn("Word is null for wordProgress.id={}, userId={}", wp.getId(), userId);
                }
                if (wp.getText() == null) {
                    log.warn("Text is null for wordProgress.id={}, userId={}, wordId={}", 
                            wp.getId(), userId, wp.getWord() != null ? wp.getWord().getId() : "null");
                }
            }
            
            return progress;
        } catch (Exception e) {
            log.error("Error getting text progress for userId: {}: {}", userId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Получить прогресс по словам из конкретного текста
     */
    public List<WordProgress> getTextProgressByTextId(Long userId, Long textId) {
        log.info("Getting text progress for userId: {} and textId: {}", userId, textId);
        return wordProgressRepository.findByUserIdAndSourceAndTextId(userId, WordSource.TEXT, textId);
    }

    /**
     * Получить список текстов, слова из которых изучает пользователь
     */
    public List<Text> getTextsWithWords(Long userId) {
        log.info("Getting texts with words for userId: {}", userId);
        try {
            List<WordProgress> textProgress = getTextProgress(userId);
            if (textProgress.isEmpty()) {
                log.info("No text progress found for userId: {}", userId);
                return Collections.emptyList();
            }
            
            List<Text> texts = textProgress.stream()
                    .map(WordProgress::getText)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            
            log.info("Found {} distinct texts for userId: {}", texts.size(), userId);
            for (Text text : texts) {
                log.debug("Text: id={}, title={}", text.getId(), text.getTitle());
            }
            
            return texts;
        } catch (Exception e) {
            log.error("Error getting texts with words for userId: {}: {}", userId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Получает активный прогресс по конкретному источнику для пользователя
     * @param userId ID пользователя
     * @param source Источник слов (PROGRAM, CUSTOM, TEXT)
     * @return Список активных слов из указанного источника
     */
    public List<WordProgress> getActiveProgressBySource(Long userId, WordSource source) {
        log.info("Retrieving active progress for userId: {} with source: {}", userId, source);
        List<WordProgress> progress = wordProgressRepository.findActiveByUserId(userId).stream()
                .filter(wp -> wp.getSource() == source)
                .collect(Collectors.toList());
        log.debug("Found {} active progress entries for userId: {} with source: {}", progress.size(), userId, source);
        return progress;
    }
    
    /**
     * Получает активный прогресс для программы обучения (исключая слова из текстов)
     * @param userId ID пользователя
     * @return Список активных слов для программы обучения
     */
    public List<WordProgress> getActiveProgressForProgram(Long userId) {
        log.info("Retrieving active progress for program for userId: {}", userId);
        List<WordProgress> progress = wordProgressRepository.findActiveByUserId(userId).stream()
                .filter(wp -> wp.getSource() != WordSource.TEXT)
                .collect(Collectors.toList());
        log.debug("Found {} active progress entries for program for userId: {}", progress.size(), userId);
        return progress;
    }

    /**
     * Проверяет, какие из переданных слов используются в прогрессе пользователей
     * @param textId ID текста
     * @param wordIds Список ID слов для проверки
     * @return Список ID слов, которые используются в прогрессе пользователей
     */
    public List<Long> getWordIdsInUseFromText(Long textId, List<Long> wordIds) {
        log.info("Checking if words are in use for textId: {}, wordIds: {}", textId, wordIds);
        
        if (wordIds == null || wordIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Находим все WordProgress записи, связанные с текстом и указанными словами
        List<WordProgress> progressEntries = wordProgressRepository.findAll().stream()
                .filter(wp -> wp.getSource() == WordSource.TEXT && 
                             wp.getText() != null && 
                             wp.getText().getId().equals(textId) &&
                             wp.getWord() != null && 
                             wordIds.contains(wp.getWord().getId()))
                .collect(Collectors.toList());
        
        List<Long> wordsInUse = progressEntries.stream()
                .map(wp -> wp.getWord().getId())
                .distinct()
                .collect(Collectors.toList());
        
        log.info("Found {} words in use out of {} requested for textId: {}", 
                wordsInUse.size(), wordIds.size(), textId);
        
        return wordsInUse;
    }
    
    /**
     * Логически отсоединяет слова от текста в записях WordProgress
     * Это нужно, чтобы слова, которые уже используются пользователями,
     * продолжали существовать в их прогрессе даже после удаления из текста
     * 
     * @param textId ID текста
     * @param wordIds Список ID слов для отсоединения
     */
    @Transactional
    public void detachWordsFromText(Long textId, List<Long> wordIds) {
        log.info("Detaching words from text: textId={}, wordIds={}", textId, wordIds);
        
        if (wordIds == null || wordIds.isEmpty()) {
            return;
        }
        
        // Находим все WordProgress записи, связанные с текстом и указанными словами
        List<WordProgress> progressEntries = wordProgressRepository.findAll().stream()
                .filter(wp -> wp.getSource() == WordSource.TEXT && 
                             wp.getText() != null && 
                             wp.getText().getId().equals(textId) &&
                             wp.getWord() != null && 
                             wordIds.contains(wp.getWord().getId()))
                .collect(Collectors.toList());
        
        // Отмечаем эти записи как происходящие из другого источника, чтобы они
        // не зависели от удаления слов из текста
        for (WordProgress wp : progressEntries) {
            // Меняем источник на CUSTOM, чтобы сохранить слово в прогрессе пользователя
            wp.setSource(WordSource.CUSTOM);
            // Удаляем связь с текстом, так как теперь это пользовательское слово
            wp.setText(null);
            log.debug("Detached word {} from text {} in progress for user {}", 
                    wp.getWord().getId(), textId, wp.getUser().getId());
        }
        
        // Сохраняем изменения
        if (!progressEntries.isEmpty()) {
            wordProgressRepository.saveAll(progressEntries);
            log.info("Detached {} progress entries from text {}", progressEntries.size(), textId);
        }
    }

    public boolean isTextInProgress(Long userId, Long textId) {
        return wordProgressRepository.existsByUserIdAndTextIdAndSource(userId, textId, WordSource.TEXT);
    }
}