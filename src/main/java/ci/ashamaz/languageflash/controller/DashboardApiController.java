package ci.ashamaz.languageflash.controller;

import ci.ashamaz.languageflash.model.*;
import ci.ashamaz.languageflash.repository.WordRepository;
import ci.ashamaz.languageflash.service.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
@Slf4j
public class DashboardApiController {

    private final DashboardService dashboardService;
    private final WordRepository wordRepository;

    public DashboardApiController(DashboardService dashboardService, WordRepository wordRepository) {
        this.dashboardService = dashboardService;
        this.wordRepository = wordRepository;
    }

    @PostMapping("/settings")
    public ResponseEntity<Void> updateSettings(@RequestParam("userId") Long userId,
                                               @RequestBody Map<String, Object> settings) {
        log.info("=== updateSettings called ===");
        log.info("UserId: {}", userId);
        log.info("Settings received: {}", settings);
        
        try {
            ResponseEntity<Void> response = dashboardService.updateSettings(userId, settings);
            log.info("updateSettings completed with status: {}", response.getStatusCode());
            return response;
        } catch (Exception e) {
            log.error("Error in updateSettings: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/languages/levels")
    public ResponseEntity<List<String>> getLanguageLevels(@RequestParam("language") String language) {
        return dashboardService.getLanguageLevelsAsStrings(language);
    }

    @PostMapping("/settings/reset")
    public ResponseEntity<Void> resetSettings(@RequestParam("userId") Long userId) {
        return dashboardService.resetSettings(userId);
    }

    @GetMapping("/words/active")
    public ResponseEntity<List<WordProgress>> getActiveWords(@RequestParam("userId") Long userId) {
        return ResponseEntity.ok(dashboardService.getActiveWords(userId));
    }

    @GetMapping("/words/learned")
    public ResponseEntity<List<WordProgress>> getLearnedWords(@RequestParam("userId") Long userId) {
        return ResponseEntity.ok(dashboardService.getLearnedWords(userId));
    }

    @GetMapping("/settings")
    public ResponseEntity<Map<String, Object>> getSettings(@RequestParam("userId") Long userId) {
        return ResponseEntity.ok(dashboardService.getSettings(userId));
    }

    @PostMapping("/words/refill")
    public ResponseEntity<Void> refillWords(@RequestParam("userId") Long userId,
                                            @RequestParam("language") String language,
                                            @RequestParam("minLevel") String minLevel,
                                            @RequestParam(value = "tags", required = false) List<String> tags) {
        dashboardService.refillWords(userId, language, minLevel, tags);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/words/tags/add")
    public ResponseEntity<Void> addTag(@RequestParam("userId") Long userId,
                                       @RequestParam("tag") String tag) {
        return dashboardService.addTag(userId, tag);
    }

    @PostMapping("/words/remove")
    public ResponseEntity<Void> removeWord(@RequestParam("userId") Long userId,
                                           @RequestParam("wordId") Long wordId) {
        dashboardService.removeWord(userId, wordId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/words/progress")
    public ResponseEntity<Void> updateWordProgress(@RequestParam("userId") Long userId,
                                                   @RequestParam("wordId") Long wordId,
                                                   @RequestParam("knows") boolean knows) {
        dashboardService.updateWordProgress(userId, wordId, knows);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/words/custom")
    public ResponseEntity<List<WordProgress>> getCustomWords(@RequestParam("userId") Long userId) {
        return ResponseEntity.ok(dashboardService.getCustomWords(userId));
    }

    @GetMapping("/words/autocomplete")
    public ResponseEntity<List<String>> checkAutocomplete(@RequestParam("word") String word) {
        List<Word> words = wordRepository.findByWordStartingWith(word);
        List<String> suggestions = words.stream()
                .map(Word::getWord)
                .collect(Collectors.toList());
        return ResponseEntity.ok(suggestions);
    }

    @GetMapping("/words/duplicate")
    public ResponseEntity<Boolean> checkDuplicate(@RequestParam("word") String word,
                                                  @RequestParam("translation") String translation) {
        List<Word> words = wordRepository.findByWordStartingWith(word);
        boolean isDuplicate = words.stream()
                .anyMatch(w -> w.getWord().equals(word) && w.getTranslation().equals(translation));
        return ResponseEntity.ok(isDuplicate);
    }

    @PostMapping("/words/custom/add")
    public ResponseEntity<Void> addCustomWord(@RequestParam("word") String word,
                                              @RequestParam("translation") String translation,
                                              @RequestParam(value = "example", required = false) String example,
                                              @RequestParam(value = "exampleTranslation", required = false) String exampleTranslation,
                                              @RequestParam("userId") Long userId) {
        dashboardService.addCustomWord(word, translation, example, exampleTranslation, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/texts/words/count")
    public ResponseEntity<Long> getTextWordsCount(@RequestParam("textId") Long textId) {
        return ResponseEntity.ok(dashboardService.getTextWordsCount(textId));
    }

    @GetMapping("/texts/words/count/all")
    public ResponseEntity<Long> getAllTextWordsCount(@RequestParam("userId") Long userId) {
        return ResponseEntity.ok(dashboardService.getAllTextWordsCount(userId));
    }

    @GetMapping("/texts/words/learned/count")
    public ResponseEntity<Long> getLearnedWordsCount(@RequestParam("textId") Long textId,
                                                     @RequestParam("userId") Long userId) {
        return ResponseEntity.ok(dashboardService.getLearnedWordsCount(userId, textId));
    }

    @GetMapping("/texts/words")
    public ResponseEntity<List<Word>> getTextWords(@RequestParam("textId") Long textId) {
        return ResponseEntity.ok(dashboardService.getTextWords(textId));
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        log.info("Test endpoint called");
        return ResponseEntity.ok("Controller is working. Timestamp: " + System.currentTimeMillis());
    }
}