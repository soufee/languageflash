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
import java.util.Arrays;

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

    @OneToMany(mappedBy = "text", cascade = CascadeType.ALL, orphanRemoval = false)
    @JsonManagedReference
    private List<TextWord> words;

    private static final ObjectMapper mapper = new ObjectMapper();

    @JsonGetter("tagsAsSet")
    public Set<Tag> getTagsAsSet() {
        if (tags == null || tags.isEmpty()) {
            return Collections.emptySet();
        }
        
        try {
            // Пробуем сначала распарсить как JSON массив строк
            if (tags.startsWith("[") && tags.endsWith("]")) {
                Set<String> tagNames = mapper.readValue(tags, new TypeReference<Set<String>>() {});
                return tagNames.stream()
                        .map(String::trim)
                        .filter(name -> !name.isEmpty())
                        .map(name -> {
                            try {
                                return Tag.valueOf(name);
                            } catch (IllegalArgumentException e) {
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
            } else {
                // Если не JSON, разделяем по запятым (обычная строка)
                return Arrays.stream(tags.split(","))
                        .map(String::trim)
                        .filter(name -> !name.isEmpty())
                        .map(name -> {
                            try {
                                return Tag.valueOf(name);
                            } catch (IllegalArgumentException e) {
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
            }
        } catch (IOException e) {
            // В случае ошибки парсинга, пытаемся разделить строку по запятым
            try {
                return Arrays.stream(tags.split(","))
                        .map(String::trim)
                        .filter(name -> !name.isEmpty())
                        .map(name -> {
                            try {
                                return Tag.valueOf(name);
                            } catch (IllegalArgumentException e2) {
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
            } catch (Exception e2) {
                return Collections.emptySet();
            }
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
            if (tags != null && !tags.isEmpty()) {
                Set<String> tagNames = tags.stream()
                    .map(Enum::name)
                    .collect(Collectors.toSet());
                this.tags = mapper.writeValueAsString(tagNames);
            } else {
                this.tags = null;
            }
        } catch (IOException e) {
            this.tags = null;
        }
    }
}