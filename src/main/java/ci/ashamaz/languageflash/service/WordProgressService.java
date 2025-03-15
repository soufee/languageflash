package ci.ashamaz.languageflash.service;

import ci.ashamaz.languageflash.model.User;
import ci.ashamaz.languageflash.model.Word;
import ci.ashamaz.languageflash.model.WordProgress;
import ci.ashamaz.languageflash.repository.WordProgressRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class WordProgressService {

    @Autowired
    private WordProgressRepository wordProgressRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private WordService wordService;

    @Transactional
    public void initializeProgress(Long userId, List<Word> words) {
        log.info("Initializing progress for user: {}, words count: {}", userId, words != null ? words.size() : 0);
        if (words == null || words.isEmpty()) {
            log.warn("No words provided for initializing progress for user: {}", userId);
            return;
        }

        User user = userService.getUserById(userId);
        if (user == null) {
            log.error("User not found for userId: {}", userId);
            return;
        }

        // Получаем существующие активные слова пользователя
        List<Long> existingActiveWordIds = wordProgressRepository.findActiveByUserId(userId).stream()
                .map(progress -> progress.getWord().getId())
                .collect(Collectors.toList());
        log.debug("Existing active word IDs for user {}: {}", userId, existingActiveWordIds);

        List<WordProgress> newProgressList = new ArrayList<>();
        for (Word word : words) {
            if (word == null || word.getId() == null) {
                log.warn("Skipping null word or word without ID for user: {}", userId);
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
                newProgressList.add(progress);
                log.debug("Prepared new progress for word: {}", word.getWord());
            } else {
                log.debug("Word {} already exists in active progress for user: {}", word.getWord(), userId);
            }
        }

        if (!newProgressList.isEmpty()) {
            try {
                List<WordProgress> savedProgress = wordProgressRepository.saveAll(newProgressList);
                log.info("Saved {} new progress records for user: {}", savedProgress.size(), userId);
                // Добавим дополнительное логирование для проверки
                savedProgress.forEach(p -> log.debug("Saved progress: {}", p));
            } catch (Exception e) {
                log.error("Failed to save progress for user: {}, error: {}", userId, e.getMessage(), e);
                throw e;
            }
        } else {
            log.info("No new progress records needed for user: {}", userId);
        }
    }

    @Transactional
    public WordProgress initializeSingleProgress(Long userId, Long wordId) {
        log.info("Initializing progress for userId: {}, wordId: {}", userId, wordId);
        User user = userService.getUserById(userId);
        Word word = wordService.getWordById(wordId);

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
        WordProgress savedProgress = wordProgressRepository.save(progress);
        log.info("Progress initialized: {}", savedProgress);
        return savedProgress;
    }

    @Transactional
    public void updateProgress(Long userId, Long wordId, boolean knows) {
        log.info("Updating progress for userId: {}, wordId: {}, knows: {}", userId, wordId, knows);
        WordProgress progress = wordProgressRepository.findByUserIdAndWordId(userId, wordId)
                .orElseGet(() -> initializeSingleProgress(userId, wordId));

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
        log.info("Progress updated: {}", progress);
    }

    public List<WordProgress> getActiveProgress(Long userId) {
        log.info("Retrieving active progress for userId: {}", userId);
        return wordProgressRepository.findActiveByUserId(userId);
    }

    public List<WordProgress> getLearnedProgress(Long userId) {
        log.info("Retrieving learned progress for userId: {}", userId);
        return wordProgressRepository.findLearnedByUserId(userId);
    }

    public WordProgress getProgress(Long userId, Long wordId) {
        return wordProgressRepository.findByUserIdAndWordId(userId, wordId)
                .orElseThrow(() -> new IllegalArgumentException("Прогресс для слова " + wordId + " и пользователя " + userId + " не найден"));
    }

    @Transactional
    public void resetProgress(Long userId) {
        log.info("Resetting progress for user: {}", userId);
        List<WordProgress> progress = wordProgressRepository.findByUserId(userId);
        wordProgressRepository.deleteAll(progress);
    }

    @Transactional
    public void save(WordProgress progress) {
        wordProgressRepository.save(progress);
    }
}