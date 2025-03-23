package ci.ashamaz.languageflash.controller;

import ci.ashamaz.languageflash.model.*;
import ci.ashamaz.languageflash.service.LanguageService;
import ci.ashamaz.languageflash.service.TextService;
import ci.ashamaz.languageflash.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@Slf4j
public class TextController extends CommonControllerUtil {

    @Autowired
    private TextService textService;

    @Autowired
    private LanguageService languageService;

    @Autowired
    private UserService userService;


    @GetMapping("/texts")
    public String texts(HttpSession session, Model model,
                        @RequestParam(value = "language", required = false) String selectedLanguage,
                        @RequestParam(value = "tag", required = false) String selectedTag,
                        @RequestParam(value = "page", defaultValue = "0") int page,
                        @RequestParam(value = "size", defaultValue = "10") int size) {
        addAuthAttributes(session, model);
        User user = (User) session.getAttribute("user");

        String defaultLanguage = "English";
        if (user != null) {
            Map<String, Object> settings = userService.getSettings(user.getId());
            String programLanguage = (String) settings.get("language");
            defaultLanguage = programLanguage != null ? programLanguage : defaultLanguage;
        }
        String activeLanguage = selectedLanguage != null ? selectedLanguage : defaultLanguage;

        List<Language> activeLanguages = languageService.getAllLanguages().stream()
                .filter(Language::isActive)
                .collect(Collectors.toList());
        model.addAttribute("languages", activeLanguages);
        model.addAttribute("selectedLanguage", activeLanguage);

        model.addAttribute("tags", Tag.values());
        model.addAttribute("selectedTag", selectedTag);

        Pageable pageable = PageRequest.of(page, size);
        Page<Text> textPage = selectedTag != null
                ? textService.getActiveTextsByLanguageAndTag(activeLanguage, selectedTag, pageable)
                : textService.getActiveTextsByLanguage(activeLanguage, pageable);

        // Преобразуем теги в русский текст для каждого текста
        List<Text> textsWithRussianTags = textPage.getContent().stream().map(text -> {
            String russianTags = text.getTagsAsSet().stream()
                    .map(Tag::getRussianName)
                    .collect(Collectors.joining(", "));
            model.addAttribute("russianTags_" + text.getId(), russianTags);
            return text;
        }).collect(Collectors.toList());

        model.addAttribute("texts", textsWithRussianTags);
        model.addAttribute("page", textPage);

        Authentication auth = null;
        if (SecurityContextHolder.getContext() != null) {
            auth = SecurityContextHolder.getContext().getAuthentication();
        }
        boolean isAuthenticated = auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
        boolean isAdmin = isAuthenticated && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        model.addAttribute("isAuthenticated", isAuthenticated);
        model.addAttribute("isAdmin", isAdmin);

        log.info("Loaded texts page for language: {}, tag: {}, page: {}, size: {}, texts count: {}, isAuthenticated: {}, isAdmin: {}",
                activeLanguage, selectedTag, page, size, textPage.getTotalElements(), isAuthenticated, isAdmin);
        return "texts";
    }

    @GetMapping("/texts/{id}")
    @ResponseBody
    public Text getText(@PathVariable Long id) {
        return textService.getTextById(id);
    }

    @PostMapping("/admin/texts/add")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public Map<String, String> addText(@RequestBody Map<String, Object> requestBody) {
        log.info("Adding new text");
        try {
            Text text = new Text();
            text.setTitle((String) requestBody.get("title"));
            Language language = languageService.getLanguageByName((String) requestBody.get("language"));
            if (language == null) {
                throw new IllegalArgumentException("Язык " + requestBody.get("language") + " не найден");
            }
            text.setLanguage(language);
            text.setLevel((String) requestBody.get("level"));
            text.setTags((String) requestBody.get("tags"));
            text.setContent((String) requestBody.get("content")); // Принимаем HTML
            text.setTranslation((String) requestBody.get("translation")); // Принимаем HTML
            text.setActive(true);

            log.debug("Text data: title={}, language={}, level={}, tags={}, content={}, translation={}",
                    text.getTitle(), text.getLanguage().getName(), text.getLevel(), text.getTags(), text.getContent(), text.getTranslation());

            @SuppressWarnings("unchecked")
            List<Map<String, String>> wordsData = (List<Map<String, String>>) requestBody.get("words");
            List<TextWord> words = wordsData.stream().map(wordData -> {
                TextWord textWord = new TextWord();
                textWord.setWord(wordData.get("word"));
                textWord.setTranslation(wordData.get("translation"));
                textWord.setExampleSentence(wordData.get("exampleSentence"));
                textWord.setExampleTranslation(wordData.get("exampleTranslation"));
                textWord.setText(text);
                textWord.setLevel(text.getLevel());
                textWord.setLanguage(text.getLanguage());
                return textWord;
            }).collect(Collectors.toList());

            text.setWords(words);
            textService.saveText(text);

            log.info("Text added: {}", text.getTitle());
            return Map.of("status", "success");
        } catch (Exception e) {
            log.error("Error adding text: {}", e.getMessage(), e);
            return Map.of("status", "error", "message", e.getMessage());
        }
    }

    @PostMapping("/admin/texts/edit")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public Map<String, String> editText(@RequestBody Map<String, Object> requestBody) {
        log.info("Editing text");

        try {
            Long textId = Long.valueOf(requestBody.get("id").toString());
            Text existingText = textService.getTextById(textId);
            existingText.setTitle((String) requestBody.get("title"));
            Language language = languageService.getLanguageByName((String) requestBody.get("language"));
            if (language == null) {
                throw new IllegalArgumentException("Язык " + requestBody.get("language") + " не найден");
            }
            existingText.setLanguage(language);
            existingText.setLevel((String) requestBody.get("level"));
            existingText.setTags((String) requestBody.get("tags"));
            existingText.setContent((String) requestBody.get("content")); // Принимаем HTML
            existingText.setTranslation((String) requestBody.get("translation")); // Принимаем HTML

            log.debug("Updated text data: id={}, title={}, language={}, level={}, tags={}, content={}, translation={}",
                    existingText.getId(), existingText.getTitle(), existingText.getLanguage().getName(), existingText.getLevel(),
                    existingText.getTags(), existingText.getContent(), existingText.getTranslation());

            @SuppressWarnings("unchecked")
            List<Map<String, String>> wordsData = (List<Map<String, String>>) requestBody.get("words");
            List<TextWord> updatedWords = wordsData.stream().map(wordData -> {
                TextWord textWord = new TextWord();
                textWord.setWord(wordData.get("word"));
                textWord.setTranslation(wordData.get("translation"));
                textWord.setExampleSentence(wordData.get("exampleSentence"));
                textWord.setExampleTranslation(wordData.get("exampleTranslation"));
                textWord.setText(existingText);
                textWord.setLevel(existingText.getLevel());
                textWord.setLanguage(existingText.getLanguage());
                return textWord;
            }).collect(Collectors.toList());

            existingText.getWords().clear();
            existingText.getWords().addAll(updatedWords);

            textService.saveText(existingText);

            log.info("Text edited: {}", existingText.getTitle());
            return Map.of("status", "success");
        } catch (Exception e) {
            log.error("Error editing text: {}", e.getMessage(), e);
            return Map.of("status", "error", "message", e.getMessage());
        }
    }

    @PostMapping("/admin/texts/delete")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public Map<String, String> deleteText(@RequestBody Map<String, Long> requestBody) {
        Long textId = requestBody.get("textId");
        log.info("Soft deleting text: {}", textId);
        textService.softDeleteText(textId);
        return Map.of("status", "success");
    }
}