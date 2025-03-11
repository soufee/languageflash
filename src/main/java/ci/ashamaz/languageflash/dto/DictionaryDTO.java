package ci.ashamaz.languageflash.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DictionaryDTO {
    private Long id;
    private String name;
    private String languageName;
    private String level;
    private String theme;
    private int wordCount;
    private Long languageLevelId;

    public DictionaryDTO(Long id, String name, String languageName, String level, String theme, int wordCount, Long languageLevelId) {
        this.id = id;
        this.name = name;
        this.languageName = languageName;
        this.level = level;
        this.theme = theme;
        this.wordCount = wordCount;
        this.languageLevelId = languageLevelId;
    }
}