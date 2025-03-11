package ci.ashamaz.languageflash.controller;

import ci.ashamaz.languageflash.dto.DictionaryDTO;
import ci.ashamaz.languageflash.model.*;
import ci.ashamaz.languageflash.model.Dictionary;
import ci.ashamaz.languageflash.service.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.*;

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
    private DictionaryService dictionaryService;
    @Autowired
    private WordService wordService;
    @Autowired
    private ProgramService programService;
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
        String currentEmail = auth.getName();
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
        String currentEmail = auth.getName();
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
        String currentEmail = auth.getName();
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
        String currentEmail = auth.getName();
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

    @GetMapping("/dictionaries")
    public String dictionaries(Model model) {
        Map<Language, Map<String, List<DictionaryDTO>>> dictionariesByLanguage = dictionaryService.getDictionariesByLanguage();
        model.addAttribute("dictionariesByLanguage", dictionariesByLanguage);
        model.addAttribute("languageLevels", languageService.getAllLevels());
        return "dictionaries";
    }

    @PostMapping("/dictionaries/add")
    public String addDictionary(@RequestParam String name, @RequestParam Long languageLevelId, @RequestParam String theme) {
        dictionaryService.addDictionary(name, languageLevelId, theme);
        return "redirect:/admin/dictionaries";
    }

    @PostMapping("/dictionaries/upload")
    public String uploadDictionary(@RequestParam("file") MultipartFile file, Model model) {
        log.info("Uploading dictionary JSON file: {}", file.getOriginalFilename());
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> jsonData = mapper.readValue(file.getInputStream(), new TypeReference<>() {});

            Map<String, String> dictionaryData = (Map<String, String>) jsonData.get("dictionary");
            String name = dictionaryData.get("name");
            Long languageId = Long.parseLong(dictionaryData.get("languageId"));
            String levelStr = dictionaryData.get("level");
            String theme = dictionaryData.get("theme");
            Level level = Level.valueOf(levelStr);

            LanguageLevel languageLevel = languageLevelService.getLanguageLevelByLanguageAndLevel(languageId, level);
            Dictionary dictionary = dictionaryService.addDictionary(name, languageLevel.getId(), theme);

            List<Map<String, String>> wordsData = (List<Map<String, String>>) jsonData.get("words");
            for (Map<String, String> wordData : wordsData) {
                Word word = wordService.addWord(
                        wordData.get("word"),
                        wordData.get("translation"),
                        wordData.get("exampleSentence"),
                        wordData.get("exampleTranslation"),
                        languageId
                );
                dictionaryService.addWordToDictionary(dictionary.getId(), word.getId()); // Исправлено
            }
            return "redirect:/admin/dictionaries";
        } catch (Exception e) {
            log.error("Failed to upload dictionary: {}", e.getMessage());
            model.addAttribute("error", "Ошибка при загрузке файла: " + e.getMessage());
            return "dictionaries";
        }
    }

    @GetMapping("/dictionaries/{id}/words")
    public String dictionaryWords(@PathVariable Long id, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size, Model model) {
        Dictionary dictionary = dictionaryService.getDictionary(id);
        Page<Word> wordsPage = dictionaryService.getWordsInDictionary(id, page, size);
        model.addAttribute("dictionary", dictionary);
        model.addAttribute("wordsPage", wordsPage);
        model.addAttribute("allWords", wordService.getAllWords());
        return "dictionaryWords";
    }

    @PostMapping("/dictionaries/{id}/words/add")
    public String addWordToDictionary(@PathVariable Long id, @RequestParam Long wordId) {
        dictionaryService.addWordToDictionary(id, wordId);
        return "redirect:/admin/dictionaries/{id}/words";
    }

    @PostMapping("/dictionaries/{id}/words/remove")
    public String removeWordFromDictionary(@PathVariable Long id, @RequestParam Long wordId) {
        dictionaryService.removeWordFromDictionary(id, wordId);
        return "redirect:/admin/dictionaries/{id}/words";
    }

    @PostMapping("/words")
    public String addWord(@RequestParam("word") String word,
                          @RequestParam("translation") String translation,
                          @RequestParam("exampleSentence") String exampleSentence,
                          @RequestParam("exampleTranslation") String exampleTranslation,
                          @RequestParam("languageId") Long languageId) {
        log.info("Adding new word: {} for languageId={}", word, languageId);
        wordService.addWord(word, translation, exampleSentence, exampleTranslation, languageId);
        return "redirect:/admin/dictionaries";
    }

    @GetMapping("/users/{id}/programs")
    public String viewUserPrograms(@PathVariable("id") Long userId, Model model) {
        log.info("Viewing programs for userId={}", userId);
        User user = userService.getUserById(userId);
        List<Program> programs = programService.getProgramsByUser(userId);
        model.addAttribute("user", user);
        model.addAttribute("programs", programs);
        return "userPrograms";
    }
}