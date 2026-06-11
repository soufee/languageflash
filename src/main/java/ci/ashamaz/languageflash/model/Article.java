package ci.ashamaz.languageflash.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "articles")
@Getter
@Setter
public class Article {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(columnDefinition = "text")
    private String translation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "language_id", nullable = false)
    private Language language;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Level level = Level.A1;

    @Column(name = "level_order", nullable = false)
    private int levelOrder = 1;

    @Column(columnDefinition = "text")
    private String tags;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ArticleStatus status = ArticleStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    private User owner;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private LocalDateTime createdAt;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public void setLevel(Level level) {
        this.level = level;
        this.levelOrder = level.order();
    }

    public Set<Tag> tagsAsSet() {
        if (tags == null || tags.isBlank()) {
            return Collections.emptySet();
        }
        try {
            Set<String> names = MAPPER.readValue(tags, new TypeReference<>() {});
            return names.stream()
                    .map(n -> {
                        try {
                            return Tag.valueOf(n.trim());
                        } catch (IllegalArgumentException e) {
                            return null;
                        }
                    })
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }

    public void setTagsAsSet(Set<Tag> tagSet) {
        try {
            this.tags = (tagSet == null || tagSet.isEmpty()) ? null
                    : MAPPER.writeValueAsString(tagSet.stream().map(Enum::name).collect(Collectors.toSet()));
        } catch (Exception e) {
            this.tags = null;
        }
    }
}
