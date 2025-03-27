package ci.ashamaz.languageflash.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "texts")
@Getter
@Setter
public class Text {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "translation", nullable = false, columnDefinition = "TEXT")
    private String translation;

    @ManyToOne
    @JoinColumn(name = "language_id", nullable = false)
    private Language language;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "tags")
    private String tags;

    @Column(name = "level")
    private String level;

    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate = LocalDateTime.now();

    @OneToMany(mappedBy = "text", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<TextWord> words;

    private static final ObjectMapper mapper = new ObjectMapper();

    @JsonGetter("tagsAsSet")
    public Set<Tag> getTagsAsSet() {
        if (tags == null || tags.isEmpty()) {
            return Collections.emptySet();
        }
        try {
            Set<String> tagNames = mapper.readValue(tags, new TypeReference<Set<String>>() {});
            return tagNames.stream()
                    .map(Tag::valueOf)
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            return Collections.emptySet();
        }
    }

    @JsonGetter("dtoTagsAsSet")
    public List<Map<String, String>> getDTOTagsAsSet() {
        if (tags == null || tags.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            Set<String> tagNames = mapper.readValue(tags, new TypeReference<Set<String>>() {});
            return tagNames.stream()
                    .map(tagName -> {
                        try {
                            Tag tag = Tag.valueOf(tagName);
                            Map<String, String> tagMap = new HashMap<>();
                            tagMap.put("name", tag.name());
                            tagMap.put("russianName", tag.getRussianName());
                            tagMap.put("color", tag.getColor());
                            return tagMap;
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    public void setTagsAsSet(Set<Tag> tags) {
        try {
            this.tags = tags != null && !tags.isEmpty()
                    ? mapper.writeValueAsString(tags.stream().map(Enum::name).collect(Collectors.toSet()))
                    : null;
        } catch (IOException e) {
            this.tags = null;
        }
    }
}