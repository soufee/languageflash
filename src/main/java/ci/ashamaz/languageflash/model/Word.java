package ci.ashamaz.languageflash.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.util.HashSet;
import java.util.Set;

/**
 * Системное (глобальное) слово. Таблица words исторически содержит также строки
 * type='TEXT' и type='CUSTOM' (legacy); эта сущность работает только с type='WORD'.
 */
@Entity
@Table(name = "words")
@SQLRestriction("type = 'WORD'")
@Getter
@Setter
public class Word {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String word;

    @Column(nullable = false)
    private String translation;

    @Column(name = "example_sentence")
    private String exampleSentence;

    @Column(name = "example_translation")
    private String exampleTranslation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "language_id", nullable = false)
    private Language language;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Level level = Level.A1;

    @Column(name = "level_order", nullable = false)
    private int levelOrder = 1;

    @Column(name = "type", nullable = false)
    private String type = "WORD";

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "word_tags", joinColumns = @JoinColumn(name = "word_id"))
    @Column(name = "tag")
    @Enumerated(EnumType.STRING)
    private Set<Tag> tags = new HashSet<>();

    public void setLevel(Level level) {
        this.level = level;
        this.levelOrder = level.order();
    }
}
