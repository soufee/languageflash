package ci.ashamaz.languageflash.controller;

import ci.ashamaz.languageflash.model.*;
import ci.ashamaz.languageflash.repository.WordRepository;
import ci.ashamaz.languageflash.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DashboardApiControllerTest {

    @Mock
    private DashboardService dashboardService;

    @Mock
    private WordRepository wordRepository;

    @InjectMocks
    private DashboardApiController dashboardApiController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testUpdateSettings() {
        // Подготовка данных
        Long userId = 1L;
        Map<String, Object> settings = new HashMap<>();
        settings.put("knowThreshold", 0.1);
        settings.put("flashSpeed", 1000);
        settings.put("tags", Collections.emptyList());

        // Настройка моков
        when(dashboardService.updateSettings(userId, settings)).thenReturn(ResponseEntity.ok().build());

        // Вызов тестируемого метода
        ResponseEntity<Void> response = dashboardApiController.updateSettings(userId, settings);

        // Проверка результатов
        assertEquals(200, response.getStatusCodeValue());
        verify(dashboardService).updateSettings(userId, settings);
    }

    @Test
    void testUpdateSettings_UserNotFound() {
        // Подготовка данных
        Long userId = 1L;
        Map<String, Object> settings = new HashMap<>();

        // Настройка моков
        when(dashboardService.updateSettings(userId, settings)).thenReturn(ResponseEntity.notFound().build());

        // Вызов тестируемого метода
        ResponseEntity<Void> response = dashboardApiController.updateSettings(userId, settings);

        // Проверка результатов
        assertEquals(404, response.getStatusCodeValue());
        verify(dashboardService).updateSettings(userId, settings);
    }

    @Test
    void testGetLanguageLevels() {
        // Подготовка данных
        String language = "English";
        List<String> expectedLevels = Arrays.asList("A1", "A2");

        // Настройка моков
        when(dashboardService.getLanguageLevelsAsStrings(language)).thenReturn(ResponseEntity.ok(expectedLevels));

        // Вызов тестируемого метода
        ResponseEntity<List<String>> response = dashboardApiController.getLanguageLevels(language);

        // Проверка результатов
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals("A1", response.getBody().get(0));
        assertEquals("A2", response.getBody().get(1));
        verify(dashboardService).getLanguageLevelsAsStrings(language);
    }

    @Test
    void testGetLanguageLevels_LanguageNotFound() {
        // Подготовка данных
        String language = "NonExistentLanguage";

        // Настройка моков
        when(dashboardService.getLanguageLevelsAsStrings(language)).thenReturn(ResponseEntity.notFound().build());

        // Вызов тестируемого метода
        ResponseEntity<List<String>> response = dashboardApiController.getLanguageLevels(language);

        // Проверка результатов
        assertEquals(404, response.getStatusCodeValue());
        verify(dashboardService).getLanguageLevelsAsStrings(language);
    }

    @Test
    void testGetActiveWords() {
        // Подготовка данных
        Long userId = 1L;
        List<WordProgress> activeWords = new ArrayList<>();
        Word word1 = new Word();
        word1.setId(1L);
        word1.setWord("test");
        word1.setTranslation("тест");
        WordProgress progress1 = new WordProgress();
        progress1.setWord(word1);
        progress1.setKnowledgeFactor(0.5f);
        activeWords.add(progress1);

        // Настройка моков
        when(dashboardService.getActiveWords(userId)).thenReturn(activeWords);

        // Вызов тестируемого метода
        ResponseEntity<List<WordProgress>> response = dashboardApiController.getActiveWords(userId);

        // Проверка результатов
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(dashboardService).getActiveWords(userId);
    }

    @Test
    void testGetLearnedWords() {
        // Подготовка данных
        Long userId = 1L;
        List<WordProgress> learnedWords = new ArrayList<>();
        Word word1 = new Word();
        word1.setId(1L);
        word1.setWord("test");
        word1.setTranslation("тест");
        WordProgress progress1 = new WordProgress();
        progress1.setWord(word1);
        progress1.setKnowledgeFactor(1.0f);
        learnedWords.add(progress1);

        // Настройка моков
        when(dashboardService.getLearnedWords(userId)).thenReturn(learnedWords);

        // Вызов тестируемого метода
        ResponseEntity<List<WordProgress>> response = dashboardApiController.getLearnedWords(userId);

        // Проверка результатов
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(dashboardService).getLearnedWords(userId);
    }

    @Test
    void testResetSettings() {
        // Подготовка данных
        Long userId = 1L;

        // Настройка моков
        when(dashboardService.resetSettings(userId)).thenReturn(ResponseEntity.ok().build());

        // Вызов тестируемого метода
        ResponseEntity<Void> response = dashboardApiController.resetSettings(userId);

        // Проверка результатов
        assertEquals(200, response.getStatusCodeValue());
        verify(dashboardService).resetSettings(userId);
    }

    @Test
    void testGetSettings() {
        // Подготовка данных
        Long userId = 1L;
        Map<String, Object> settings = new HashMap<>();
        settings.put("knowThreshold", 0.1);
        settings.put("flashSpeed", 1000);

        // Настройка моков
        when(dashboardService.getSettings(userId)).thenReturn(settings);

        // Вызов тестируемого метода
        ResponseEntity<Map<String, Object>> response = dashboardApiController.getSettings(userId);

        // Проверка результатов
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(settings, response.getBody());
        verify(dashboardService).getSettings(userId);
    }

    @Test
    void testRefillWords() {
        // Подготовка данных
        Long userId = 1L;
        String language = "English";
        String minLevel = "A1";
        List<String> tags = Arrays.asList("BASIC_VOCABULARY");

        // Настройка моков
        doNothing().when(dashboardService).refillWords(userId, language, minLevel, tags);

        // Вызов тестируемого метода
        ResponseEntity<Void> response = dashboardApiController.refillWords(userId, language, minLevel, tags);

        // Проверка результатов
        assertEquals(200, response.getStatusCodeValue());
        verify(dashboardService).refillWords(userId, language, minLevel, tags);
    }

    @Test
    void testAddTag() {
        // Подготовка данных
        Long userId = 1L;
        String tag = "BASIC_VOCABULARY";

        // Настройка моков
        when(dashboardService.addTag(userId, tag)).thenReturn(ResponseEntity.ok().build());

        // Вызов тестируемого метода
        ResponseEntity<Void> response = dashboardApiController.addTag(userId, tag);

        // Проверка результатов
        assertEquals(200, response.getStatusCodeValue());
        verify(dashboardService).addTag(userId, tag);
    }

    @Test
    void testRemoveWord() {
        // Подготовка данных
        Long userId = 1L;
        Long wordId = 1L;

        // Настройка моков
        doNothing().when(dashboardService).removeWord(userId, wordId);

        // Вызов тестируемого метода
        ResponseEntity<Void> response = dashboardApiController.removeWord(userId, wordId);

        // Проверка результатов
        assertEquals(200, response.getStatusCodeValue());
        verify(dashboardService).removeWord(userId, wordId);
    }

    @Test
    void testUpdateWordProgress() {
        // Подготовка данных
        Long userId = 1L;
        Long wordId = 1L;
        boolean knows = true;

        // Настройка моков
        doNothing().when(dashboardService).updateWordProgress(userId, wordId, knows);

        // Вызов тестируемого метода
        ResponseEntity<Void> response = dashboardApiController.updateWordProgress(userId, wordId, knows);

        // Проверка результатов
        assertEquals(200, response.getStatusCodeValue());
        verify(dashboardService).updateWordProgress(userId, wordId, knows);
    }

    @Test
    void testGetCustomWords() {
        // Подготовка данных
        Long userId = 1L;
        List<WordProgress> customWords = new ArrayList<>();
        CustomWord customWord = new CustomWord();
        customWord.setId(1L);
        customWord.setWord("custom");
        customWord.setTranslation("кастомный");
        WordProgress progress = new WordProgress();
        progress.setWord(customWord);
        customWords.add(progress);

        // Настройка моков
        when(dashboardService.getCustomWords(userId)).thenReturn(customWords);

        // Вызов тестируемого метода
        ResponseEntity<List<WordProgress>> response = dashboardApiController.getCustomWords(userId);

        // Проверка результатов
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(dashboardService).getCustomWords(userId);
    }

    @Test
    void testCheckAutocomplete() {
        // Подготовка данных
        String word = "test";
        List<Word> words = new ArrayList<>();
        Word word1 = new Word();
        word1.setWord("test");
        words.add(word1);

        // Настройка моков
        when(wordRepository.findByWordStartingWith(word)).thenReturn(words);

        // Вызов тестируемого метода
        ResponseEntity<List<String>> response = dashboardApiController.checkAutocomplete(word);

        // Проверка результатов
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("test", response.getBody().get(0));
        verify(wordRepository).findByWordStartingWith(word);
    }

    @Test
    void testCheckDuplicate() {
        // Подготовка данных
        String word = "test";
        String translation = "тест";
        List<Word> words = new ArrayList<>();
        Word word1 = new Word();
        word1.setWord("test");
        word1.setTranslation("тест");
        words.add(word1);

        // Настройка моков
        when(wordRepository.findByWordStartingWith(word)).thenReturn(words);

        // Вызов тестируемого метода
        ResponseEntity<Boolean> response = dashboardApiController.checkDuplicate(word, translation);

        // Проверка результатов
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertTrue(response.getBody());
        verify(wordRepository).findByWordStartingWith(word);
    }

    @Test
    void testAddCustomWord() {
        // Подготовка данных
        String word = "custom";
        String translation = "кастомный";
        String example = "This is custom";
        String exampleTranslation = "Это кастомный";
        Long userId = 1L;

        // Настройка моков
        doNothing().when(dashboardService).addCustomWord(word, translation, example, exampleTranslation, userId);

        // Вызов тестируемого метода
        ResponseEntity<Void> response = dashboardApiController.addCustomWord(word, translation, example, exampleTranslation, userId);

        // Проверка результатов
        assertEquals(200, response.getStatusCodeValue());
        verify(dashboardService).addCustomWord(word, translation, example, exampleTranslation, userId);
    }

    @Test
    void testGetTextWordsCount() {
        // Подготовка данных
        Long textId = 1L;
        Long expectedCount = 100L;

        // Настройка моков
        when(dashboardService.getTextWordsCount(textId)).thenReturn(expectedCount);

        // Вызов тестируемого метода
        ResponseEntity<Long> response = dashboardApiController.getTextWordsCount(textId);

        // Проверка результатов
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(expectedCount, response.getBody());
        verify(dashboardService).getTextWordsCount(textId);
    }

    @Test
    void testGetAllTextWordsCount() {
        // Подготовка данных
        Long userId = 1L;
        Long expectedCount = 250L;

        // Настройка моков
        when(dashboardService.getAllTextWordsCount(userId)).thenReturn(expectedCount);

        // Вызов тестируемого метода
        ResponseEntity<Long> response = dashboardApiController.getAllTextWordsCount(userId);

        // Проверка результатов
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(expectedCount, response.getBody());
        verify(dashboardService).getAllTextWordsCount(userId);
    }

    @Test
    void testGetLearnedWordsCount() {
        // Подготовка данных
        Long textId = 1L;
        Long userId = 1L;
        Long expectedCount = 50L;

        // Настройка моков
        when(dashboardService.getLearnedWordsCount(userId, textId)).thenReturn(expectedCount);

        // Вызов тестируемого метода
        ResponseEntity<Long> response = dashboardApiController.getLearnedWordsCount(textId, userId);

        // Проверка результатов
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(expectedCount, response.getBody());
        verify(dashboardService).getLearnedWordsCount(userId, textId);
    }

    @Test
    void testGetTextWords() {
        // Подготовка данных
        Long textId = 1L;
        List<Word> expectedWords = new ArrayList<>();
        Word word1 = new Word();
        word1.setId(1L);
        word1.setWord("text");
        word1.setTranslation("текст");
        expectedWords.add(word1);

        // Настройка моков
        when(dashboardService.getTextWords(textId)).thenReturn(expectedWords);

        // Вызов тестируемого метода
        ResponseEntity<List<Word>> response = dashboardApiController.getTextWords(textId);

        // Проверка результатов
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("text", response.getBody().get(0).getWord());
        verify(dashboardService).getTextWords(textId);
    }
} 