package ci.ashamaz.languageflash.controller;

import ci.ashamaz.languageflash.config.TestSecurityConfig;
import ci.ashamaz.languageflash.model.Level;
import ci.ashamaz.languageflash.service.AdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@Import(TestSecurityConfig.class)
class AdminControllerTest {

    @MockBean
    private AdminService adminService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    void testRedirectToUsers() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "ADMIN")
    void testListUsers() throws Exception {
        // Подготовка данных
        Map<String, Object> userData = new HashMap<>();
        userData.put("users", java.util.Collections.emptyList());
        userData.put("currentUserId", 1L);

        // Настройка моков
        when(adminService.getUserListData("test@example.com")).thenReturn(userData);

        // Вызов тестируемого метода
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("userList"))
                .andExpect(model().attributeExists("users"))
                .andExpect(model().attributeExists("currentUserId"));

        verify(adminService).getUserListData("test@example.com");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testListLanguages() throws Exception {
        // Подготовка данных
        Map<String, Object> languageData = new HashMap<>();
        languageData.put("languages", java.util.Collections.emptyList());
        languageData.put("levels", java.util.Collections.emptyList());
        languageData.put("languageLevelsMap", new HashMap<>());

        // Настройка моков
        when(adminService.getLanguageListData()).thenReturn(languageData);

        // Вызов тестируемого метода
        mockMvc.perform(get("/admin/languages"))
                .andExpect(status().isOk())
                .andExpect(view().name("languages"))
                .andExpect(model().attributeExists("languages"))
                .andExpect(model().attributeExists("levels"))
                .andExpect(model().attributeExists("languageLevelsMap"));

        verify(adminService).getLanguageListData();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testListWords() throws Exception {
        // Подготовка данных
        String filter = "test";
        Map<String, Object> wordData = new HashMap<>();
        wordData.put("words", java.util.Collections.emptyList());
        wordData.put("wordTags", new HashMap<>());
        wordData.put("languages", java.util.Collections.emptyList());
        wordData.put("levels", java.util.Collections.emptyList());
        wordData.put("tags", java.util.Collections.emptyList());

        // Настройка моков
        when(adminService.getWordListData(filter)).thenReturn(wordData);

        // Вызов тестируемого метода
        mockMvc.perform(get("/admin/words").param("filter", filter))
                .andExpect(status().isOk())
                .andExpect(view().name("adminWords"))
                .andExpect(model().attributeExists("words"))
                .andExpect(model().attributeExists("wordTags"))
                .andExpect(model().attributeExists("languages"))
                .andExpect(model().attributeExists("levels"))
                .andExpect(model().attributeExists("tags"));

        verify(adminService).getWordListData(filter);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testAddWord() throws Exception {
        // Подготовка данных
        String word = "test";
        String translation = "тест";
        String exampleSentence = "This is a test";
        String exampleTranslation = "Это тест";
        Long languageId = 1L;
        String level = "A1";

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("id", 1L);

        // Настройка моков
        when(adminService.addWord(eq(word), eq(translation), eq(exampleSentence), eq(exampleTranslation), eq(languageId), eq(level), isNull())).thenReturn(response);

        // Вызов тестируемого метода
        mockMvc.perform(post("/admin/words/add")
                        .param("word", word)
                        .param("translation", translation)
                        .param("exampleSentence", exampleSentence)
                        .param("exampleTranslation", exampleTranslation)
                        .param("languageId", languageId.toString())
                        .param("level", level))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/words"));

        verify(adminService).addWord(eq(word), eq(translation), eq(exampleSentence), eq(exampleTranslation), eq(languageId), eq(level), isNull());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testEditWord() throws Exception {
        // Подготовка данных
        Long id = 1L;
        String word = "test";
        String translation = "тест";
        String exampleSentence = "This is a test";
        String exampleTranslation = "Это тест";
        Long languageId = 1L;
        String level = "A1";

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("id", id);

        // Настройка моков
        when(adminService.editWord(eq(id), eq(word), eq(translation), eq(exampleSentence), eq(exampleTranslation), eq(languageId), eq(level), isNull())).thenReturn(response);

        // Вызов тестируемого метода
        mockMvc.perform(post("/admin/words/edit")
                        .param("id", id.toString())
                        .param("word", word)
                        .param("translation", translation)
                        .param("exampleSentence", exampleSentence)
                        .param("exampleTranslation", exampleTranslation)
                        .param("languageId", languageId.toString())
                        .param("level", level))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/words"));

        verify(adminService).editWord(eq(id), eq(word), eq(translation), eq(exampleSentence), eq(exampleTranslation), eq(languageId), eq(level), isNull());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testAddLanguage() throws Exception {
        // Подготовка данных
        String name = "English";

        // Настройка моков
        doNothing().when(adminService).addLanguage(eq(name));

        // Вызов тестируемого метода
        mockMvc.perform(post("/admin/languages")
                        .param("name", name))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/languages"));

        verify(adminService).addLanguage(eq(name));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdateLanguage() throws Exception {
        // Подготовка данных
        Long id = 1L;
        String name = "English";
        boolean active = true;

        // Настройка моков
        doNothing().when(adminService).updateLanguage(eq(id), eq(name), eq(active));

        // Вызов тестируемого метода
        mockMvc.perform(post("/admin/languages/{id}/update", id)
                        .param("name", name)
                        .param("active", String.valueOf(active)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/languages"));

        verify(adminService).updateLanguage(eq(id), eq(name), eq(active));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdateLanguageLevel() throws Exception {
        // Подготовка данных
        Long id = 1L;
        Level level = Level.A1;
        boolean active = true;

        // Настройка моков
        doNothing().when(adminService).updateLanguageLevel(eq(id), eq(level), eq(active));

        // Вызов тестируемого метода
        mockMvc.perform(post("/admin/languages/levels/update")
                        .param("id", id.toString())
                        .param("level", level.toString())
                        .param("active", String.valueOf(active)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/languages"));

        verify(adminService).updateLanguageLevel(eq(id), eq(level), eq(active));
    }
} 