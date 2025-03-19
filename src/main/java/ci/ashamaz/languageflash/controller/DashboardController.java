package ci.ashamaz.languageflash.controller;

import ci.ashamaz.languageflash.model.*;
import ci.ashamaz.languageflash.repository.WordRepository;
import ci.ashamaz.languageflash.service.*;
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
import java.time.LocalDateTime;
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

    @Autowired
    private WordRepository wordRepository;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping(value = "/dashboard", produces = "text/html;charset=UTF-8")
    public String dashboard(Model model, HttpSession session) throws JsonProcessingException {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/";
        }
        log.info("Loading dashboard for user: {}", user.getEmail());
        model.addAttribute("user", user);
        model.addAttribute("userId", user.getId());

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

        List<AbstractWord> customWords = wordRepository.findCustomWordsByUserId(user.getId());
        model.addAttribute("customWordsCount", customWords.size());

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

        // Передаём реальное количество текущих активных слов
        List<WordProgress> currentActiveWords = wordProgressService.getActiveProgress(user.getId());
        int currentActiveCount = currentActiveWords.size();
        initializeLearningWords(user, language, minLevel, tagList, activeWordsCount, currentActiveCount, model);

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
                .filter(wp -> !(wp.getWord() instanceof CustomWord))
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

    @GetMapping("/dashboard/learned-words-json")
    @ResponseBody
    public String getLearnedWordsJson(HttpSession session) throws JsonProcessingException {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "[]";
        }
        List<WordProgress> learnedProgress = wordProgressService.getLearnedProgress(user.getId());
        List<Map<String, Object>> learnedWordsJson = learnedProgress.stream()
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
        return objectMapper.writeValueAsString(learnedWordsJson);
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
            if (currentActiveCount < activeWordsCount) {
                List<Word> selectedWords = wordService.selectWordsForLearning(user.getId(), language, minLevel, tagList, currentActiveCount);
                int wordsToAdd = Math.min(activeWordsCount - currentActiveCount, selectedWords.size());
                if (wordsToAdd > 0) {
                    wordProgressService.initializeProgress(user.getId(), selectedWords.subList(0, wordsToAdd));
                    log.info("Initialized {} new words for user {}", wordsToAdd, user.getEmail());
                }
                model.addAttribute("selectedWords", selectedWords.subList(0, wordsToAdd));
            } else {
                log.info("No new words needed, current active count: {} meets or exceeds target: {}", currentActiveCount, activeWordsCount);
            }

            List<WordProgress> updatedActiveWords = wordProgressService.getActiveProgress(user.getId());
            if (updatedActiveWords.size() < activeWordsCount && tagList.size() < Tag.values().length) {
                model.addAttribute("showTagPrompt", true);
                model.addAttribute("availableTags", Arrays.stream(Tag.values())
                        .filter(tag -> !tagList.contains(tag.name()))
                        .collect(Collectors.toList()));
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

        List<WordProgress> activeWords = wordProgressService.getActiveProgress(user.getId());
        int currentActiveCount = activeWords.size();

        if (currentActiveCount < activeWordsCount) {
            List<Word> selectedWords = wordService.selectWordsForLearning(user.getId(), language, minLevel, updatedTags, currentActiveCount);
            int wordsToAdd = Math.min(activeWordsCount - currentActiveCount, selectedWords.size());
            if (wordsToAdd > 0) {
                wordProgressService.initializeProgress(user.getId(), selectedWords.subList(0, wordsToAdd));
            }
        }

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

    @PostMapping("/dashboard/remove-word")
    @ResponseBody
    public Map<String, String> removeWord(@RequestBody Map<String, Long> requestBody, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            throw new IllegalStateException("Пользователь не авторизован");
        }
        Long wordId = requestBody.get("wordId");
        if (wordId == null) {
            throw new IllegalArgumentException("Идентификатор слова не указан");
        }

        WordProgress progress = wordProgressService.getProgress(user.getId(), wordId);
        progress.setKnowledgeFactor(0.0f);
        progress.setLearned(true);
        wordProgressService.save(progress);

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        return response;
    }

    @PostMapping("/dashboard/update-progress")
    @ResponseBody
    public Map<String, String> updateProgress(@RequestBody Map<String, Object> requestBody, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            throw new IllegalStateException("Пользователь не авторизован");
        }
        Long wordId = Long.valueOf(requestBody.get("wordId").toString());
        Boolean knows = Boolean.valueOf(requestBody.get("knows").toString());

        wordProgressService.updateProgress(user.getId(), wordId, knows);

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        return response;
    }

    @GetMapping("/dashboard/custom-words")
    @ResponseBody
    public List<Map<String, Object>> getCustomWords(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Collections.emptyList();
        }
        List<AbstractWord> customWords = wordRepository.findCustomWordsByUserId(user.getId());
        return customWords.stream()
                .map(word -> {
                    Map<String, Object> wordData = new HashMap<>();
                    wordData.put("id", word.getId());
                    wordData.put("word", word.getWord());
                    wordData.put("translation", word.getTranslation());
                    wordData.put("exampleSentence", word.getExampleSentence());
                    wordData.put("exampleTranslation", word.getExampleTranslation());
                    return wordData;
                })
                .collect(Collectors.toList());
    }

    @PostMapping("/dashboard/custom-words/check-autocomplete")
    @ResponseBody
    public Map<String, Object> checkAutocomplete(@RequestBody Map<String, String> requestBody, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            throw new IllegalStateException("Пользователь не авторизован");
        }
        String word = requestBody.get("word");

        Map<String, Object> response = new HashMap<>();

        List<Word> foundWords = wordRepository.findByWordStartingWith(word.toLowerCase());
        log.info("Found {} words starting with '{}'", foundWords.size(), word);
        Optional<Word> existingWord = foundWords.stream()
                .filter(w -> w.getWord().equalsIgnoreCase(word))
                .findFirst();
        if (existingWord.isPresent()) {
            log.info("Autocomplete found for word '{}': translation='{}', example='{}', exampleTranslation='{}'",
                    word, existingWord.get().getTranslation(), existingWord.get().getExampleSentence(), existingWord.get().getExampleTranslation());
            response.put("status", "autocomplete");
            response.put("translation", existingWord.get().getTranslation());
            response.put("exampleSentence", existingWord.get().getExampleSentence());
            response.put("exampleTranslation", existingWord.get().getExampleTranslation());
            return response;
        }

        log.info("No autocomplete found for word '{}'", word);
        response.put("status", "ok");
        return response;
    }

    @PostMapping("/dashboard/custom-words/check-duplicates")
    @ResponseBody
    public Map<String, Object> checkDuplicates(@RequestBody Map<String, String> requestBody, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            throw new IllegalStateException("Пользователь не авторизован");
        }
        String word = requestBody.get("word");
        String translation = requestBody.get("translation");

        Map<String, Object> response = new HashMap<>();
        List<WordProgress> customWords = wordProgressService.getCustomWordsProgress(user.getId());

        Optional<WordProgress> duplicate = customWords.stream()
                .filter(wp -> wp.getWord().getWord().equalsIgnoreCase(word))
                .findFirst();
        if (duplicate.isPresent()) {
            if (duplicate.get().getWord().getTranslation().equalsIgnoreCase(translation)) {
                response.put("status", "error");
                response.put("message", "Это слово с переводом уже присутствует в вашем словаре");
            } else {
                response.put("status", "warning");
                response.put("message", "Вы уверены, что хотите сохранить слово '" + word + "' с переводом '" + translation +
                        "'? Это слово уже присутствует в вашем словаре с переводом '" + duplicate.get().getWord().getTranslation() +
                        "'. Это может запутать вас во время изучения. Рекомендуем омонимы изучать в разных словарях.");
                response.put("existingTranslation", duplicate.get().getWord().getTranslation());
            }
            return response;
        }

        response.put("status", "ok");
        return response;
    }

    @PostMapping("/dashboard/custom-words/add")
    @ResponseBody
    public Map<String, Object> addCustomWord(@RequestBody Map<String, String> requestBody, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            throw new IllegalStateException("Пользователь не авторизован");
        }
        String word = requestBody.get("word");
        String translation = requestBody.get("translation");
        String exampleSentence = requestBody.get("exampleSentence") != null && !requestBody.get("exampleSentence").isEmpty()
                ? requestBody.get("exampleSentence")
                : "";
        String exampleTranslation = requestBody.get("exampleTranslation") != null && !requestBody.get("exampleTranslation").isEmpty()
                ? requestBody.get("exampleTranslation")
                : "";

        Map<String, Object> response = new HashMap<>();
        List<WordProgress> customWords = wordProgressService.getCustomWordsProgress(user.getId());

        Optional<WordProgress> duplicate = customWords.stream()
                .filter(wp -> wp.getWord().getWord().equalsIgnoreCase(word))
                .findFirst();
        if (duplicate.isPresent()) {
            if (duplicate.get().getWord().getTranslation().equalsIgnoreCase(translation)) {
                response.put("status", "error");
                response.put("message", "Это слово с переводом уже присутствует в вашем словаре");
                return response;
            } else if (!requestBody.containsKey("force")) {
                response.put("status", "warning");
                response.put("message", "Вы уверены, что хотите сохранить слово '" + word + "' с переводом '" + translation +
                        "'? Это слово уже присутствует в вашем словаре с переводом '" + duplicate.get().getWord().getTranslation() +
                        "'. Это может запутать вас во время изучения. Рекомендуем омонимы изучать в разных словарях.");
                response.put("existingTranslation", duplicate.get().getWord().getTranslation());
                return response;
            }
        }

        CustomWord customWord = new CustomWord();
        customWord.setWord(word);
        customWord.setTranslation(translation);
        customWord.setExampleSentence(exampleSentence);
        customWord.setExampleTranslation(exampleTranslation);
        customWord.setUser(user);
        wordRepository.save(customWord);

        response.put("status", "success");
        return response;
    }
}