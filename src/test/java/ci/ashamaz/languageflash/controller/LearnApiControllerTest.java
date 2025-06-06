package ci.ashamaz.languageflash.controller;

import ci.ashamaz.languageflash.model.*;
import ci.ashamaz.languageflash.service.UserService;
import ci.ashamaz.languageflash.service.WordProgressService;
import ci.ashamaz.languageflash.service.WordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpSession;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LearnApiControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private WordProgressService wordProgressService;

    @Mock
    private WordService wordService;

    @Mock
    private HttpSession session;

    @InjectMocks
    private LearnApiController learnApiController;

    private User testUser;
    private Word testWord;
    private WordProgress testWordProgress;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Подготовка тестовых данных
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");

        testWord = new Word();
        testWord.setId(1L);
        testWord.setWord("test");
        testWord.setTranslation("тест");
        testWord.setExampleSentence("This is a test");
        testWord.setExampleTranslation("Это тест");

        testWordProgress = new WordProgress();
        testWordProgress.setWord(testWord);
        testWordProgress.setKnowledgeFactor(0.5f);
        testWordProgress.setLearned(false);
    }

    @Test
    void testUpdateProgress_Unauthorized() {
        when(session.getAttribute("user")).thenReturn(null);

        ResponseEntity<Void> response = learnApiController.updateProgress(1L, true, false, session);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(wordProgressService, never()).updateProgress(anyLong(), anyLong(), anyBoolean());
    }

    @Test
    void testUpdateProgress_Success() {
        when(session.getAttribute("user")).thenReturn(testUser);
        doNothing().when(wordProgressService).updateProgress(anyLong(), anyLong(), anyBoolean());

        ResponseEntity<Void> response = learnApiController.updateProgress(1L, true, false, session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(wordProgressService).updateProgress(testUser.getId(), 1L, true);
    }

    @Test
    void testUpdateProgress_ForceLearned() {
        when(session.getAttribute("user")).thenReturn(testUser);
        when(wordProgressService.getProgress(anyLong(), anyLong())).thenReturn(testWordProgress);
        doNothing().when(wordProgressService).save(any(WordProgress.class));

        ResponseEntity<Void> response = learnApiController.updateProgress(1L, true, true, session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(testWordProgress.isLearned());
        assertEquals(0.0f, testWordProgress.getKnowledgeFactor());
        verify(wordProgressService).save(testWordProgress);
    }

    @Test
    void testGetNextWord_Unauthorized() {
        when(session.getAttribute("user")).thenReturn(null);

        Map<String, Object> response = learnApiController.getNextWord(session);

        assertTrue(response.containsKey("error"));
        assertEquals("User not authenticated", response.get("error"));
    }

    @Test
    void testGetNextWord_NoWords() {
        when(session.getAttribute("user")).thenReturn(testUser);
        when(wordProgressService.getActiveProgress(anyLong())).thenReturn(Collections.emptyList());

        Map<String, Object> response = learnApiController.getNextWord(session);

        assertNull(response.get("word"));
    }

    @Test
    void testGetNextWord_Success() {
        when(session.getAttribute("user")).thenReturn(testUser);
        when(wordProgressService.getActiveProgress(anyLong())).thenReturn(Collections.singletonList(testWordProgress));

        Map<String, Object> response = learnApiController.getNextWord(session);

        assertNotNull(response.get("word"));
        @SuppressWarnings("unchecked")
        Map<String, Object> wordData = (Map<String, Object>) response.get("word");
        assertEquals(testWord.getId(), wordData.get("id"));
        assertEquals(testWord.getWord(), wordData.get("word"));
        assertEquals(testWord.getTranslation(), wordData.get("translation"));
    }

    @Test
    void testRefillWords_Unauthorized() {
        when(session.getAttribute("user")).thenReturn(null);

        ResponseEntity<Map<String, Object>> response = learnApiController.refillWords(session);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void testRefillWords_Success() {
        when(session.getAttribute("user")).thenReturn(testUser);
        Map<String, Object> settings = new HashMap<>();
        settings.put("language", "English");
        settings.put("minLevel", "A1");
        settings.put("tags", Collections.emptyList());
        settings.put("activeWordsCount", 50);
        when(userService.getSettings(anyLong())).thenReturn(settings);
        when(wordProgressService.getActiveProgressForProgram(anyLong())).thenReturn(Collections.emptyList());
        when(wordService.selectWordsForLearning(anyLong(), anyString(), anyString(), anyList(), anyInt()))
                .thenReturn(Collections.singletonList(testWord));
        when(wordProgressService.getActiveProgress(anyLong())).thenReturn(Collections.singletonList(testWordProgress));
        when(wordProgressService.getLearnedProgress(anyLong())).thenReturn(Collections.emptyList());

        ResponseEntity<Map<String, Object>> response = learnApiController.refillWords(session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().get("activeCount"));
        assertEquals(0, response.getBody().get("learnedCount"));
        assertTrue(response.getBody().containsKey("activeWords"));
        assertTrue(response.getBody().containsKey("showTagPrompt"));
    }

    @Test
    void testGetActiveWords_Unauthorized() {
        when(session.getAttribute("user")).thenReturn(null);

        List<Map<String, Object>> response = learnApiController.getActiveWords(session);

        assertTrue(response.isEmpty());
    }

    @Test
    void testGetActiveWords_Success() {
        when(session.getAttribute("user")).thenReturn(testUser);
        when(wordProgressService.getActiveProgress(anyLong())).thenReturn(Collections.singletonList(testWordProgress));

        List<Map<String, Object>> response = learnApiController.getActiveWords(session);

        assertFalse(response.isEmpty());
        assertEquals(1, response.size());
        Map<String, Object> wordData = response.get(0);
        assertEquals(testWord.getId(), wordData.get("id"));
        assertEquals(testWord.getWord(), wordData.get("word"));
        assertEquals(testWord.getTranslation(), wordData.get("translation"));
        assertEquals(testWordProgress.getKnowledgeFactor(), wordData.get("knowledgeFactor"));
    }

    @Test
    void testGetLearnedWords_Unauthorized() {
        when(session.getAttribute("user")).thenReturn(null);

        List<Map<String, Object>> response = learnApiController.getLearnedWords(session);

        assertTrue(response.isEmpty());
    }

    @Test
    void testGetLearnedWords_Success() {
        when(session.getAttribute("user")).thenReturn(testUser);
        testWordProgress.setLearned(true);
        when(wordProgressService.getLearnedProgress(anyLong())).thenReturn(Collections.singletonList(testWordProgress));

        List<Map<String, Object>> response = learnApiController.getLearnedWords(session);

        assertFalse(response.isEmpty());
        assertEquals(1, response.size());
        Map<String, Object> wordData = response.get(0);
        assertEquals(testWord.getId(), wordData.get("id"));
        assertEquals(testWord.getWord(), wordData.get("word"));
        assertEquals(testWord.getTranslation(), wordData.get("translation"));
        assertEquals(testWord.getExampleSentence(), wordData.get("exampleSentence"));
        assertEquals(testWord.getExampleTranslation(), wordData.get("exampleTranslation"));
    }

    @Test
    void testGetTextWords_Unauthorized() {
        when(session.getAttribute("user")).thenReturn(null);

        List<Map<String, Object>> response = learnApiController.getTextWords(session, null);

        assertTrue(response.isEmpty());
    }

    @Test
    void testGetTextWords_Success() {
        when(session.getAttribute("user")).thenReturn(testUser);
        Text text = new Text();
        text.setId(1L);
        text.setTitle("Test Text");
        testWordProgress.setText(text);
        when(wordProgressService.getTextProgress(anyLong())).thenReturn(Collections.singletonList(testWordProgress));

        List<Map<String, Object>> response = learnApiController.getTextWords(session, null);

        assertFalse(response.isEmpty());
        assertEquals(1, response.size());
        Map<String, Object> wordData = response.get(0);
        assertEquals(testWord.getId(), wordData.get("id"));
        assertEquals(testWord.getWord(), wordData.get("word"));
        assertEquals(testWord.getTranslation(), wordData.get("translation"));
        assertEquals(testWordProgress.isLearned(), wordData.get("learned"));
        assertEquals(text.getId(), wordData.get("textId"));
        assertEquals(text.getTitle(), wordData.get("textTitle"));
    }

    @Test
    void testGetTextWords_WithTextId() {
        when(session.getAttribute("user")).thenReturn(testUser);
        Text text = new Text();
        text.setId(1L);
        text.setTitle("Test Text");
        testWordProgress.setText(text);
        when(wordProgressService.getTextProgressByTextId(anyLong(), anyLong()))
                .thenReturn(Collections.singletonList(testWordProgress));

        List<Map<String, Object>> response = learnApiController.getTextWords(session, 1L);

        assertFalse(response.isEmpty());
        assertEquals(1, response.size());
        Map<String, Object> wordData = response.get(0);
        assertEquals(testWord.getId(), wordData.get("id"));
        assertEquals(testWord.getWord(), wordData.get("word"));
        assertEquals(testWord.getTranslation(), wordData.get("translation"));
        assertEquals(testWordProgress.isLearned(), wordData.get("learned"));
        assertEquals(text.getId(), wordData.get("textId"));
        assertEquals(text.getTitle(), wordData.get("textTitle"));
    }

    @Test
    void testGetTextTitles_Unauthorized() {
        when(session.getAttribute("user")).thenReturn(null);

        List<Map<String, Object>> response = learnApiController.getTextTitles(session);

        assertTrue(response.isEmpty());
    }

    @Test
    void testGetTextTitles_Success() {
        when(session.getAttribute("user")).thenReturn(testUser);
        Text text = new Text();
        text.setId(1L);
        text.setTitle("Test Text");
        when(wordProgressService.getTextsWithWords(anyLong())).thenReturn(Collections.singletonList(text));

        List<Map<String, Object>> response = learnApiController.getTextTitles(session);

        assertFalse(response.isEmpty());
        assertEquals(1, response.size());
        Map<String, Object> textData = response.get(0);
        assertEquals(text.getId(), textData.get("id"));
        assertEquals(text.getTitle(), textData.get("title"));
    }
} 