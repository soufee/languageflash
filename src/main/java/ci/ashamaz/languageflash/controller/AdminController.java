package ci.ashamaz.languageflash.controller;

import ci.ashamaz.languageflash.model.Level;
import ci.ashamaz.languageflash.model.User;
import ci.ashamaz.languageflash.service.EmailService;
import ci.ashamaz.languageflash.service.LanguageService;
import ci.ashamaz.languageflash.service.UserService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
    private EmailService emailService;

    @Value("${support.email:support@languageflash.com}")
    private String supportEmail;

    @GetMapping
    public String adminRedirect() {
        return "redirect:/admin/languages"; // По умолчанию на языки
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
                        "/templates/email/blocked-email-template.txt" :
                        "/templates/email/unblocked-email-template.txt";
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
            } else {
                log.warn("Попытка заблокировать самого себя: {}", currentEmail);
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
                        "/templates/email/admin-granted-email-template.txt" :
                        "/templates/email/admin-removed-email-template.txt";
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
            } else {
                log.warn("Попытка изменить свои роли: {}", currentEmail);
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
            var levels = languageService.getLevelsForLanguage(language.getId());
            Map<Level, Boolean> levelActiveMap = new HashMap<>();
            for (var level : levels) {
                levelActiveMap.put(level.getLevel(), level.isActive());
            }
            languageLevelsMap.put(language.getId(), levelActiveMap);
        }
        log.info("Loaded {} languages with levels: {}", languages.size(), languageLevelsMap);
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
        languageService.updateLanguageLevel(languageId, level, active);
        return "redirect:/admin/languages";
    }

    @GetMapping("/dictionaries")
    public String listDictionaries(Model model) {
        log.info("Handling GET /admin/dictionaries");
        return "dictionaries";
    }
}
