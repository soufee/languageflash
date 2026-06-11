package ci.ashamaz.languageflash.controller;

import ci.ashamaz.languageflash.dto.DictionaryDtos.EntryDto;
import ci.ashamaz.languageflash.dto.PageResponse;
import ci.ashamaz.languageflash.security.UserPrincipal;
import ci.ashamaz.languageflash.service.LearnService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/learn")
public class LearnController {

    public record AnswerRequest(@NotNull Long entryId, @NotNull Boolean knows, Boolean forceLearned) {}

    private final LearnService learnService;

    public LearnController(LearnService learnService) {
        this.learnService = learnService;
    }

    @GetMapping("/active")
    public List<EntryDto> active(@AuthenticationPrincipal UserPrincipal principal) {
        return learnService.activeBatch(principal.id());
    }

    @GetMapping("/next")
    public Map<String, Object> next(@AuthenticationPrincipal UserPrincipal principal) {
        EntryDto next = learnService.next(principal.id());
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("word", next);
        return response;
    }

    @PostMapping("/answer")
    public EntryDto answer(@AuthenticationPrincipal UserPrincipal principal,
                           @Valid @RequestBody AnswerRequest request) {
        return learnService.answer(principal.id(), request.entryId(), request.knows(),
                Optional.ofNullable(request.forceLearned()).orElse(false));
    }

    @PostMapping("/refill")
    public List<EntryDto> refill(@AuthenticationPrincipal UserPrincipal principal) {
        return learnService.refill(principal.id());
    }

    @GetMapping("/learned")
    public PageResponse<EntryDto> learned(@AuthenticationPrincipal UserPrincipal principal,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        return learnService.learned(principal.id(), page, size);
    }

    /** Слова для режима 25-кадра (RSVP, ТЗ 3.3). */
    @GetMapping("/flash-words")
    public List<EntryDto> flashWords(@AuthenticationPrincipal UserPrincipal principal,
                                     @RequestParam(defaultValue = "ACTIVE") String source,
                                     @RequestParam(required = false) Long languageId,
                                     @RequestParam(required = false) String level,
                                     @RequestParam(required = false) String tag,
                                     @RequestParam(defaultValue = "100") int limit) {
        return learnService.flashWords(principal.id(), source, languageId, level, tag, limit);
    }
}
