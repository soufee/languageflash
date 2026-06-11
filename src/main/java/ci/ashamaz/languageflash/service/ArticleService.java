package ci.ashamaz.languageflash.service;

import ci.ashamaz.languageflash.dto.ArticleDtos.*;
import ci.ashamaz.languageflash.dto.PageResponse;
import ci.ashamaz.languageflash.exception.ApiException;
import ci.ashamaz.languageflash.model.*;
import ci.ashamaz.languageflash.repository.ArticleRepository;
import ci.ashamaz.languageflash.repository.LanguageRepository;
import ci.ashamaz.languageflash.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final LanguageRepository languageRepository;
    private final UserRepository userRepository;
    private final TokenizerService tokenizerService;
    private final ProfanityService profanityService;
    private final DictionaryService dictionaryService;

    public ArticleService(ArticleRepository articleRepository,
                          LanguageRepository languageRepository,
                          UserRepository userRepository,
                          TokenizerService tokenizerService,
                          ProfanityService profanityService,
                          DictionaryService dictionaryService) {
        this.articleRepository = articleRepository;
        this.languageRepository = languageRepository;
        this.userRepository = userRepository;
        this.tokenizerService = tokenizerService;
        this.profanityService = profanityService;
        this.dictionaryService = dictionaryService;
    }

    public PageResponse<ArticleSummary> list(Long userId, Long languageId, String level, String tag,
                                             int page, int size) {
        Integer levelOrder = null;
        if (level != null && !level.isBlank()) {
            levelOrder = Level.fromString(level).order();
        }
        String tagFilter = null;
        if (tag != null && !tag.isBlank()) {
            tagFilter = Tag.valueOf(tag).name();
        }
        var result = articleRepository.findFiltered(ArticleStatus.ACTIVE, userId, languageId,
                levelOrder, tagFilter, PageRequest.of(page, Math.min(size, 50)));
        return PageResponse.of(result, ArticleSummary::from);
    }

    public ArticleFull get(Long userId, Long articleId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> ApiException.notFound("Статья не найдена"));
        if (article.getStatus() != ArticleStatus.ACTIVE
                && (article.getOwner() == null || !article.getOwner().getId().equals(userId))) {
            throw ApiException.notFound("Статья не найдена");
        }
        if (article.getOwner() != null && !article.getOwner().getId().equals(userId)) {
            throw ApiException.notFound("Статья не найдена");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("Пользователь не найден"));
        dictionaryService.checkLevelAccess(user, article.getLevel());
        return ArticleFull.from(article, tokenizerService.parse(article.getContent()));
    }

    /** Парсинг пользовательского текста «на лету», без сохранения (ТЗ 3.2.2). */
    public ParseResponse parse(ParseRequest request) {
        return new ParseResponse(tokenizerService.parse(request.text()));
    }

    // ===== Администрирование статей (ТЗ 3.10.4) =====

    @Transactional
    public ArticleFull create(SaveArticleRequest request) {
        Article article = new Article();
        applyRequest(article, request);
        return ArticleFull.from(articleRepository.save(article), List.of());
    }

    @Transactional
    public ArticleFull update(Long articleId, SaveArticleRequest request) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> ApiException.notFound("Статья не найдена"));
        applyRequest(article, request);
        return ArticleFull.from(articleRepository.save(article), List.of());
    }

    @Transactional
    public void delete(Long articleId) {
        if (!articleRepository.existsById(articleId)) {
            throw ApiException.notFound("Статья не найдена");
        }
        articleRepository.deleteById(articleId);
    }

    public PageResponse<ArticleSummary> adminList(int page, int size) {
        return PageResponse.of(
                articleRepository.findByOwnerIsNullOrderByCreatedAtDesc(PageRequest.of(page, Math.min(size, 50))),
                ArticleSummary::from);
    }

    private void applyRequest(Article article, SaveArticleRequest request) {
        Language language = languageRepository.findById(request.languageId())
                .orElseThrow(() -> ApiException.notFound("Язык не найден"));
        article.setTitle(request.title());
        article.setContent(request.content());
        article.setTranslation(request.translation());
        article.setLanguage(language);
        article.setLevel(Level.fromString(request.level()));
        if (request.tags() != null) {
            Set<Tag> tags = request.tags().stream().map(Tag::valueOf).collect(Collectors.toSet());
            article.setTagsAsSet(tags);
        }
        // модерация: при обнаружении нецензурной лексики статья уходит в MODERATION
        List<String> profanity = profanityService.findProfanity(request.content());
        if (!profanity.isEmpty()) {
            article.setStatus(ArticleStatus.MODERATION);
            log.warn("Статья '{}' отправлена на модерацию, найдены слова: {}", request.title(), profanity);
        } else {
            article.setStatus(ArticleStatus.ACTIVE);
        }
    }
}
