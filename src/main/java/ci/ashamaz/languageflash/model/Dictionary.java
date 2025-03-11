package ci.ashamaz.languageflash.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "dictionaries")
@Getter
@Setter
public class Dictionary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "language_level_id", nullable = false)
    private LanguageLevel languageLevel;

    @Column(nullable = false)
    private String theme;

    @ManyToMany(mappedBy = "dictionaries", fetch = FetchType.LAZY)
    private Set<Word> words = new HashSet<>();
}