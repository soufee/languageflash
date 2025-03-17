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
        log.info("Initializing progress for userId: {}, words count: {}", userId, words != null ? words.size() : 0);
        if (words == null || words.isEmpty()) {
            log.warn("No words provided for initializing progress for userId: {}", userId);
            return;
        }

        User user = userService.getUserById(userId);
        if (user == null) {
            log.error("User not found for userId: {}", userId);
            return;
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
    public WordProgress initializeSingleProgress(Long userId, Long wordId) {
        log.info("Initializing single progress for userId: {}, wordId: {}", userId, wordId);
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
        log.info("Progress initialized for userId: {}, wordId: {}, knowledgeFactor: {}", userId, wordId, savedProgress.getKnowledgeFactor());
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
        log.info("Progress updated for userId: {}, wordId: {}, newKnowledgeFactor: {}, learned: {}",
                userId, wordId, progress.getKnowledgeFactor(), progress.isLearned());
    }

    public List<WordProgress> getActiveProgress(Long userId) {
        log.info("Retrieving active progress for userId: {}", userId);
        List<WordProgress> progress = wordProgressRepository.findActiveByUserId(userId);
        log.debug("Found {} active progress entries for userId: {}", progress.size(), userId);
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
}