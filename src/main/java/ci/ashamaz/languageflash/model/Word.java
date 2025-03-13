package ci.ashamaz.languageflash.model;

import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import javax.persistence.*;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

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

    @Column(nullable = false)
    private String level;

    @Column(name = "tags", columnDefinition = "text")
    private String tags;

    private static final ObjectMapper mapper = new ObjectMapper();

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