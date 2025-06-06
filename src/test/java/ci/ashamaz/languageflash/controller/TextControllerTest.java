package ci.ashamaz.languageflash.controller;

import ci.ashamaz.languageflash.model.*;
import ci.ashamaz.languageflash.service.LanguageService;
import ci.ashamaz.languageflash.service.TextService;
import ci.ashamaz.languageflash.service.UserService;
import ci.ashamaz.languageflash.service.WordProgressService;
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
import org.springframework.ui.Model;

import javax.servlet.http.HttpSession;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    private WordProgressService wordProgressService;

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

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
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
        when(textService.getActiveTextsByLanguage(eq("English"), any(Pageable.class))).thenReturn(textPage);
        when(textService.getAllTags()).thenReturn(Collections.emptyList());
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("user");
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))).when(authentication).getAuthorities();
        
        // Вызов тестируемого метода
        String viewName = textController.texts("English", null, 0, model, session);
        
        // Проверка результатов
        verify(model, times(1)).addAttribute(eq("languages"), anyList());
        verify(model, times(1)).addAttribute(eq("selectedLanguage"), eq("English"));
        verify(model, times(1)).addAttribute(eq("allTags"), anyList());
        verify(model, times(1)).addAttribute(eq("isAuthenticated"), eq(true));
        verify(model, times(1)).addAttribute(eq("texts"), eq(texts));
        verify(model, times(1)).addAttribute(eq("page"), eq(textPage));
        verify(textService, times(1)).getActiveTextsByLanguage(eq("English"), any(Pageable.class));
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
        when(textService.getAllTags()).thenReturn(Collections.emptyList());
        when(textService.getActiveTextsByLanguageAndTag(eq(selectedLanguage), eq(selectedTag), any(Pageable.class))).thenReturn(textPage);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("user");
        
        // Вызов тестируемого метода
        String viewName = textController.texts(selectedLanguage, selectedTag, 0, model, session);
        
        // Проверка результатов
        verify(textService).getAllTags();
        verify(textService).getActiveTextsByLanguageAndTag(eq(selectedLanguage), eq(selectedTag), any(Pageable.class));
        verify(model).addAttribute(eq("texts"), eq(texts));
        verify(model).addAttribute(eq("page"), eq(textPage));
        verify(model).addAttribute(eq("selectedTag"), eq(selectedTag));
        assertEquals("texts", viewName);
    }

    @Test
    void testGetTextWordsDebug() {
        // Подготовка данных
        User user = new User();
        user.setId(1L);
        
        Text text = new Text();
        text.setId(1L);
        text.setTitle("Test Text");
        
        WordProgress wordProgress = new WordProgress();
        wordProgress.setId(1L);
        wordProgress.setText(text);
        
        List<WordProgress> wordProgresses = Collections.singletonList(wordProgress);
        
        // Настройка моков
        when(session.getAttribute("user")).thenReturn(user);
        when(wordProgressService.getTextProgress(user.getId())).thenReturn(wordProgresses);
        when(wordProgressService.getTextsWithWords(user.getId())).thenReturn(Collections.singletonList(text));
        
        // Вызов тестируемого метода
        String viewName = textController.getTextWordsDebug(model, session);
        
        // Проверка результатов
        verify(model).addAttribute(eq("textWords"), eq(wordProgresses));
        verify(model).addAttribute(eq("texts"), anyList());
        verify(model).addAttribute(eq("user"), eq(user));
        assertEquals("debug/text-words-debug", viewName);
    }

    @Test
    void testViewText() {
        // Подготовка данных
        Long textId = 1L;
        User user = new User();
        user.setId(1L);
        
        Text text = new Text();
        text.setId(textId);
        text.setTitle("Test Text");
        
        // Настройка моков
        when(session.getAttribute("user")).thenReturn(user);
        when(textService.getTextById(textId)).thenReturn(text);
        when(wordProgressService.isTextInProgress(user.getId(), textId)).thenReturn(true);
        
        // Вызов тестируемого метода
        String viewName = textController.viewText(textId, model, session);
        
        // Проверка результатов
        verify(model).addAttribute(eq("text"), eq(text));
        verify(model).addAttribute(eq("isInProgress"), eq(true));
        verify(model).addAttribute(eq("isAuthenticated"), eq(true));
        assertEquals("text", viewName);
    }

    @Test
    void testShowAddTextForm() {
        // Подготовка данных
        Language language = new Language();
        language.setName("English");
        List<Language> languages = Collections.singletonList(language);
        
        // Настройка моков
        when(languageService.getAllLanguages()).thenReturn(languages);
        
        // Вызов тестируемого метода
        String viewName = textController.showAddTextForm(model);
        
        // Проверка результатов
        verify(model).addAttribute(eq("languages"), eq(languages));
        assertEquals("addText", viewName);
    }

    @Test
    void testShowEditTextForm() {
        // Подготовка данных
        Long textId = 1L;
        Text text = new Text();
        text.setId(textId);
        text.setTitle("Test Text");
        
        Language language = new Language();
        language.setName("English");
        List<Language> languages = Collections.singletonList(language);
        
        // Настройка моков
        when(textService.getTextById(textId)).thenReturn(text);
        when(languageService.getAllLanguages()).thenReturn(languages);
        
        // Вызов тестируемого метода
        String viewName = textController.showEditTextForm(textId, model);
        
        // Проверка результатов
        verify(model).addAttribute(eq("text"), eq(text));
        verify(model).addAttribute(eq("languages"), eq(languages));
        assertEquals("editText", viewName);
    }

    @Test
    void testShowDeleteTextForm() {
        // Подготовка данных
        Long textId = 1L;
        Text text = new Text();
        text.setId(textId);
        text.setTitle("Test Text");
        
        // Настройка моков
        when(textService.getTextById(textId)).thenReturn(text);
        
        // Вызов тестируемого метода
        String viewName = textController.showDeleteTextForm(textId, model);
        
        // Проверка результатов
        verify(model).addAttribute(eq("text"), eq(text));
        assertEquals("deleteText", viewName);
    }
} 