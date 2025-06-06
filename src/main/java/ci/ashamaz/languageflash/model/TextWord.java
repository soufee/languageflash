package ci.ashamaz.languageflash.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "words")
@DiscriminatorValue("TEXT")
@Getter
@Setter
public class TextWord extends AbstractWord {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "text_id", nullable = false)
    @JsonBackReference
    private Text text;

    @Column(name = "level")
    private String level;

    @ManyToOne
    @JoinColumn(name = "language_id", nullable = false)
    private Language language;

    @Column(name = "tags")
    private String tags;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

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