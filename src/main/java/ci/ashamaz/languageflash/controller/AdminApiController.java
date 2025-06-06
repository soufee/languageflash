package ci.ashamaz.languageflash.controller;

import ci.ashamaz.languageflash.model.*;
import ci.ashamaz.languageflash.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;



@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminApiController {

    private final AdminService adminService;

    @Value("${support.email:support@languageflash.com}")
    private String supportEmail;

    public AdminApiController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public Page<User> listUsers(Pageable pageable) {
        return adminService.getAllUsers(pageable);
    }

    @GetMapping("/users/search")
    public Page<User> searchUsers(@RequestParam("email") String email, Pageable pageable) {
        return adminService.searchUsersByEmail(email, pageable);
    }

    @PostMapping("/users/block")
    public ResponseEntity<Void> blockUser(@RequestParam("userId") Long userId,
                                          @RequestParam("blocked") boolean blocked) {
        adminService.blockUser(userId, blocked, supportEmail);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/users/toggle-admin")
    public ResponseEntity<Void> toggleAdmin(@RequestParam("userId") Long userId,
                                            @RequestParam("isAdmin") boolean isAdmin) {
        adminService.toggleAdmin(userId, isAdmin, supportEmail);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/languages")
    public List<Language> listLanguages() {
        return adminService.getAllLanguages();
    }

    @PostMapping("/languages")
    public ResponseEntity<Void> addLanguage(@RequestParam("name") String name) {
        adminService.addLanguage(name);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/languages/update")
    public ResponseEntity<Void> updateLanguage(@RequestParam("id") Long id,
                                               @RequestParam("name") String name,
                                               @RequestParam("active") boolean active) {
        adminService.updateLanguage(id, name, active);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/languages/levels/update")
    public ResponseEntity<Void> updateLanguageLevel(@RequestParam("id") Long id,
                                                    @RequestParam("level") Level level,
                                                    @RequestParam("active") boolean active) {
        adminService.updateLanguageLevel(id, level, active);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/words")
    public Page<Word> listWords(@RequestParam(value = "filter", required = false) String filter,
                                @RequestParam(value = "language", required = false) String language,
                                @RequestParam(value = "level", required = false) String level,
                                Pageable pageable) {
        return adminService.getFilteredWords(filter, language, level, pageable);
    }

    @PostMapping("/words/add")
    public ResponseEntity<Map<String, Object>> addWord(@RequestParam("word") String word,
                                                       @RequestParam("translation") String translation,
                                                       @RequestParam("exampleSentence") String exampleSentence,
                                                       @RequestParam("exampleTranslation") String exampleTranslation,
                                                       @RequestParam("languageId") Long languageId,
                                                       @RequestParam("level") String level,
                                                       @RequestParam(value = "tags", required = false) List<String> tags) {
        return ResponseEntity.ok(adminService.addWord(word, translation, exampleSentence, exampleTranslation, languageId, level, tags));
    }

    @PostMapping("/words/edit")
    public ResponseEntity<Map<String, Object>> editWord(@RequestParam("id") Long id,
                                                        @RequestParam("word") String word,
                                                        @RequestParam("translation") String translation,
                                                        @RequestParam("exampleSentence") String exampleSentence,
                                                        @RequestParam("exampleTranslation") String exampleTranslation,
                                                        @RequestParam("languageId") Long languageId,
                                                        @RequestParam("level") String level,
                                                        @RequestParam(value = "tags", required = false) List<String> tags) {
        return ResponseEntity.ok(adminService.editWord(id, word, translation, exampleSentence, exampleTranslation, languageId, level, tags));
    }
}