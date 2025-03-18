package ci.ashamaz.languageflash.controller;

import ci.ashamaz.languageflash.model.*;
import ci.ashamaz.languageflash.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminController {

    @Autowired
    private UserService userService;
    @Autowired
    private LanguageService languageService;
    @Autowired
    private LanguageLevelService languageLevelService;
    @Autowired
    private WordService wordService;
    @Autowired
    private EmailService emailService;

    @Value("${support.email:support@languageflash.com}")
    private String supportEmail;

    @GetMapping
    public String adminRedirect() {
        return "redirect:/admin/languages";
    }

    @GetMapping("/users")
    public String listUsers(@RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "10") int size,
                            Model model) {
        log.info("Handling GET /admin/users");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentEmail = auth != null ? auth.getName() : "unknown";
        Optional<User> currentUserOptional = userService.findByEmail(currentEmail);

        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage = userService.getAllUsers(pageable);
        model.addAttribute("users", userPage.getContent());
        model.addAttribute("page", userPage);
        model.addAttribute("currentUserId", currentUserOptional.map(User::getId).orElse(null));
        log.debug("Loaded {} users", userPage.getTotalElements());
        return "userList";
    }

    @GetMapping("/users/search")
    public String searchUsers(@RequestParam("email") String email,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "10") int size,
                              Model model) {
        log.info("Handling GET /admin/users/search with email={}", email);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentEmail = auth != null ? auth.getName() : "unknown";
        Optional<User> currentUserOptional = userService.findByEmail(currentEmail);

        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage = userService.searchUsersByEmail(email, pageable);
        model.addAttribute("users", userPage.getContent());
        model.addAttribute("page", userPage);
        model.addAttribute("currentUserId", currentUserOptional.map(User::getId).orElse(null));
        return "userList";
    }

    @PostMapping("/users/block")
    public String blockUser(@RequestParam("userId") Long userId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentEmail = auth != null ? auth.getName() : "unknown";
        Optional<User> currentUserOptional = userService.findByEmail(currentEmail);

        Optional<User> userOptional = userService.findById(userId);
        if (userOptional.isPresent() && currentUserOptional.isPresent()) {
            User user = userOptional.get();
            User currentUser = currentUserOptional.get();
            if (!user.getId().equals(currentUser.getId())) {
                user.setBlocked(!user.isBlocked());
                userService.save(user);

                String templatePath = user.isBlocked() ?
                        "/templates/email/blocked-email.html" :
                        "/templates/email/unblocked-email.html";
                try {
                    java.io.InputStream templateStream = this.getClass().getResourceAsStream(templatePath);
                    if (templateStream == null) {
                        log.error("Шаблон email не найден: {}", templatePath);
                        return "redirect:/admin/users";
                    }
                    String content = new String(templateStream.readAllBytes());
                    String firstName = user.getFirstName() != null ? user.getFirstName() : "Пользователь";
                    content = content.replace("${firstName}", firstName)
                            .replace("${supportEmail}", supportEmail);
                    emailService.sendHtmlEmail(user.getEmail(),
                            user.isBlocked() ? "Ваш аккаунт заблокирован" : "Ваш аккаунт разблокирован",
                            content);
                } catch (IOException e) {
                    log.error("Ошибка чтения шаблона email для {}: {}", user.getEmail(), e.getMessage());
                } catch (MessagingException e) {
                    log.error("Ошибка отправки email для {}: {}", user.getEmail(), e.getMessage());
                }
            }
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/toggle-admin")
    public String toggleAdmin(@RequestParam("userId") Long userId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentEmail = auth != null ? auth.getName() : "unknown";
        Optional<User> currentUserOptional = userService.findByEmail(currentEmail);

        Optional<User> userOptional = userService.findById(userId);
        if (userOptional.isPresent() && currentUserOptional.isPresent()) {
            User user = userOptional.get();
            User currentUser = currentUserOptional.get();
            if (!user.getId().equals(currentUser.getId())) {
                Set<String> newRoles = new HashSet<>(user.getRoles());
                if (newRoles.contains("ADMIN")) {
                    newRoles.remove("ADMIN");
                } else {
                    newRoles.add("ADMIN");
                }
                user.setRoles(newRoles);
                userService.save(user);

                String templatePath = user.getRoles().contains("ADMIN") ?
                        "/templates/email/admin-rights-granted-email.html" :
                        "/templates/email/admin-rights-removed-email.html";
                try {
                    java.io.InputStream templateStream = this.getClass().getResourceAsStream(templatePath);
                    if (templateStream == null) {
                        log.error("Шаблон email не найден: {}", templatePath);
                        return "redirect:/admin/users";
                    }
                    String content = new String(templateStream.readAllBytes());
                    String firstName = user.getFirstName() != null ? user.getFirstName() : "Пользователь";
                    content = content.replace("${firstName}", firstName)
                            .replace("${supportEmail}", supportEmail);
                    emailService.sendHtmlEmail(user.getEmail(),
                            user.getRoles().contains("ADMIN") ? "Вам предоставлены права администратора" : "Ваши права администратора сняты",
                            content);
                } catch (IOException e) {
                    log.error("Ошибка чтения шаблона email для {}: {}", user.getEmail(), e.getMessage());
                } catch (MessagingException e) {
                    log.error("Ошибка отправки email для {}: {}", user.getEmail(), e.getMessage());
                }
            }
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/languages")
    public String listLanguages(Model model) {
        log.info("Handling GET /admin/languages");
        var languages = languageService.getAllLanguages();
        Map<Long, Map<Level, Boolean>> languageLevelsMap = new HashMap<>();
        for (var language : languages) {
            var levels = languageLevelService.getLanguageLevelsByLanguage(language.getId());
            Map<Level, Boolean> levelActiveMap = new HashMap<>();
            for (var level : levels) {
                levelActiveMap.put(level.getLevel(), level.isActive());
            }
            languageLevelsMap.put(language.getId(), levelActiveMap);
        }
        model.addAttribute("languages", languages);
        model.addAttribute("levels", Level.values());
        model.addAttribute("languageLevelsMap", languageLevelsMap);
        return "languages";
    }

    @PostMapping("/languages")
    public String addLanguage(@RequestParam("name") String name, Model model) {
        log.info("Handling POST /admin/languages with name={}", name);
        try {
            languageService.addLanguage(name);
            return "redirect:/admin/languages";
        } catch (IllegalArgumentException e) {
            log.warn("Failed to add language: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            return listLanguages(model);
        }
    }

    @PostMapping("/languages/update")
    public String updateLanguage(@RequestParam("languageId") Long languageId,
                                 @RequestParam("name") String name,
                                 @RequestParam(value = "active", defaultValue = "false") boolean active) {
        languageService.updateLanguage(languageId, name, active);
        return "redirect:/admin/languages";
    }

    @PostMapping("/languages/levels/update")
    public String updateLanguageLevel(@RequestParam("languageId") Long languageId,
                                      @RequestParam("level") Level level,
                                      @RequestParam(value = "active", defaultValue = "false") boolean active) {
        log.info("Updating level for languageId={}, level={}, active={}", languageId, level, active);
        languageLevelService.updateLanguageLevel(
                languageLevelService.getLanguageLevelByLanguageAndLevel(languageId, level).getId(), active);
        return "redirect:/admin/languages";
    }

    @GetMapping("/words")
    public String listWords(@RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "10") int size,
                            @RequestParam(value = "wordFilter", required = false) String wordFilter,
                            @RequestParam(value = "translationFilter", required = false) String translationFilter,
                            Model model) {
        log.info("Handling GET /admin/words with wordFilter={}, translationFilter={}", wordFilter, translationFilter);
        Pageable pageable = PageRequest.of(page, size, Sort.by("level").ascending().and(Sort.by("word").ascending()));

        Page<Word> wordsPage = wordService.getFilteredWords(wordFilter, translationFilter, pageable);

        Map<Long, String> wordTagsMap = new HashMap<>();
        Map<Long, String> wordTagNamesMap = new HashMap<>();
        wordsPage.getContent().forEach(word -> {
            Set<Tag> tags = word.getTagsAsSet();
            String tagsRussianString = tags != null && !tags.isEmpty()
                    ? tags.stream().map(Tag::getRussianName).collect(Collectors.joining(", "))
                    : "";
            String tagsNamesString = tags != null && !tags.isEmpty()
                    ? tags.stream().map(Tag::name).collect(Collectors.joining(","))
                    : "";
            wordTagsMap.put(word.getId(), tagsRussianString);
            wordTagNamesMap.put(word.getId(), tagsNamesString);
        });

        model.addAttribute("wordsPage", wordsPage);
        model.addAttribute("languages", languageService.getAllLanguages());
        model.addAttribute("levels", Level.values());
        model.addAttribute("tags", Tag.values());
        model.addAttribute("wordTagsMap", wordTagsMap);
        model.addAttribute("wordTagNamesMap", wordTagNamesMap);
        model.addAttribute("wordFilter", wordFilter);
        model.addAttribute("translationFilter", translationFilter);

        return "adminWords";
    }

    @PostMapping("/words/add")
    public String addWord(@RequestParam("word") String word,
                          @RequestParam("translation") String translation,
                          @RequestParam("exampleSentence") String exampleSentence,
                          @RequestParam("exampleTranslation") String exampleTranslation,
                          @RequestParam("languageId") Long languageId,
                          @RequestParam("level") String level,
                          @RequestParam(value = "tags", required = false) List<String> tags) {
        log.info("Adding new word: {} for languageId={}, level={}, tags={}", word, languageId, level, tags);
        try {
            wordService.addWord(word, translation, exampleSentence, exampleTranslation, languageId, level, tags);
            return "redirect:/admin/words";
        } catch (IllegalArgumentException e) {
            log.error("Error adding word: {}", e.getMessage());
            return "redirect:/admin/words?error=" + e.getMessage();
        }
    }

    @PostMapping("/words/edit")
    public String editWord(@RequestParam("wordId") Long wordId,
                           @RequestParam("word") String word,
                           @RequestParam("translation") String translation,
                           @RequestParam("exampleSentence") String exampleSentence,
                           @RequestParam("exampleTranslation") String exampleTranslation,
                           @RequestParam("level") String level,
                           @RequestParam(value = "tags", required = false) List<String> tags) {
        log.info("Editing wordId: {}", wordId);
        try {
            Word existingWord = wordService.getWordById(wordId);
            existingWord.setWord(word);
            existingWord.setTranslation(translation);
            existingWord.setExampleSentence(exampleSentence);
            existingWord.setExampleTranslation(exampleTranslation);
            existingWord.setLevel(level);
            Set<Tag> tagSet = tags != null
                    ? tags.stream().map(Tag::valueOf).collect(Collectors.toSet())
                    : null;
            existingWord.setTagsAsSet(tagSet);
            wordService.save(existingWord);
            return "redirect:/admin/words";
        } catch (IllegalArgumentException e) {
            log.error("Error editing word {}: {}", wordId, e.getMessage());
            return "redirect:/admin/words?error=" + e.getMessage();
        }
    }
}