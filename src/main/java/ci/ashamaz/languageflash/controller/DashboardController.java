package ci.ashamaz.languageflash.controller;

import ci.ashamaz.languageflash.model.*;
import ci.ashamaz.languageflash.service.LanguageService;
import ci.ashamaz.languageflash.service.UserService;
import ci.ashamaz.languageflash.service.WordProgressService;
import ci.ashamaz.languageflash.service.WordService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @Autowired
    private WordService wordService;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) throws JsonProcessingException {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/";
        }
        log.info("Loading dashboard for user: {}", user.getEmail());
        model.addAttribute("user", user);

        List<Language> activeLanguages = languageService.getAllLanguages().stream()
                .filter(Language::isActive)
                .collect(Collectors.toList());
        model.addAttribute("languages", activeLanguages);

        model.addAttribute("tags", Tag.values());

        List<WordProgress> activeProgress = wordProgressService.getActiveProgress(user.getId());
        List<WordProgress> learnedProgress = wordProgressService.getLearnedProgress(user.getId());
        log.info("Active words count: {}, Learned words count: {}", activeProgress.size(), learnedProgress.size());
        model.addAttribute("activeWords", activeProgress);
        model.addAttribute("learnedWords", learnedProgress);
        model.addAttribute("progressCount", activeProgress.size());
        model.addAttribute("learnedCount", learnedProgress.size());

        List<Map<String, Object>> activeWordsJson = activeProgress.stream()
                .map(wp -> {
                    Map<String, Object> wordData = new HashMap<>();
                    wordData.put("id", wp.getWord().getId());
                    wordData.put("word", wp.getWord().getWord());
                    wordData.put("translation", wp.getWord().getTranslation());
                    wordData.put("knowledgeFactor", wp.getKnowledgeFactor());
                    wordData.put("exampleSentence", wp.getWord().getExampleSentence());
                    wordData.put("exampleTranslation", wp.getWord().getExampleTranslation());
                    return wordData;
                })
                .collect(Collectors.toList());
        String jsonString = objectMapper.writeValueAsString(activeWordsJson);
        log.info("activeWordsJson: {}", jsonString);
        model.addAttribute("activeWordsJson", jsonString);

        Map<String, Object> settings = userService.getSettings(user.getId());
        model.addAttribute("settings", settings);

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
                                 @RequestParam("activeWordsCount") int activeWordsCount,
                                 HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/";
        }
        log.info("Updating settings for user: {}, language: {}, minLevel: {}, tags: {}, activeWordsCount: {}",
                user.getEmail(), language, minLevel, tags, activeWordsCount);

        List<String> tagList = tags != null && !tags.isEmpty()
                ? Arrays.asList(tags.split("\\s*,\\s*"))
                : Collections.emptyList();

        Map<String, Object> settings = userService.getSettings(user.getId());
        settings.put("language", language);
        settings.put("minLevel", minLevel);
        settings.put("tags", tagList);
        settings.put("activeWordsCount", activeWordsCount);

        userService.updateSettings(user.getId(), settings);

        // Передаем текущее количество активных слов (0, так как это начальная настройка)
        initializeLearningWords(user, language, minLevel, tagList, activeWordsCount, 0, model);

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
                .filter(LanguageLevel::isActive)
                .map(level -> level.getLevel().name())
                .collect(Collectors.toList());
    }

    @PostMapping("/dashboard/reset-settings")
    public ResponseEntity<Void> resetSettings(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        log.info("Resetting settings for user: {}", user.getEmail());
        Map<String, Object> defaultSettings = new HashMap<>();
        defaultSettings.put("knowThreshold", 0.1);
        defaultSettings.put("flashSpeed", 1000);
        defaultSettings.put("tags", Collections.emptyList());
        defaultSettings.put("language", null);
        defaultSettings.put("minLevel", null);
        defaultSettings.put("activeWordsCount", 50);
        userService.updateSettings(user.getId(), defaultSettings);
        wordProgressService.resetProgress(user.getId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/dashboard/active-words-json")
    @ResponseBody
    public String getActiveWordsJson(HttpSession session) throws JsonProcessingException {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "[]";
        }
        List<WordProgress> activeProgress = wordProgressService.getActiveProgress(user.getId());
        List<Map<String, Object>> activeWordsJson = activeProgress.stream()
                .map(wp -> {
                    Map<String, Object> wordData = new HashMap<>();
                    wordData.put("id", wp.getWord().getId());
                    wordData.put("word", wp.getWord().getWord());
                    wordData.put("translation", wp.getWord().getTranslation());
                    wordData.put("knowledgeFactor", wp.getKnowledgeFactor());
                    wordData.put("exampleSentence", wp.getWord().getExampleSentence());
                    wordData.put("exampleTranslation", wp.getWord().getExampleTranslation());
                    return wordData;
                })
                .collect(Collectors.toList());
        return objectMapper.writeValueAsString(activeWordsJson);
    }

    @GetMapping("/dashboard/settings")
    @ResponseBody
    public Map<String, Object> getSettings(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Collections.emptyMap();
        }
        return userService.getSettings(user.getId());
    }

    private void initializeLearningWords(User user, String language, String minLevel, List<String> tagList, int activeWordsCount, int currentActiveCount, Model model) {
        try {
            List<Word> selectedWords = wordService.selectWordsForLearning(user.getId(), language, minLevel, tagList, currentActiveCount);
            model.addAttribute("selectedWords", selectedWords);
            if (selectedWords.size() < activeWordsCount && tagList.size() < Tag.values().length) {
                model.addAttribute("showTagPrompt", true);
                model.addAttribute("availableTags", Arrays.stream(Tag.values())
                        .filter(tag -> !tagList.contains(tag.name()))
                        .collect(Collectors.toList()));
            } else {
                wordProgressService.initializeProgress(user.getId(), selectedWords.subList(0, Math.min(activeWordsCount - currentActiveCount, selectedWords.size())));
            }
        } catch (Exception e) {
            log.error("Error initializing learning words for user: {}, error: {}", user.getEmail(), e.getMessage());
            model.addAttribute("error", "Ошибка при выборе слов для изучения: " + e.getMessage());
        }
    }

    @PostMapping("/dashboard/add-tags")
    public String addTags(@RequestParam(value = "tags", required = false) String additionalTags, HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/";
        }
        Map<String, Object> settings = userService.getSettings(user.getId());
        List<String> currentTags = (List<String>) settings.getOrDefault("tags", Collections.emptyList());
        List<String> newTags = additionalTags != null && !additionalTags.isEmpty()
                ? Arrays.asList(additionalTags.split("\\s*,\\s*"))
                : Collections.emptyList();

        List<String> updatedTags = new ArrayList<>(currentTags);
        updatedTags.addAll(newTags);
        settings.put("tags", updatedTags);
        userService.updateSettings(user.getId(), settings);

        String language = (String) settings.get("language");
        String minLevel = (String) settings.get("minLevel");
        int activeWordsCount = (int) settings.getOrDefault("activeWordsCount", 50);

        // Получаем текущее количество активных слов
        List<WordProgress> activeWords = wordProgressService.getActiveProgress(user.getId());
        int currentActiveCount = activeWords.size();

        List<Word> selectedWords = wordService.selectWordsForLearning(user.getId(), language, minLevel, updatedTags, currentActiveCount);
        wordProgressService.initializeProgress(user.getId(), selectedWords.subList(0, Math.min(activeWordsCount - currentActiveCount, selectedWords.size())));

        // Проверяем, нужно ли показать addTagsModal
        List<WordProgress> updatedActiveWords = wordProgressService.getActiveProgress(user.getId());
        if (updatedActiveWords.size() < activeWordsCount && updatedTags.size() < Tag.values().length) {
            model.addAttribute("showTagPrompt", true);
            model.addAttribute("availableTags", Arrays.stream(Tag.values())
                    .filter(tag -> !updatedTags.contains(tag.name()))
                    .map(Tag::name)
                    .collect(Collectors.toList()));
        }

        return "redirect:/dashboard";
    }
}