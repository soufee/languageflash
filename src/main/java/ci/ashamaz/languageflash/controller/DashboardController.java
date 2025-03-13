package ci.ashamaz.languageflash.controller;

import ci.ashamaz.languageflash.model.*;
import ci.ashamaz.languageflash.service.LanguageService;
import ci.ashamaz.languageflash.service.UserService;
import ci.ashamaz.languageflash.service.WordProgressService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@Slf4j
public class DashboardController {

    @Autowired
    private UserService userService;

    @Autowired
    private LanguageService languageService;

    @Autowired
    private WordProgressService wordProgressService;

    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/";
        }
        log.info("Loading dashboard for user: {}", user.getEmail());
        model.addAttribute("user", user);

        // Фильтруем только активные языки
        List<Language> activeLanguages = languageService.getAllLanguages().stream()
                .filter(Language::isActive)
                .collect(Collectors.toList());
        model.addAttribute("languages", activeLanguages);

        // Все теги
        model.addAttribute("tags", Tag.values());

        // Прогресс пользователя
        List<WordProgress> activeProgress = wordProgressService.getActiveProgress(user.getId());
        List<WordProgress> learnedProgress = wordProgressService.getLearnedProgress(user.getId());
        model.addAttribute("activeWords", activeProgress);
        model.addAttribute("learnedWords", learnedProgress);
        model.addAttribute("progressCount", activeProgress.size());
        model.addAttribute("learnedCount", learnedProgress.size());

        // Настройки пользователя
        Map<String, Object> settings = userService.getSettings(user.getId());
        model.addAttribute("settings", settings);

        // Преобразуем tags в русские названия
        List<String> tagRussianNames = settings.containsKey("tags") && settings.get("tags") != null
                ? ((List<String>) settings.get("tags")).stream()
                .map(Tag::valueOf)
                .map(Tag::getRussianName)
                .collect(Collectors.toList())
                : Collections.emptyList();
        model.addAttribute("tagRussianNames", tagRussianNames);

        return "dashboard";
    }

    @PostMapping("/dashboard/update-settings")
    public String updateSettings(@RequestParam("language") String language,
                                 @RequestParam("minLevel") String minLevel,
                                 @RequestParam(value = "tags", required = false) String tags,
                                 HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/";
        }
        log.info("Updating settings for user: {}, language: {}, minLevel: {}, tags: {}",
                user.getEmail(), language, minLevel, tags);

        List<String> tagList = tags != null && !tags.isEmpty()
                ? Arrays.asList(tags.split("\\s*,\\s*"))
                : Collections.emptyList();

        Map<String, Object> settings = userService.getSettings(user.getId());
        settings.put("language", language);
        settings.put("minLevel", minLevel);
        settings.put("tags", tagList);

        userService.updateSettings(user.getId(), settings);

        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard/levels")
    @ResponseBody
    public List<String> getLevelsForLanguage(@RequestParam("language") String languageName) {
        Language language = languageService.getAllLanguages().stream()
                .filter(lang -> lang.getName().equals(languageName) && lang.isActive())
                .findFirst()
                .orElse(null);
        if (language == null) {
            return Collections.emptyList();
        }
        return languageService.getLevelsForLanguage(language.getId()).stream()
                .filter(level -> level.isActive())
                .map(level -> level.getLevel().name())
                .collect(Collectors.toList());
    }
}