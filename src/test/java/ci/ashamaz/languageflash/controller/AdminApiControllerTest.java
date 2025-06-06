package ci.ashamaz.languageflash.controller;

import ci.ashamaz.languageflash.model.*;
import ci.ashamaz.languageflash.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminApiControllerTest {

    @Mock
    private AdminService adminService;

    @InjectMocks
    private AdminApiController adminApiController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testListUsers() {
        // Подготовка данных
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        
        List<User> users = Collections.singletonList(user);
        Page<User> userPage = new PageImpl<>(users);
        
        // Настройка моков
        when(adminService.getAllUsers(any(Pageable.class))).thenReturn(userPage);
        
        // Вызов тестируемого метода
        Page<User> result = adminApiController.listUsers(Pageable.unpaged());
        
        // Проверка результатов
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(adminService).getAllUsers(any(Pageable.class));
    }

    @Test
    void testSearchUsers() {
        // Подготовка данных
        String email = "test@example.com";
        User user = new User();
        user.setId(1L);
        user.setEmail(email);
        
        List<User> users = Collections.singletonList(user);
        Page<User> userPage = new PageImpl<>(users);
        
        // Настройка моков
        when(adminService.searchUsersByEmail(eq(email), any(Pageable.class))).thenReturn(userPage);
        
        // Вызов тестируемого метода
        Page<User> result = adminApiController.searchUsers(email, Pageable.unpaged());
        
        // Проверка результатов
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(adminService).searchUsersByEmail(eq(email), any(Pageable.class));
    }

    @Test
    void testBlockUser() {
        // Подготовка данных
        Long userId = 1L;
        boolean blocked = true;
        
        // Настройка моков
        doNothing().when(adminService).blockUser(eq(userId), eq(blocked), isNull());
        
        // Вызов тестируемого метода
        ResponseEntity<Void> response = adminApiController.blockUser(userId, blocked);
        
        // Проверка результатов
        assertEquals(200, response.getStatusCodeValue());
        verify(adminService).blockUser(eq(userId), eq(blocked), isNull());
    }

    @Test
    void testToggleAdmin() {
        // Подготовка данных
        Long userId = 1L;
        boolean isAdmin = true;
        
        // Настройка моков
        doNothing().when(adminService).toggleAdmin(eq(userId), eq(isAdmin), isNull());
        
        // Вызов тестируемого метода
        ResponseEntity<Void> response = adminApiController.toggleAdmin(userId, isAdmin);
        
        // Проверка результатов
        assertEquals(200, response.getStatusCodeValue());
        verify(adminService).toggleAdmin(eq(userId), eq(isAdmin), isNull());
    }

    @Test
    void testListLanguages() {
        // Подготовка данных
        Language language = new Language();
        language.setId(1L);
        language.setName("English");
        
        List<Language> languages = Collections.singletonList(language);
        
        // Настройка моков
        when(adminService.getAllLanguages()).thenReturn(languages);
        
        // Вызов тестируемого метода
        List<Language> result = adminApiController.listLanguages();
        
        // Проверка результатов
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(adminService).getAllLanguages();
    }

    @Test
    void testAddLanguage() {
        // Подготовка данных
        String name = "English";
        
        // Настройка моков
        doNothing().when(adminService).addLanguage(eq(name));
        
        // Вызов тестируемого метода
        ResponseEntity<Void> response = adminApiController.addLanguage(name);
        
        // Проверка результатов
        assertEquals(200, response.getStatusCodeValue());
        verify(adminService).addLanguage(eq(name));
    }

    @Test
    void testUpdateLanguage() {
        // Подготовка данных
        Long id = 1L;
        String name = "English";
        boolean active = true;
        
        // Настройка моков
        doNothing().when(adminService).updateLanguage(eq(id), eq(name), eq(active));
        
        // Вызов тестируемого метода
        ResponseEntity<Void> response = adminApiController.updateLanguage(id, name, active);
        
        // Проверка результатов
        assertEquals(200, response.getStatusCodeValue());
        verify(adminService).updateLanguage(eq(id), eq(name), eq(active));
    }

    @Test
    void testUpdateLanguageLevel() {
        // Подготовка данных
        Long id = 1L;
        Level level = Level.A1;
        boolean active = true;
        
        // Настройка моков
        doNothing().when(adminService).updateLanguageLevel(eq(id), eq(level), eq(active));
        
        // Вызов тестируемого метода
        ResponseEntity<Void> response = adminApiController.updateLanguageLevel(id, level, active);
        
        // Проверка результатов
        assertEquals(200, response.getStatusCodeValue());
        verify(adminService).updateLanguageLevel(eq(id), eq(level), eq(active));
    }

    @Test
    void testListWords() {
        // Подготовка данных
        String filter = "test";
        String language = "English";
        String level = "A1";
        
        Word word = new Word();
        word.setId(1L);
        word.setWord("test");
        
        List<Word> words = Collections.singletonList(word);
        Page<Word> wordPage = new PageImpl<>(words);
        
        // Настройка моков
        when(adminService.getFilteredWords(eq(filter), eq(language), eq(level), any(Pageable.class))).thenReturn(wordPage);
        
        // Вызов тестируемого метода
        Page<Word> result = adminApiController.listWords(filter, language, level, Pageable.unpaged());
        
        // Проверка результатов
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(adminService).getFilteredWords(eq(filter), eq(language), eq(level), any(Pageable.class));
    }

    @Test
    void testAddWord() {
        // Подготовка данных
        String word = "test";
        String translation = "тест";
        String exampleSentence = "This is a test";
        String exampleTranslation = "Это тест";
        Long languageId = 1L;
        String level = "A1";
        List<String> tags = Arrays.asList("COMMON", "BASIC");
        
        Map<String, Object> expectedResponse = new HashMap<>();
        expectedResponse.put("id", 1L);
        expectedResponse.put("status", "success");
        
        // Настройка моков
        when(adminService.addWord(eq(word), eq(translation), eq(exampleSentence), eq(exampleTranslation), eq(languageId), eq(level), eq(tags))).thenReturn(expectedResponse);
        
        // Вызов тестируемого метода
        ResponseEntity<Map<String, Object>> response = adminApiController.addWord(word, translation, exampleSentence, exampleTranslation, languageId, level, tags);
        
        // Проверка результатов
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals("success", response.getBody().get("status"));
        assertEquals(1L, response.getBody().get("id"));
        verify(adminService).addWord(eq(word), eq(translation), eq(exampleSentence), eq(exampleTranslation), eq(languageId), eq(level), eq(tags));
    }

    @Test
    void testEditWord() {
        // Подготовка данных
        Long id = 1L;
        String word = "test";
        String translation = "тест";
        String exampleSentence = "This is a test";
        String exampleTranslation = "Это тест";
        Long languageId = 1L;
        String level = "A1";
        List<String> tags = Arrays.asList("COMMON", "BASIC");
        
        Map<String, Object> expectedResponse = new HashMap<>();
        expectedResponse.put("id", id);
        expectedResponse.put("status", "success");
        
        // Настройка моков
        when(adminService.editWord(eq(id), eq(word), eq(translation), eq(exampleSentence), eq(exampleTranslation), eq(languageId), eq(level), eq(tags))).thenReturn(expectedResponse);
        
        // Вызов тестируемого метода
        ResponseEntity<Map<String, Object>> response = adminApiController.editWord(id, word, translation, exampleSentence, exampleTranslation, languageId, level, tags);
        
        // Проверка результатов
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals("success", response.getBody().get("status"));
        assertEquals(id, response.getBody().get("id"));
        verify(adminService).editWord(eq(id), eq(word), eq(translation), eq(exampleSentence), eq(exampleTranslation), eq(languageId), eq(level), eq(tags));
    }
} 