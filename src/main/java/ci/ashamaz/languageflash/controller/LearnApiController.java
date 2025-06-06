package ci.ashamaz.languageflash.controller;

import ci.ashamaz.languageflash.model.Tag;
import ci.ashamaz.languageflash.model.User;
import ci.ashamaz.languageflash.model.Word;
import ci.ashamaz.languageflash.model.WordProgress;
import ci.ashamaz.languageflash.service.UserService;
import ci.ashamaz.languageflash.service.WordProgressService;
import ci.ashamaz.languageflash.service.WordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/learn")
@Slf4j
public class LearnApiController {

    private final UserService userService;
    private final WordProgressService wordProgressService;
    private final WordService wordService;

    public LearnApiController(UserService userService,
                            WordProgressService wordProgressService,
                            WordService wordService) {
        this.userService = userService;
        this.wordProgressService = wordProgressService;
        this.wordService = wordService;
    }

    @PostMapping("/update")
    public ResponseEntity<Void> updateProgress(@RequestParam("wordId") Long wordId,
                                             @RequestParam("knows") boolean knows,
                                             @RequestParam(value = "forceLearned", required = false, defaultValue = "false") boolean forceLearned,
                                             HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        log.info("Updating progress for user: {}, wordId: {}, knows: {}, forceLearned: {}", user.getEmail(), wordId, knows, forceLearned);

        if (forceLearned) {
            WordProgress progress = wordProgressService.getProgress(user.getId(), wordId);
            progress.setKnowledgeFactor(0.0f);
            progress.setLearned(true);
            wordProgressService.save(progress);
        } else {
            wordProgressService.updateProgress(user.getId(), wordId, knows);
        }

        return ResponseEntity.ok().build();
    }

    @GetMapping("/next")
    public Map<String, Object> getNextWord(HttpSession session) {
        User user = (User) session.getAttribute("user");
        Map<String, Object> response = new HashMap<>();
        if (user == null) {
            response.put("error", "User not authenticated");
            return response;
        }
        List<WordProgress> activeWords = wordProgressService.getActiveProgress(user.getId());
        if (activeWords.isEmpty()) {
            response.put("word", null);
        } else {
            WordProgress nextWord = activeWords.get(0);
            response.put("word", Map.of(
                    "id", nextWord.getWord().getId(),
                    "word", nextWord.getWord().getWord(),
                    "translation", nextWord.getWord().getTranslation(),
                    "exampleSentence", nextWord.getWord().getExampleSentence(),
                    "exampleTranslation", nextWord.getWord().getExampleTranslation()
            ));
        }
        return response;
    }

