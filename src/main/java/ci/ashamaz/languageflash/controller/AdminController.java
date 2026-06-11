package ci.ashamaz.languageflash.controller;

import ci.ashamaz.languageflash.dto.ArticleDtos.ArticleFull;
import ci.ashamaz.languageflash.dto.ArticleDtos.ArticleSummary;
import ci.ashamaz.languageflash.dto.ArticleDtos.SaveArticleRequest;
import ci.ashamaz.languageflash.dto.AuthDtos.UserDto;
import ci.ashamaz.languageflash.dto.PageResponse;
import ci.ashamaz.languageflash.exception.ApiException;
import ci.ashamaz.languageflash.model.Language;
import ci.ashamaz.languageflash.model.SystemSetting;
import ci.ashamaz.languageflash.model.User;
import ci.ashamaz.languageflash.repository.LanguageRepository;
import ci.ashamaz.languageflash.repository.UserRepository;
import ci.ashamaz.languageflash.service.ArticleService;
import ci.ashamaz.languageflash.service.SystemSettingsService;
import ci.ashamaz.languageflash.service.WordImportService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    public record UpdateUserRequest(Boolean blocked, Boolean premium, LocalDateTime premiumExpiresAt) {}
    public record AddLanguageRequest(@jakarta.validation.constraints.NotBlank String name) {}

    private final UserRepository userRepository;
    private final LanguageRepository languageRepository;
    private final SystemSettingsService settingsService;
    private final WordImportService wordImportService;
    private final ArticleService articleService;

    public AdminController(UserRepository userRepository,
                           LanguageRepository languageRepository,
                           SystemSettingsService settingsService,
                           WordImportService wordImportService,
                           ArticleService articleService) {
        this.userRepository = userRepository;
        this.languageRepository = languageRepository;
        this.settingsService = settingsService;
        this.wordImportService = wordImportService;
        this.articleService = articleService;
    }

    // ===== Пользователи (ТЗ 3.10.2) =====

    @GetMapping("/users")
    public PageResponse<UserDto> users(@RequestParam(required = false) String email,
                                       @RequestParam(required = false) String subscription,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        Specification<User> spec = (root, query, cb) -> cb.conjunction();
        if (email != null && !email.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%"));
        }
        if ("PREMIUM".equalsIgnoreCase(subscription)) {
            spec = spec.and((root, query, cb) -> cb.isTrue(root.get("premium")));
        } else if ("FREE".equalsIgnoreCase(subscription)) {
            spec = spec.and((root, query, cb) -> cb.isFalse(root.get("premium")));
        }
        var result = userRepository.findAll(spec, PageRequest.of(page, Math.min(size, 100)));
        return PageResponse.of(result, UserDto::from);
    }

    @PatchMapping("/users/{id}")
    public UserDto updateUser(@PathVariable Long id, @RequestBody UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Пользователь не найден"));
        if (request.blocked() != null) {
            user.setBlocked(request.blocked());
        }
        if (request.premium() != null) {
            user.setPremium(request.premium());
            user.setPremiumExpiresAt(request.premium() ? request.premiumExpiresAt() : null);
        } else if (request.premiumExpiresAt() != null) {
            user.setPremiumExpiresAt(request.premiumExpiresAt());
        }
        userRepository.save(user);
        return UserDto.from(user);
    }

    // ===== Системные настройки (ТЗ 3.10.1, 3.10.5, 3.10.6) =====

    @GetMapping("/settings")
    public List<SystemSetting> settings() {
        return settingsService.getAll();
    }

    @PatchMapping("/settings")
    public List<SystemSetting> updateSettings(@RequestBody Map<String, String> updates) {
        settingsService.update(updates);
        return settingsService.getAll();
    }

    // ===== Импорт словарей из JSON (ТЗ 3.10.3) =====

    @PostMapping("/words/import")
    public WordImportService.ImportResult importWords(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw ApiException.badRequest("Файл пуст");
        }
        try {
            return wordImportService.importJson(file.getBytes());
        } catch (IOException e) {
            throw ApiException.badRequest("Не удалось прочитать файл: " + e.getMessage());
        }
    }

    // ===== Языки =====

    @PostMapping("/languages")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> addLanguage(@Valid @RequestBody AddLanguageRequest request) {
        if (languageRepository.findByName(request.name()).isPresent()) {
            throw ApiException.conflict("LANGUAGE_EXISTS", "Язык уже существует");
        }
        Language language = new Language();
        language.setName(request.name());
        language.setActive(true);
        languageRepository.save(language);
        return Map.of("id", language.getId(), "name", language.getName());
    }

    // ===== Статьи (ТЗ 3.10.4) =====

    @GetMapping("/articles")
    public PageResponse<ArticleSummary> articles(@RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        return articleService.adminList(page, size);
    }

    @PostMapping("/articles")
    @ResponseStatus(HttpStatus.CREATED)
    public ArticleFull createArticle(@Valid @RequestBody SaveArticleRequest request) {
        return articleService.create(request);
    }

    @PutMapping("/articles/{id}")
    public ArticleFull updateArticle(@PathVariable Long id, @Valid @RequestBody SaveArticleRequest request) {
        return articleService.update(id, request);
    }

    @DeleteMapping("/articles/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteArticle(@PathVariable Long id) {
        articleService.delete(id);
    }
}
