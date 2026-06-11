package ci.ashamaz.languageflash.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "translation_cache")
@Getter
@Setter
public class TranslationCacheEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_text", nullable = false)
    private String sourceText;

    @Column(name = "source_language", nullable = false)
    private String sourceLanguage;

    @Column(name = "target_language", nullable = false)
    private String targetLanguage;

    @Column(nullable = false, columnDefinition = "text")
    private String translation;

    @Column(nullable = false)
    private String provider;

    @Column(name = "cached_at", nullable = false, updatable = false, insertable = false)
    private LocalDateTime cachedAt;
}
