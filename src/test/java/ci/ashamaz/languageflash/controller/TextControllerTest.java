package ci.ashamaz.languageflash.controller;

import ci.ashamaz.languageflash.model.*;
import ci.ashamaz.languageflash.service.LanguageService;
import ci.ashamaz.languageflash.service.TextService;
import ci.ashamaz.languageflash.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.Model;

import javax.servlet.http.HttpSession;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TextControllerTest {

    @Mock
    private TextService textService;

    @Mock
    private LanguageService languageService;

    @Mock
    private UserService userService;

    @Mock
    private HttpSession session;

    @Mock
    private Model model;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private TextController textController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(textController).build();
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
    }

    @Test
    void testTexts() {
        // Подготовка данных
        User user = new User();
        user.setId(1L);
        
        Map<String, Object> settings = new HashMap<>();
        settings.put("language", "English");
        
        Language language = new Language();
        language.setName("English");
        language.setActive(true);
        List<Language> languages = Collections.singletonList(language);
        
        Text text = new Text();
        text.setId(1L);
        text.setTitle("Test Text");
        text.setLanguage(language);
        text.setTags("LITERATURE,BUSINESS");
        
        List<Text> texts = Collections.singletonList(text);
        Page<Text> textPage = new PageImpl<>(texts);
        
        // Настройка моков
        when(session.getAttribute("user")).thenReturn(user);
        when(userService.getSettings(user.getId())).thenReturn(settings);
        when(languageService.getAllLanguages()).thenReturn(languages);
        when(textService.getActiveTextsByLanguage(anyString(), any(Pageable.class))).thenReturn(textPage);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("user");
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))).when(authentication).getAuthorities();
        
        // Вызов тестируемого метода
        String viewName = textController.texts(session, model, null, null, 0, 10);
        
        // Проверка результатов
        verify(model, times(1)).addAttribute(eq("languages"), anyList());
        verify(model, times(1)).addAttribute(eq("selectedLanguage"), eq("English"));
        verify(model, times(1)).addAttribute(eq("tags"), eq(Tag.values()));
        verify(model, times(1)).addAttribute(eq("texts"), anyList());
        verify(model, times(1)).addAttribute(eq("page"), any(Page.class));
        verify(model, times(1)).addAttribute(eq("isAuthenticated"), eq(true));
        verify(model, times(1)).addAttribute(eq("isAdmin"), eq(false));
        assertEquals("texts", viewName);
    }

    @Test
    void testTextsWithTag() {
        // Подготовка данных
        String selectedLanguage = "English";
        String selectedTag = "LITERATURE";
        
        Language language = new Language();
        language.setName(selectedLanguage);
        language.setActive(true);
        List<Language> languages = Collections.singletonList(language);
        
        Text text = new Text();
        text.setId(1L);
        text.setTitle("Test Text");
        text.setLanguage(language);
        text.setTags(selectedTag);
        
        List<Text> texts = Collections.singletonList(text);
        Page<Text> textPage = new PageImpl<>(texts);
        
        // Настройка моков
        when(languageService.getAllLanguages()).thenReturn(languages);
        when(textService.getActiveTextsByLanguageAndTag(eq(selectedLanguage), eq(selectedTag), any(Pageable.class))).thenReturn(textPage);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("user");
        
        // Вызов тестируемого метода
        String viewName = textController.texts(session, model, selectedLanguage, selectedTag, 0, 10);
        
        // Проверка результатов
        verify(textService).getActiveTextsByLanguageAndTag(eq(selectedLanguage), eq(selectedTag), any(Pageable.class));
        assertEquals("texts", viewName);
    }

    @Test
    void testGetText() {
        // Подготовка данных
        Long textId = 1L;
        Text text = new Text();
        text.setId(textId);
        text.setTitle("Test Text");
        
        // Настройка моков
        when(textService.getTextById(textId)).thenReturn(text);
        
        // Вызов тестируемого метода
        Text result = textController.getText(textId);
        
        // Проверка результатов
        verify(textService).getTextById(textId);
        assertEquals(textId, result.getId());
        assertEquals("Test Text", result.getTitle());
    }

    @Test
    void testAddText() {
        // Подготовка данных
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("title", "New Text");
        requestBody.put("language", "English");
        requestBody.put("level", "B1");
        requestBody.put("tags", "LITERATURE");
        requestBody.put("content", "<p>Content</p>");
        requestBody.put("translation", "<p>Перевод</p>");
        
        List<Map<String, String>> words = new ArrayList<>();
        Map<String, String> word = new HashMap<>();
        word.put("word", "test");
        word.put("translation", "тест");
        word.put("exampleSentence", "This is a test");
        word.put("exampleTranslation", "Это тест");
        words.add(word);
        requestBody.put("words", words);
        
        Language language = new Language();
        language.setName("English");
        
        // Настройка моков
        when(languageService.getLanguageByName("English")).thenReturn(language);
        doNothing().when(textService).saveText(any(Text.class));
        
        // Настройка аутентификации для админа
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(authentication).getAuthorities();
        
        // Вызов тестируемого метода
        Map<String, String> result = textController.addText(requestBody);
        
        // Проверка результатов
        assertEquals("success", result.get("status"));
        verify(languageService).getLanguageByName("English");
        verify(textService).saveText(any(Text.class));
    }

    @Test
    void testAddTextWithError() {
        // Подготовка данных
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("title", "New Text");
        requestBody.put("language", "Unknown");
        
        // Настройка моков
        when(languageService.getLanguageByName("Unknown")).thenReturn(null);
        
        // Вызов тестируемого метода
        Map<String, String> result = textController.addText(requestBody);
        
        // Проверка результатов
        assertEquals("error", result.get("status"));
        assertTrue(result.containsKey("message"));
    }

    @Test
    void testEditText() {
        // Подготовка данных
        Long textId = 1L;
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("id", textId);
        requestBody.put("title", "Updated Text");
        requestBody.put("language", "English");
        requestBody.put("level", "B1");
        requestBody.put("tags", "LITERATURE");
        requestBody.put("content", "<p>Updated Content</p>");
        requestBody.put("translation", "<p>Обновленный перевод</p>");
        
        List<Map<String, String>> words = new ArrayList<>();
        Map<String, String> word = new HashMap<>();
        word.put("word", "updated");
        word.put("translation", "обновлено");
        word.put("exampleSentence", "This is updated");
        word.put("exampleTranslation", "Это обновлено");
        words.add(word);
        requestBody.put("words", words);
        
        Text existingText = new Text();
        existingText.setId(textId);
        existingText.setWords(new ArrayList<>());
        
        Language language = new Language();
        language.setName("English");
        
        // Настройка моков
        when(textService.getTextById(textId)).thenReturn(existingText);
        when(languageService.getLanguageByName("English")).thenReturn(language);
        
        // Вызов тестируемого метода
        Map<String, String> result = textController.editText(requestBody);
        
        // Проверка результатов
        assertEquals("success", result.get("status"));
        verify(textService).getTextById(textId);
        verify(languageService).getLanguageByName("English");
        verify(textService).saveText(any(Text.class));
    }

    @Test
    void testEditTextWithError() {
        // Подготовка данных
        Long textId = 1L;
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("id", textId);
        requestBody.put("language", "Unknown");
        
        Text existingText = new Text();
        existingText.setId(textId);
        
        // Настройка моков
        when(textService.getTextById(textId)).thenReturn(existingText);
        when(languageService.getLanguageByName("Unknown")).thenReturn(null);
        
        // Вызов тестируемого метода
        Map<String, String> result = textController.editText(requestBody);
        
        // Проверка результатов
        assertEquals("error", result.get("status"));
        assertTrue(result.containsKey("message"));
    }

    @Test
    void testDeleteText() {
        // Подготовка данных
        Long textId = 1L;
        Map<String, Long> requestBody = new HashMap<>();
        requestBody.put("textId", textId);
        
        // Настройка моков
        doNothing().when(textService).softDeleteText(textId);
        
        // Вызов тестируемого метода
        Map<String, String> result = textController.deleteText(requestBody);
        
        // Проверка результатов
        assertEquals("success", result.get("status"));
        verify(textService).softDeleteText(textId);
    }
} 