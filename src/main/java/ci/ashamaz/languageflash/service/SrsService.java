package ci.ashamaz.languageflash.service;

import ci.ashamaz.languageflash.model.UserDictionaryEntry;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Алгоритм интервального повторения (ТЗ 3.4):
 * «Знаю» — factor *= 0.75; «Не знаю» — factor *= 1.3 (макс. 10.0);
 * выучено при factor <= 0.1; nextReview = lastReviewed + factor * 2 дня.
 */
@Service
public class SrsService {

    public static final double KNOW_MULTIPLIER = 0.75;
    public static final double DONT_KNOW_MULTIPLIER = 1.3;
    public static final double MAX_FACTOR = 10.0;
    public static final double LEARNED_THRESHOLD = 0.1;

    /** @return true, если слово стало выученным этим ответом */
    public boolean applyAnswer(UserDictionaryEntry entry, boolean knows) {
        double factor = entry.getKnowledgeFactor();
        boolean wasLearned = entry.isLearned();

        if (knows) {
            factor *= KNOW_MULTIPLIER;
            if (factor <= LEARNED_THRESHOLD) {
                factor = 0.0;
                entry.setLearned(true);
                entry.setInActiveBatch(false);
            }
        } else {
            // забытое выученное слово (factor ≈ 0) начинает изучение заново
            factor = factor <= LEARNED_THRESHOLD ? 1.0 : Math.min(factor * DONT_KNOW_MULTIPLIER, MAX_FACTOR);
            if (entry.isLearned()) {
                entry.setLearned(false);
            }
        }

        entry.setKnowledgeFactor(factor);
        entry.setLastReviewed(LocalDateTime.now());
        long hoursUntilNext = Math.max(1, Math.round(factor * 48)); // factor * 2 дня
        entry.setNextReviewDate(entry.getLastReviewed().plusHours(hoursUntilNext));

        return !wasLearned && entry.isLearned();
    }

    public void markAsLearned(UserDictionaryEntry entry) {
        entry.setKnowledgeFactor(0.0);
        entry.setLearned(true);
        entry.setInActiveBatch(false);
        entry.setLastReviewed(LocalDateTime.now());
        entry.setNextReviewDate(LocalDateTime.now().plusDays(7));
    }
}
