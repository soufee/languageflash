package ci.ashamaz.languageflash.controller;

import ci.ashamaz.languageflash.model.Level;
import ci.ashamaz.languageflash.service.AdminService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin")
@Slf4j
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public String redirectToUsers() {
        log.info("Handling GET /admin");
        return "redirect:/admin/users";
    }

    @GetMapping("/users")
    public String listUsers(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Handling GET /admin/users");
        model.addAllAttributes(adminService.getUserListData(userDetails.getUsername()));
        return "userList";
    }

    @GetMapping("/languages")
    public String listLanguages(Model model) {
        log.info("Handling GET /admin/languages");
        model.addAllAttributes(adminService.getLanguageListData());
        return "languages";
    }

    @GetMapping("/words")
    public String listWords(@RequestParam(required = false) String filter, Model model) {
        log.info("Handling GET /admin/words with filter: {}", filter);
        model.addAllAttributes(adminService.getWordListData(filter));
        return "adminWords";
    }

    @PostMapping("/words/add")
    public String addWord(@RequestParam String word,
                          @RequestParam String translation,
                          @RequestParam(required = false) String exampleSentence,
                          @RequestParam(required = false) String exampleTranslation,
                          @RequestParam Long languageId,
                          @RequestParam String level,
                          @RequestParam(required = false) List<String> tags) {
        log.info("Handling POST /admin/words/add");
        adminService.addWord(word, translation, exampleSentence, exampleTranslation, languageId, level, tags);
        return "redirect:/admin/words";
    }

    @PostMapping("/words/edit")
    public String editWord(@RequestParam Long id,
                           @RequestParam String word,
                           @RequestParam String translation,
                           @RequestParam(required = false) String exampleSentence,
                           @RequestParam(required = false) String exampleTranslation,
                           @RequestParam Long languageId,
                           @RequestParam String level,
                           @RequestParam(required = false) List<String> tags) {
        log.info("Handling POST /admin/words/edit for id: {}", id);
        adminService.editWord(id, word, translation, exampleSentence, exampleTranslation, languageId, level, tags);
        return "redirect:/admin/words";
    }

    @PostMapping("/languages")
    public String addLanguage(@RequestParam String name) {
        log.info("Handling POST /admin/languages with name: {}", name);
        adminService.addLanguage(name);
        return "redirect:/admin/languages";
    }

    @PostMapping("/languages/{id}/update")
    public String updateLanguage(@PathVariable Long id,
                                 @RequestParam String name,
                                 @RequestParam(required = false) Boolean active) {
        log.info("Handling POST /admin/languages/{} - name: {}, active: {}", id, name, active);
        adminService.updateLanguage(id, name, active != null ? active : false);
        return "redirect:/admin/languages";
    }

    @PostMapping("/languages/levels/update")
    public String updateLanguageLevel(@RequestParam Long id,
                                      @RequestParam Level level,
                                      @RequestParam(required = false) Boolean active) {
        log.info("Handling POST /admin/languages/levels/update - id: {}, level: {}, active: {}", id, level, active);
        adminService.updateLanguageLevel(id, level, active != null ? active : false);
        return "redirect:/admin/languages";
    }
}