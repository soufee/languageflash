package ci.ashamaz.languageflash.controller;

import ci.ashamaz.languageflash.model.*;
import ci.ashamaz.languageflash.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.*;

/**
 * Контроллер для отладки приложения
 * Только для разработки и тестирования
 */
@Controller
@RequestMapping("/debug")
@Slf4j
public class DebugController {

    @Autowired
    private UserService userService;

    @Autowired
    private WordProgressService wordProgressService;

    @Autowired
    private TextService textService;

    /**
     * Страница для тестирования API
     */
    @GetMapping("/test-api")
    public String testApiPage(Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        model.addAttribute("user", user);
        log.info("Открыта страница тестирования API пользователем {}", user != null ? user.getEmail() : "не авторизован");
        return "debug/test-api";
    }

    /**
     * Прямой доступ к словам из текстов (отладочная страница)
     */
    @GetMapping("/text-words")
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