    @PostMapping("/refill")
    public ResponseEntity<Map<String, Object>> refillWords(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            log.warn("RefillWords: User not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        Map<String, Object> settings = userService.getSettings(user.getId());
        String language = (String) settings.get("language");
        String minLevel = (String) settings.get("minLevel");
        List<String> tagList = (List<String>) settings.getOrDefault("tags", Collections.emptyList());
        int activeWordsCount = (int) settings.getOrDefault("activeWordsCount", 50);

        // Получаем только активные слова для программы обучения (PROGRAM)
        List<WordProgress> activeWords = wordProgressService.getActiveProgressForProgram(user.getId());
        log.info("RefillWords: Current active program words count: {}, Target count: {}", activeWords.size(), activeWordsCount);

        Map<String, Object> response = new HashMap<>();
        if (activeWords.size() < activeWordsCount) {
            List<Word> selectedWords = wordService.selectWordsForLearning(user.getId(), language, minLevel, tagList, activeWords.size());
            int wordsToAdd = Math.min(activeWordsCount - activeWords.size(), selectedWords.size());
            log.info("RefillWords: Available words to add: {}, Words to add: {}", selectedWords.size(), wordsToAdd);

            if (wordsToAdd > 0) {
                wordProgressService.initializeProgress(user.getId(), selectedWords.subList(0, wordsToAdd));
                log.info("RefillWords: Called initializeProgress for {} new words for user {}", wordsToAdd, user.getEmail());
            } else {
                log.warn("RefillWords: No new words available to add for user {}", user.getEmail());
            }
        } else {
            log.info("RefillWords: No refill needed, active words count is sufficient");
        }

        // Получаем обновленные списки слов (включая слова из текстов для отображения)
        List<WordProgress> updatedActiveWords = wordProgressService.getActiveProgress(user.getId());
        List<WordProgress> learnedWords = wordProgressService.getLearnedProgress(user.getId());

        response.put("activeCount", updatedActiveWords.size());
        response.put("learnedCount", learnedWords.size());
        response.put("activeWords", updatedActiveWords.stream().map(wp -> {
            Map<String, Object> wordData = new HashMap<>();
            wordData.put("id", wp.getWord().getId());
            wordData.put("word", wp.getWord().getWord());
            wordData.put("translation", wp.getWord().getTranslation());
            wordData.put("knowledgeFactor", wp.getKnowledgeFactor());
            wordData.put("source", wp.getSource().name());
            if (wp.getText() != null) {
                wordData.put("textTitle", wp.getText().getTitle());
            }
            return wordData;
        }).collect(Collectors.toList()));

        boolean showTagPrompt = activeWords.size() < activeWordsCount && tagList.size() < Tag.values().length;
        response.put("showTagPrompt", showTagPrompt);
        if (showTagPrompt) {
            response.put("availableTags", Arrays.stream(Tag.values())
                    .filter(tag -> !tagList.contains(tag.name()))
                    .map(Tag::name)
                    .collect(Collectors.toList()));
        }

        log.info("RefillWords response: activeCount={}, learnedCount={}, showTagPrompt={}",
                updatedActiveWords.size(), learnedWords.size(), showTagPrompt);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/active-words")
    public List<Map<String, Object>> getActiveWords(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Collections.emptyList();
        }
        return wordProgressService.getActiveProgress(user.getId()).stream()
                .map(wp -> {
                    Map<String, Object> wordData = new HashMap<>();
                    wordData.put("id", wp.getWord().getId());
                    wordData.put("word", wp.getWord().getWord());
                    wordData.put("translation", wp.getWord().getTranslation());
                    wordData.put("knowledgeFactor", wp.getKnowledgeFactor());
                    return wordData;
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/learned-words")
    public List<Map<String, Object>> getLearnedWords(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Collections.emptyList();
        }
        return wordProgressService.getLearnedProgress(user.getId()).stream()
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
    }

    @GetMapping("/text-words")
    public List<Map<String, Object>> getTextWords(HttpSession session, 
                                                 @RequestParam(value = "textId", required = false) Long textId) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Collections.emptyList();
        }
        
        List<WordProgress> textWordProgress;
        if (textId != null) {
            // Получаем слова из конкретного текста
            textWordProgress = wordProgressService.getTextProgressByTextId(user.getId(), textId);
        } else {
            // Получаем все слова из текстов
            textWordProgress = wordProgressService.getTextProgress(user.getId());
        }
        
        return textWordProgress.stream()
            .map(wp -> {
                Map<String, Object> wordData = new HashMap<>();
                wordData.put("id", wp.getWord().getId());
                wordData.put("word", wp.getWord().getWord());
                wordData.put("translation", wp.getWord().getTranslation());
                wordData.put("knowledgeFactor", wp.getKnowledgeFactor());
                wordData.put("learned", wp.isLearned());
                wordData.put("exampleSentence", wp.getWord().getExampleSentence());
                wordData.put("exampleTranslation", wp.getWord().getExampleTranslation());
                
                // Добавляем информацию о тексте
                if (wp.getText() != null) {
                    wordData.put("textId", wp.getText().getId());
                    wordData.put("textTitle", wp.getText().getTitle());
                }
                
                return wordData;
            })
            .collect(Collectors.toList());
    }

    @GetMapping("/text-titles")
    public List<Map<String, Object>> getTextTitles(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Collections.emptyList();
        }
        
        return wordProgressService.getTextsWithWords(user.getId()).stream()
            .map(text -> {
                Map<String, Object> textData = new HashMap<>();
                textData.put("id", text.getId());
                textData.put("title", text.getTitle());
                return textData;
            })
            .collect(Collectors.toList());
    }
} 