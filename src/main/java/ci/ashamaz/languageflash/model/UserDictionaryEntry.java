package ci.ashamaz.languageflash.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Запись личного словаря. Либо ссылается на системное слово (word != null),
 * либо содержит пользовательское слово в custom-полях. Прогресс изучения
 * (интервальное повторение) хранится прямо в записи.
 */
@Entity
@Table(name = "user_dictionary")
@Getter
@Setter
public class UserDictionaryEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "word_id")
    private Word word;

    @Column(name = "custom_word")
    private String customWord;

    @Column(name = "custom_translation")
    private String customTranslation;

    @Column(name = "custom_example")
    private String customExample;

    @Column(name = "custom_example_translation")
    private String customExampleTranslation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DictionarySource source = DictionarySource.SYSTEM;

    @Column(name = "knowledge_factor", nullable = false)
    private double knowledgeFactor = 1.0;

    @Column(name = "is_learned", nullable = false)
    private boolean learned = false;

    @Column(name = "in_active_batch", nullable = false)
    private boolean inActiveBatch = false;

    @Column(name = "last_reviewed")
    private LocalDateTime lastReviewed;

    @Column(name = "next_review_date")
    private LocalDateTime nextReviewDate;

    @Column(name = "added_at", nullable = false, updatable = false, insertable = false)
    private LocalDateTime addedAt;

    public String displayWord() {
        return word != null ? word.getWord() : customWord;
    }

    public String displayTranslation() {
        return word != null ? word.getTranslation() : customTranslation;
    }

    public String displayExample() {
        return word != null ? word.getExampleSentence() : customExample;
    }

    public String displayExampleTranslation() {
        return word != null ? word.getExampleTranslation() : customExampleTranslation;
    }
}
