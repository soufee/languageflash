package ci.ashamaz.languageflash.controller;

import ci.ashamaz.languageflash.model.Language;
import ci.ashamaz.languageflash.model.Text;
import ci.ashamaz.languageflash.model.TextWord;
import ci.ashamaz.languageflash.model.User;
import ci.ashamaz.languageflash.service.LanguageService;
import ci.ashamaz.languageflash.service.TextService;
import ci.ashamaz.languageflash.service.WordProgressService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/texts")
@Slf4j
public class TextApiController {

    private static final String FIELD_TRANSLATION = "translation";
    private static final String FIELD_STATUS = "status";
    private static final String STATUS_SUCCESS = "success";
    private static final String STATUS_ERROR = "error";
    private static final String FIELD_MESSAGE = "message";
    private static final String FIELD_LANGUAGE = "language";

    private final TextService textService;
    private final LanguageService languageService;
    private final WordProgressService wordProgressService;

    @Autowired
    public TextApiController(TextService textService,
                            LanguageService languageService,
                            WordProgressService wordProgressService) {
        this.textService = textService;
        this.languageService = languageService;
        this.wordProgressService = wordProgressService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Text> getText(@PathVariable Long id) {
        Text text = textService.getTextById(id);
        if (text == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(text);
    }

    @PostMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> addText(@RequestBody Map<String, Object> requestBody) {
        log.info("Adding new text");
        try {
            Text text = new Text();
            text.setTitle((String) requestBody.get("title"));
            Language language = languageService.getLanguageByName((String) requestBody.get(FIELD_LANGUAGE));
            if (language == null) {
                throw new IllegalArgumentException("Язык " + requestBody.get(FIELD_LANGUAGE) + " не найден");
            }
            text.setLanguage(language);
            text.setLevel((String) requestBody.get("level"));
            text.setTags((String) requestBody.get("tags"));
            text.setContent((String) requestBody.get("content"));
            text.setTranslation((String) requestBody.get(FIELD_TRANSLATION));
            text.setActive(true);

            log.debug("Text data: title={}, language={}, level={}, tags={}, content={}, translation={}",
                    text.getTitle(), text.getLanguage().getName(), text.getLevel(), text.getTags(), text.getContent(), text.getTranslation());

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> wordsData = (List<Map<String, Object>>) requestBody.get("words");
            List<TextWord> words = wordsData.stream().map(wordData -> {
                TextWord textWord = new TextWord();
                textWord.setWord(convertToString(wordData.get("word")));
                textWord.setTranslation(convertToString(wordData.get(FIELD_TRANSLATION)));
                textWord.setExampleSentence(convertToString(wordData.get("exampleSentence")));
                textWord.setExampleTranslation(convertToString(wordData.get("exampleTranslation")));
                textWord.setText(text);
                textWord.setLevel(text.getLevel());
                textWord.setLanguage(text.getLanguage());
                textWord.setActive(true);
                return textWord;
            }).collect(Collectors.toList());

            text.setWords(words);
            textService.saveText(text);

            log.info("Text added: {}", text.getTitle());
            return ResponseEntity.ok(Map.of(FIELD_STATUS, STATUS_SUCCESS));
        } catch (Exception e) {
            log.error("Error adding text: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(FIELD_STATUS, STATUS_ERROR, FIELD_MESSAGE, e.getMessage()));
        }
    }

    @PostMapping("/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> editText(@RequestBody Map<String, Object> requestBody) {
        log.info("Editing text");
        try {
            Long textId = Long.valueOf(requestBody.get("id").toString());
            Text existingText = textService.getTextById(textId);
            
            updateTextBasicInfo(existingText, requestBody);
            updateTextWords(existingText, requestBody);
            
            textService.saveText(existingText);
            log.info("Text edited: {}", existingText.getTitle());
            return ResponseEntity.ok(Map.of(FIELD_STATUS, STATUS_SUCCESS));
        } catch (Exception e) {
            log.error("Error editing text: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(FIELD_STATUS, STATUS_ERROR, FIELD_MESSAGE, e.getMessage()));
        }
    }

    @PostMapping("/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteText(@RequestBody Map<String, Long> requestBody) {
        Long textId = requestBody.get("textId");
        log.info("Soft deleting text: {}", textId);
        textService.softDeleteText(textId);
        return ResponseEntity.ok(Map.of(FIELD_STATUS, STATUS_SUCCESS));
    }

    @PostMapping("/take-to-work")
    public ResponseEntity<Map<String, Object>> takeTextToWork(@RequestBody Map<String, Long> requestBody, HttpSession session) {
        User user = (User) session.getAttribute("user");
        Map<String, Object> response = new HashMap<>();
        
        if (user == null) {
            response.put(FIELD_STATUS, STATUS_ERROR);
            response.put(FIELD_MESSAGE, "Пользователь не авторизован");
            return ResponseEntity.status(401).body(response);
        }

        Long textId = requestBody.get("textId");
        if (textId == null) {
            response.put(FIELD_STATUS, STATUS_ERROR);
            response.put(FIELD_MESSAGE, "Идентификатор текста не указан");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            Text text = textService.getTextById(textId);
            wordProgressService.initializeTextProgress(user.getId(), textId);
            
            response.put(FIELD_STATUS, STATUS_SUCCESS);
            response.put("wordCount", text.getWords().size());
            log.info("Text {} taken to work by user {}", text.getTitle(), user.getEmail());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error taking text to work: {}", e.getMessage(), e);
            response.put(FIELD_STATUS, STATUS_ERROR);
            response.put(FIELD_MESSAGE, "Ошибка при взятии текста в работу: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    private void updateTextBasicInfo(Text text, Map<String, Object> requestBody) {
        text.setTitle((String) requestBody.get("title"));
        Language language = languageService.getLanguageByName((String) requestBody.get(FIELD_LANGUAGE));
        if (language == null) {
            throw new IllegalArgumentException("Язык " + requestBody.get(FIELD_LANGUAGE) + " не найден");
        }
        text.setLanguage(language);
        text.setLevel((String) requestBody.get("level"));
        text.setTags((String) requestBody.get("tags"));
        text.setContent((String) requestBody.get("content"));
        text.setTranslation((String) requestBody.get(FIELD_TRANSLATION));
    }

    private void updateTextWords(Text text, Map<String, Object> requestBody) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> wordsData = (List<Map<String, Object>>) requestBody.get("words");
        List<Long> deletedWordIds = extractDeletedWordIds(requestBody);
        
        if (!deletedWordIds.isEmpty()) {
            handleDeletedWords(text, deletedWordIds);
        }
        
        if (wordsData != null) {
            updateExistingWords(text, wordsData);
        }
    }

    private List<Long> extractDeletedWordIds(Map<String, Object> requestBody) {
        @SuppressWarnings("unchecked")
        List<Object> rawDeletedWordIds = (List<Object>) requestBody.get("deletedWordIds");
        List<Long> deletedWordIds = new ArrayList<>();
        
        if (rawDeletedWordIds != null && !rawDeletedWordIds.isEmpty()) {
            log.info("Raw deletedWordIds: {}", rawDeletedWordIds);
            
            for (Object idObj : rawDeletedWordIds) {
                try {
                    if (idObj instanceof Integer) {
                        deletedWordIds.add(((Integer) idObj).longValue());
                    } else if (idObj instanceof Long) {
                        deletedWordIds.add((Long) idObj);
                    } else if (idObj instanceof String) {
                        deletedWordIds.add(Long.valueOf((String) idObj));
                    } else if (idObj instanceof Number) {
                        deletedWordIds.add(((Number) idObj).longValue());
                    }
                } catch (Exception e) {
                    log.warn("Failed to convert to Long: {}", idObj, e);
                }
            }
        }
        
        log.info("Processed deletedWordIds: {}", deletedWordIds);
        return deletedWordIds;
    }

    private void handleDeletedWords(Text text, List<Long> deletedWordIds) {
        log.info("Marking words as inactive (logical deletion): {}", deletedWordIds);
        
        // Отсоединяем слова от текста в WordProgress для пользователей, которые уже добавили эти слова
        wordProgressService.detachWordsFromText(text.getId(), deletedWordIds);
        
        // Маркируем слова как неактивные
        text.getWords().forEach(word -> {
            if (deletedWordIds.contains(word.getId())) {
                word.setActive(false);
                log.debug("Marking word as inactive: {} (id={})", word.getWord(), word.getId());
            }
        });
    }

    private void updateExistingWords(Text text, List<Map<String, Object>> wordsData) {
        List<TextWord> existingWords = new ArrayList<>(text.getWords());
        
        for (Map<String, Object> wordData : wordsData) {
            TextWord textWord = createOrUpdateWord(text, wordData, existingWords);
            if (textWord != null) {
                updateWordData(textWord, wordData, text);
            }
        }
    }

    private TextWord createOrUpdateWord(Text text, Map<String, Object> wordData, List<TextWord> existingWords) {
        Object wordIdObj = wordData.get("id");
        boolean isNewWord = wordIdObj == null || (wordIdObj instanceof String && ((String)wordIdObj).isEmpty());
        
        if (!isNewWord) {
            Long wordId = convertToLong(wordIdObj);
            if (wordId == null) {
                log.warn("Invalid word ID type: {}", wordIdObj.getClass().getName());
                return null;
            }
            
            return existingWords.stream()
                    .filter(w -> w.getId().equals(wordId))
                    .findFirst()
                    .orElseGet(() -> {
                        TextWord newWord = new TextWord();
                        newWord.setText(text);
                        return newWord;
                    });
        } else {
            TextWord newWord = new TextWord();
            newWord.setText(text);
            text.getWords().add(newWord);
            return newWord;
        }
    }

    private Long convertToLong(Object obj) {
        if (obj instanceof Integer) {
            return ((Integer) obj).longValue();
        } else if (obj instanceof Long) {
            return (Long) obj;
        } else if (obj instanceof String) {
            return Long.valueOf((String) obj);
        } else if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }
        return null;
    }

    private void updateWordData(TextWord textWord, Map<String, Object> wordData, Text text) {
        textWord.setWord(convertToString(wordData.get("word")));
        textWord.setTranslation(convertToString(wordData.get(FIELD_TRANSLATION)));
        textWord.setExampleSentence(convertToString(wordData.get("exampleSentence")));
        textWord.setExampleTranslation(convertToString(wordData.get("exampleTranslation")));
        textWord.setLevel(text.getLevel());
        textWord.setLanguage(text.getLanguage());
        textWord.setActive(true);
    }
    
    /**
     * Вспомогательный метод для преобразования объекта в строку
     * @param obj объект для преобразования
     * @return строковое представление объекта или пустая строка, если объект null
     */
    private String convertToString(Object obj) {
        if (obj == null) {
            return "";
        }
        return obj.toString();
    }
} 