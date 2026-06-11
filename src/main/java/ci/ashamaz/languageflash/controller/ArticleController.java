package ci.ashamaz.languageflash.controller;

import ci.ashamaz.languageflash.dto.ArticleDtos.*;
import ci.ashamaz.languageflash.dto.PageResponse;
import ci.ashamaz.languageflash.security.UserPrincipal;
import ci.ashamaz.languageflash.service.ArticleService;
import ci.ashamaz.languageflash.service.TranslationService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/articles")
public class ArticleController {

    private final ArticleService articleService;
    private final TranslationService translationService;

    public ArticleController(ArticleService articleService, TranslationService translationService) {
        this.articleService = articleService;
        this.translationService = translationService;
    }

    @GetMapping
    public PageResponse<ArticleSummary> list(@AuthenticationPrincipal UserPrincipal principal,
                                             @RequestParam(required = false) Long languageId,
                                             @RequestParam(required = false) String level,
                                             @RequestParam(required = false) String tag,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return articleService.list(principal.id(), languageId, level, tag, page, size);
    }

    @GetMapping("/{id}")
    public ArticleFull get(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        return articleService.get(principal.id(), id);
    }

    /** Парсинг пользовательского текста без сохранения (ТЗ 3.2.2, лимит 10 000 символов). */
    @PostMapping("/parse")
    public ParseResponse parse(@Valid @RequestBody ParseRequest request) {
        return articleService.parse(request);
    }

    /** Перевод слова/фразы по клику с кэшированием (ТЗ 3.2.4). */
    @PostMapping("/translate")
    public TranslateResponse translate(@Valid @RequestBody TranslateRequest request) {
        return translationService.translate(request.text(), request.sourceLanguage(), request.targetLanguage())
                .map(t -> new TranslateResponse(request.text(), t, true))
                .orElseGet(() -> new TranslateResponse(request.text(), null, false));
    }
}
