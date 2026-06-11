package ci.ashamaz.languageflash.controller;

import ci.ashamaz.languageflash.dto.DictionaryDtos.*;
import ci.ashamaz.languageflash.dto.PageResponse;
import ci.ashamaz.languageflash.model.DictionarySource;
import ci.ashamaz.languageflash.security.UserPrincipal;
import ci.ashamaz.languageflash.service.DictionaryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dictionary")
public class DictionaryController {

    private final DictionaryService dictionaryService;

    public DictionaryController(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    @GetMapping
    public PageResponse<EntryDto> list(@AuthenticationPrincipal UserPrincipal principal,
                                       @RequestParam(required = false) DictionarySource source,
                                       @RequestParam(required = false) Boolean learned,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        return dictionaryService.list(principal.id(), source, learned, page, size);
    }

    @GetMapping("/status")
    public DictionaryStatus status(@AuthenticationPrincipal UserPrincipal principal) {
        return dictionaryService.status(principal.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EntryDto add(@AuthenticationPrincipal UserPrincipal principal,
                        @Valid @RequestBody AddWordRequest request) {
        return dictionaryService.add(principal.id(), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        dictionaryService.delete(principal.id(), id);
    }
}
