package ci.ashamaz.languageflash.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "words")
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

    @Column(nullable = false)
    private String exampleSentence;

    @Column(nullable = false)
    private String exampleTranslation;

    @ManyToOne
    @JoinColumn(name = "language_id", nullable = false)
    private Language language;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "word_dictionary",
            joinColumns = @JoinColumn(name = "word_id"),
            inverseJoinColumns = @JoinColumn(name = "dictionary_id")
    )
    private Set<Dictionary> dictionaries = new HashSet<>();
}