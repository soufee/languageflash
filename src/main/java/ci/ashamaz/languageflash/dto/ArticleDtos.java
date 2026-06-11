package ci.ashamaz.languageflash.dto;

import ci.ashamaz.languageflash.model.Article;
import ci.ashamaz.languageflash.model.Tag;
import ci.ashamaz.languageflash.service.TokenizerService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class ArticleDtos {
    private ArticleDtos() {}

    public record ArticleSummary(Long id, String title, String languageName, Long languageId,
                                 String level, List<TagDto> tags, boolean own, LocalDateTime createdAt) {
        public static ArticleSummary from(Article a) {
            return new ArticleSummary(a.getId(), a.getTitle(), a.getLanguage().getName(),
                    a.getLanguage().getId(), a.getLevel().name(), TagDto.fromSet(a.tagsAsSet()),
                    a.getOwner() != null, a.getCreatedAt());
        }
    }

    public record ArticleFull(Long id, String title, String content, String translation,
                              String languageName, Long languageId, String level,
                              List<TagDto> tags, String status,
                              List<TokenizerService.Paragraph> parsed, LocalDateTime createdAt) {
        public static ArticleFull from(Article a, List<TokenizerService.Paragraph> parsed) {
            return new ArticleFull(a.getId(), a.getTitle(), a.getContent(), a.getTranslation(),
                    a.getLanguage().getName(), a.getLanguage().getId(), a.getLevel().name(),
                    TagDto.fromSet(a.tagsAsSet()), a.getStatus().name(), parsed, a.getCreatedAt());
        }
    }

    public record TagDto(String name, String russianName, String color) {
        public static List<TagDto> fromSet(Set<Tag> tags) {
            return tags.stream()
                    .map(t -> new TagDto(t.name(), t.getRussianName(), t.getColor()))
                    .sorted(java.util.Comparator.comparing(TagDto::name))
                    .toList();
        }
    }

    public record ParseRequest(
            @NotBlank @Size(max = 10000, message = "Максимальный размер текста — 10000 символов") String text,
            @NotBlank String sourceLanguage,
            @NotBlank String targetLanguage) {}

    public record ParseResponse(List<TokenizerService.Paragraph> paragraphs) {}

    public record TranslateRequest(
            @NotBlank @Size(max = 500) String text,
            @NotBlank String sourceLanguage,
            @NotBlank String targetLanguage) {}

    public record TranslateResponse(String text, String translation, boolean available) {}

    public record SaveArticleRequest(
            @NotBlank @Size(max = 255) String title,
            @NotBlank String content,
            String translation,
            @NotNull Long languageId,
            @NotBlank String level,
            Set<String> tags) {}
}
