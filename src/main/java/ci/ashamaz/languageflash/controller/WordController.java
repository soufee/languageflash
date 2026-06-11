package ci.ashamaz.languageflash.controller;

import ci.ashamaz.languageflash.dto.PageResponse;
import ci.ashamaz.languageflash.model.Level;
import ci.ashamaz.languageflash.model.Tag;
import ci.ashamaz.languageflash.model.Word;
import ci.ashamaz.languageflash.repository.WordRepository;
import ci.ashamaz.languageflash.security.UserPrincipal;
import ci.ashamaz.languageflash.service.DictionaryService;
import ci.ashamaz.languageflash.service.UserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Просмотр глобальной базы системных слов для выбора в личный словарь. */
@RestController
@RequestMapping("/api/v1/words")
public class WordController {

    private final WordRepository wordRepository;
    private final DictionaryService dictionaryService;
    private final UserService userService;

    public WordController(WordRepository wordRepository,
                          DictionaryService dictionaryService,
                          UserService userService) {
        this.wordRepository = wordRepository;
        this.dictionaryService = dictionaryService;
        this.userService = userService;
    }

    @GetMapping
    public PageResponse<Map<String, Object>> browse(@AuthenticationPrincipal UserPrincipal principal,
                                                    @RequestParam Long languageId,
                                                    @RequestParam String level,
                                                    @RequestParam(required = false) String tag,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        Level lvl = Level.fromString(level);
        // C1/C2 — только Premium (ТЗ 3.7)
        dictionaryService.checkLevelAccess(userService.getById(principal.id()), lvl);
        Tag tagFilter = (tag != null && !tag.isBlank()) ? Tag.valueOf(tag) : null;

        var result = wordRepository.findForBrowse(languageId, lvl.order(), lvl.order(), tagFilter,
                PageRequest.of(page, Math.min(size, 100)));
        return PageResponse.of(result, this::toDto);
    }

    private Map<String, Object> toDto(Word w) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("id", w.getId());
        map.put("word", w.getWord());
        map.put("translation", w.getTranslation());
        map.put("exampleSentence", w.getExampleSentence());
        map.put("exampleTranslation", w.getExampleTranslation());
        map.put("level", w.getLevel().name());
        return map;
    }
}
