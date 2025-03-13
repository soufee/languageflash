package ci.ashamaz.languageflash.controller;

import ci.ashamaz.languageflash.model.User;
import ci.ashamaz.languageflash.model.WordProgress;
import ci.ashamaz.languageflash.service.UserService;
import ci.ashamaz.languageflash.service.WordProgressService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;

@Controller
@RequestMapping("/learn")
@Slf4j
public class LearnController {

    @Autowired
    private UserService userService;

    @Autowired
    private WordProgressService wordProgressService;

    @GetMapping
    public String learn(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/";
        }
        List<WordProgress> activeWords = wordProgressService.getActiveProgress(user.getId());
        if (activeWords.isEmpty()) {
            model.addAttribute("message", "Нет слов для изучения. Настройте программу на dashboard.");
            return "learn";
        }
        WordProgress nextWord = activeWords.get(0); // Простая логика выбора, позже можно улучшить
        model.addAttribute("word", nextWord.getWord());
        model.addAttribute("settings", userService.getSettings(user.getId()));
        return "learn";
    }

    @PostMapping("/update")
    public String updateProgress(@RequestParam("wordId") Long wordId,
                                 @RequestParam("knows") boolean knows,
                                 HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/";
        }
        wordProgressService.updateProgress(user.getId(), wordId, knows);
        return "redirect:/learn";
    }

    @GetMapping("/fast")
    public String fastLearn(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/";
        }
        List<WordProgress> activeWords = wordProgressService.getActiveProgress(user.getId());
        if (activeWords.isEmpty()) {
            model.addAttribute("message", "Нет слов для быстрого просмотра.");
            return "learnFast";
        }
        model.addAttribute("words", activeWords);
        model.addAttribute("settings", userService.getSettings(user.getId()));
        return "learnFast";
    }
}