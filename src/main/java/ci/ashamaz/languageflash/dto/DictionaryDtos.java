package ci.ashamaz.languageflash.dto;

import ci.ashamaz.languageflash.model.DictionarySource;
import ci.ashamaz.languageflash.model.UserDictionaryEntry;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class DictionaryDtos {
    private DictionaryDtos() {}

    public record AddWordRequest(
            Long wordId,
            @Size(max = 100) String customWord,
            @Size(max = 200) String customTranslation,
            @Size(max = 500) String customExample,
            @Size(max = 500) String customExampleTranslation,
            DictionarySource source) {}

    public record EntryDto(Long id, Long wordId, String word, String translation,
                           String example, String exampleTranslation,
                           DictionarySource source, double knowledgeFactor, boolean learned,
                           boolean inActiveBatch, LocalDateTime lastReviewed,
                           LocalDateTime nextReviewDate, LocalDateTime addedAt) {
        public static EntryDto from(UserDictionaryEntry e) {
            return new EntryDto(e.getId(),
                    e.getWord() != null ? e.getWord().getId() : null,
                    e.displayWord(), e.displayTranslation(),
                    e.displayExample(), e.displayExampleTranslation(),
                    e.getSource(), e.getKnowledgeFactor(), e.isLearned(),
                    e.isInActiveBatch(), e.getLastReviewed(), e.getNextReviewDate(), e.getAddedAt());
        }
    }

    public record DictionaryStatus(long used, Integer limit, boolean unlimited) {}
}
