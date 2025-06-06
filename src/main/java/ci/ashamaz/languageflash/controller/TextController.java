package ci.ashamaz.languageflash.controller;

import ci.ashamaz.languageflash.model.Language;
import ci.ashamaz.languageflash.model.Text;
import ci.ashamaz.languageflash.model.User;
import ci.ashamaz.languageflash.model.WordProgress;
import ci.ashamaz.languageflash.service.LanguageService;
import ci.ashamaz.languageflash.service.TextService;
import ci.ashamaz.languageflash.service.UserService;
import ci.ashamaz.languageflash.service.WordProgressService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/texts")
@Slf4j
public class TextController {

    private final TextService textService;
    private final LanguageService languageService;
    private final UserService userService;
    private final WordProgressService wordProgressService;

    @Autowired
    public TextController(TextService textService,
                         LanguageService languageService,
                         UserService userService,
                         WordProgressService wordProgressService) {
        this.textService = textService;
        this.languageService = languageService;
        this.userService = userService;
        this.wordProgressService = wordProgressService;
    }

    @GetMapping
    public String texts(@RequestParam(required = false) String language,
                       @RequestParam(required = false) String tag,
                       @RequestParam(defaultValue = "0") int page,
                       Model model,
                       HttpSession session) {
        User user = (User) session.getAttribute("user");
        List<Language> languages = languageService.getAllLanguages();
        List<String> allTags = textService.getAllTags();
        
        model.addAttribute("settings", user != null ? user.getSettings() : null);
        model.addAttribute("languages", languages);
        model.addAttribute("selectedLanguage", language);
        model.addAttribute("allTags", allTags);
        model.addAttribute("isAuthenticated", user != null);
        
        // Добавляем логику получения текстов
        if (language != null && !language.isEmpty()) {
            Page<Text> textsPage;
            if (tag != null && !tag.isEmpty()) {
                textsPage = textService.getActiveTextsByLanguageAndTag(language, tag, PageRequest.of(page, 10));
            } else {
                textsPage = textService.getActiveTextsByLanguage(language, PageRequest.of(page, 10));
            }
            model.addAttribute("texts", textsPage.getContent());
            model.addAttribute("page", textsPage);
            model.addAttribute("selectedTag", tag);
        }
        
        return "texts";
    }

    @GetMapping("/{id}")
    public String viewText(@PathVariable Long id, Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        Text text = textService.getTextById(id);
        
        if (text == null) {
            return "redirect:/texts";
        }
        
        boolean isInProgress = user != null && wordProgressService.isTextInProgress(user.getId(), id);
        
        model.addAttribute("text", text);
        model.addAttribute("isInProgress", isInProgress);
        model.addAttribute("isAuthenticated", user != null);
        
        return "text";
    }

    @GetMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    public String showAddTextForm(Model model) {
        model.addAttribute("languages", languageService.getAllLanguages());
        return "addText";
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String showEditTextForm(@PathVariable Long id, Model model) {
        Text text = textService.getTextById(id);
        if (text == null) {
            return "redirect:/texts";
        }
        
        model.addAttribute("text", text);
        model.addAttribute("languages", languageService.getAllLanguages());
        return "editText";
    }

    @GetMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String showDeleteTextForm(@PathVariable Long id, Model model) {
        Text text = textService.getTextById(id);
        if (text == null) {
            return "redirect:/texts";
        }
        
        model.addAttribute("text", text);
        return "deleteText";
    }
    
    /**
     * Прямой доступ к словам из текстов (отладочная страница)
     */
    @GetMapping("/debug")
    public String getTextWordsDebug(Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            log.warn("getTextWordsDebug: User not authenticated");
            model.addAttribute("error", "Пользователь не авторизован");
            return "error";
        }
        
        try {
            log.info("getTextWordsDebug: Отладка слов из текстов для пользователя id={}, email={}", user.getId(), user.getEmail());
            // Получаем прогресс по словам из текстов
            List<WordProgress> textProgress = wordProgressService.getTextProgress(user.getId());
            if (textProgress == null) {
                log.error("getTextWordsDebug: textProgress is null for user {}", user.getEmail());
                textProgress = Collections.emptyList();
            }
            log.info("getTextWordsDebug: Found {} text words for user {}", textProgress.size(), user.getEmail());
            
            // Получаем список текстов
            List<Text> texts = wordProgressService.getTextsWithWords(user.getId());
            if (texts == null) {
                log.error("getTextWordsDebug: texts is null for user {}", user.getEmail());
                texts = Collections.emptyList();
            }
            log.info("getTextWordsDebug: Found {} texts with words for user {}", texts.size(), user.getEmail());
            
            // Добавляем данные в модель
            model.addAttribute("textWords", textProgress);
            model.addAttribute("texts", texts);
            model.addAttribute("user", user);
            
            return "debug/text-words-debug";
        } catch (Exception e) {
            log.error("getTextWordsDebug: Unexpected error: {}", e.getMessage(), e);
            model.addAttribute("error", "Произошла ошибка при загрузке данных: " + e.getMessage());
            model.addAttribute("stackTrace", e.getStackTrace());
            return "error";
        }
    }
}