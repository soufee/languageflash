package ci.ashamaz.languageflash.model;

import lombok.Data;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "programs")
@Data
public class Program {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "language_level_id", nullable = false)
    private LanguageLevel languageLevel;

    @ManyToMany
    @JoinTable(
            name = "program_dictionary",
            joinColumns = @JoinColumn(name = "program_id"),
            inverseJoinColumns = @JoinColumn(name = "dictionary_id")
    )
    private Set<Dictionary> dictionaries = new HashSet<>();
}
