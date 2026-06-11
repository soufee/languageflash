package ci.ashamaz.languageflash.service;

import ci.ashamaz.languageflash.model.UserDictionaryEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SrsServiceTest {

    private final SrsService srs = new SrsService();
    private UserDictionaryEntry entry;

    @BeforeEach
    void setUp() {
        entry = new UserDictionaryEntry();
        entry.setKnowledgeFactor(1.0);
        entry.setInActiveBatch(true);
    }

    @Test
    void knowReducesFactorBy25Percent() {
        srs.applyAnswer(entry, true);
        assertEquals(0.75, entry.getKnowledgeFactor(), 1e-9);
        assertFalse(entry.isLearned());
    }

    @Test
    void dontKnowIncreasesFactorBy30Percent() {
        srs.applyAnswer(entry, false);
        assertEquals(1.3, entry.getKnowledgeFactor(), 1e-9);
    }

    @Test
    void factorIsCappedAtMax() {
        entry.setKnowledgeFactor(9.0);
        srs.applyAnswer(entry, false);
        assertEquals(10.0, entry.getKnowledgeFactor(), 1e-9);
    }

    @Test
    void wordBecomesLearnedWhenFactorDropsBelowThreshold() {
        // 1.0 * 0.75^n <= 0.1 при n = 9 последовательных «знаю»
        boolean becameLearned = false;
        for (int i = 0; i < 20 && !becameLearned; i++) {
            becameLearned = srs.applyAnswer(entry, true);
        }
        assertTrue(entry.isLearned());
        assertEquals(0.0, entry.getKnowledgeFactor(), 1e-9);
        assertFalse(entry.isInActiveBatch());
        assertTrue(becameLearned);
    }

    @Test
    void learnedWordReturnsToLearningOnDontKnow() {
        srs.markAsLearned(entry);
        assertTrue(entry.isLearned());

        srs.applyAnswer(entry, false);
        assertFalse(entry.isLearned());
        assertTrue(entry.getKnowledgeFactor() > 0);
    }

    @Test
    void nextReviewDateIsSetAfterAnswer() {
        srs.applyAnswer(entry, true);
        assertNotNull(entry.getLastReviewed());
        assertNotNull(entry.getNextReviewDate());
        assertTrue(entry.getNextReviewDate().isAfter(entry.getLastReviewed()));
    }

    @Test
    void markAsLearnedForcesLearnedState() {
        srs.markAsLearned(entry);
        assertTrue(entry.isLearned());
        assertEquals(0.0, entry.getKnowledgeFactor(), 1e-9);
        assertFalse(entry.isInActiveBatch());
    }
}
