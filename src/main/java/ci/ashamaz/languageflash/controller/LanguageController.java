package ci.ashamaz.languageflash.controller;

import ci.ashamaz.languageflash.dto.ArticleDtos.TagDto;
import ci.ashamaz.languageflash.model.Tag;
import ci.ashamaz.languageflash.repository.LanguageLevelRepository;
import ci.ashamaz.languageflash.repository.LanguageRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/languages")
public class LanguageController {

    private final LanguageRepository languageRepository;
    private final LanguageLevelRepository levelRepository;

    public LanguageController(LanguageRepository languageRepository,
                              LanguageLevelRepository levelRepository) {
        this.languageRepository = languageRepository;
        this.levelRepository = levelRepository;
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        return languageRepository.findByActiveTrue().stream()
                .map(l -> Map.<String, Object>of("id", l.getId(), "name", l.getName()))
                .toList();
    }

    @GetMapping("/{id}/levels")
    public List<Map<String, Object>> levels(@PathVariable Long id) {
        return levelRepository.findByLanguageId(id).stream()
                .filter(ll -> ll.isActive())
                .map(ll -> Map.<String, Object>of(
                        "level", ll.getLevel().name(),
                        "order", ll.getLevel().order(),
                        "premium", ll.getLevel().isPremium()))
                .sorted(java.util.Comparator.comparingInt(m -> (Integer) m.get("order")))
                .toList();
    }

    @GetMapping("/tags")
    public List<TagDto> tags() {
        return TagDto.fromSet(Set.copyOf(Arrays.asList(Tag.values())));
    }
}
