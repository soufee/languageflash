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
import java.util.List;
import java.util.Optional;

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
    public WordProgress initializeProgress(Long userId, Long wordId) {
        log.info("Initializing progress for userId: {}, wordId: {}", userId, wordId);
        User user = userService.getUserById(userId);
        Word word = wordService.getWordById(wordId);
        Optional<WordProgress> existingProgress = wordProgressRepository.findByUserIdAndWordId(userId, wordId);
        if (existingProgress.isPresent()) {
            return existingProgress.get();
        }
        WordProgress progress = new WordProgress();
        progress.setUser(user);
        progress.setWord(word);
        progress.setKnowledgeFactor(1.0f);
        progress.setLearned(false);
        progress.setLastReviewed(LocalDateTime.now());
        WordProgress savedProgress = wordProgressRepository.save(progress);
        log.info("Progress initialized: {}", savedProgress);
        return savedProgress;
    }

    public List<WordProgress> getActiveProgress(Long userId) {
        log.info("Retrieving active progress for userId: {}", userId);
        return wordProgressRepository.findActiveByUserId(userId);
    }

    public List<WordProgress> getLearnedProgress(Long userId) {
        log.info("Retrieving learned progress for userId: {}", userId);
        return wordProgressRepository.findLearnedByUserId(userId);
    }

    @Transactional
    public void updateProgress(Long userId, Long wordId, boolean knows) {
        log.info("Updating progress for userId: {}, wordId: {}, knows: {}", userId, wordId, knows);
        WordProgress progress = wordProgressRepository.findByUserIdAndWordId(userId, wordId)
                .orElseGet(() -> initializeProgress(userId, wordId));
        float currentFactor = progress.getKnowledgeFactor();
        if (knows) {
            progress.setKnowledgeFactor(currentFactor / 1.3f);
            if (progress.getKnowledgeFactor() < 0.1f) {
                progress.setLearned(true);
                progress.setKnowledgeFactor(0.0f);
            }
        } else {
            progress.setKnowledgeFactor(Math.min(currentFactor * 1.3f, 10.0f));
        }
        progress.setLastReviewed(LocalDateTime.now());
        wordProgressRepository.save(progress);
        log.info("Progress updated: {}", progress);
    }

    public WordProgress getProgress(Long userId, Long wordId) {
        return wordProgressRepository.findByUserIdAndWordId(userId, wordId)
                .orElseThrow(() -> new IllegalArgumentException("Прогресс для слова " + wordId + " и пользователя " + userId + " не найден"));
    }
}