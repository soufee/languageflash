package ci.ashamaz.languageflash.model;

import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name = "language_levels")
@Data
public class LanguageLevel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "language_id", nullable = false)
    private Language language;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Level level;

    @Column(nullable = false)
    private boolean active = true;
}
