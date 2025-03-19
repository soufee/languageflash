package ci.ashamaz.languageflash.service;

import ci.ashamaz.languageflash.model.Language;
import ci.ashamaz.languageflash.model.LanguageLevel;
import ci.ashamaz.languageflash.model.Level;
import ci.ashamaz.languageflash.repository.LanguageLevelRepository;
import ci.ashamaz.languageflash.repository.LanguageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LanguageServiceTest {

    @InjectMocks
    private LanguageService languageService;

    @Mock
    private LanguageRepository languageRepository;

    @Mock
    private LanguageLevelRepository languageLevelRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getAllLanguages_success() {
        List<Language> languages = Arrays.asList(new Language(), new Language());
        when(languageRepository.findAll()).thenReturn(languages);

        List<Language> result = languageService.getAllLanguages();

        assertEquals(2, result.size());
        verify(languageRepository, times(1)).findAll();
    }

    @Test
    void getLanguageById_success() {
        Long id = 1L;
        Language language = new Language();
        language.setId(id);
        when(languageRepository.findById(id)).thenReturn(Optional.of(language));

        Language result = languageService.getLanguageById(id);

        assertEquals(id, result.getId());
        verify(languageRepository, times(1)).findById(id);
    }

    @Test
    void getLanguageById_notFound() {
        Long id = 1L;
        when(languageRepository.findById(id)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            languageService.getLanguageById(id);
        });

        assertEquals("Язык с ID " + id + " не найден", exception.getMessage());
        verify(languageRepository, times(1)).findById(id);
    }

    @Test
    void addLanguage_success() {
        String name = "English";
        when(languageRepository.findByName(name)).thenReturn(Optional.empty());
        Language savedLanguage = new Language();
        savedLanguage.setId(1L);
        savedLanguage.setName(name);
        savedLanguage.setActive(true);
        when(languageRepository.save(any(Language.class))).thenReturn(savedLanguage);

        Language result = languageService.addLanguage(name);

        assertEquals(name, result.getName());
        assertTrue(result.isActive());
        verify(languageRepository, times(1)).save(any(Language.class));
        verify(languageLevelRepository, times(Level.values().length)).save(any(LanguageLevel.class));
    }

    @Test
    void addLanguage_alreadyExists() {
        String name = "English";
        when(languageRepository.findByName(name)).thenReturn(Optional.of(new Language()));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            languageService.addLanguage(name);
        });

        assertEquals("Язык с именем '" + name + "' уже существует", exception.getMessage());
        verify(languageRepository, never()).save(any(Language.class));
    }

    @Test
    void updateLanguage_success() {
        Long id = 1L;
        String newName = "Spanish";
        boolean active = false;
        Language language = new Language();
        language.setId(id);
        when(languageRepository.findById(id)).thenReturn(Optional.of(language));
        when(languageRepository.save(any(Language.class))).thenReturn(language);

        languageService.updateLanguage(id, newName, active);

        assertEquals(newName, language.getName());
        assertFalse(language.isActive());
        verify(languageRepository, times(1)).save(language);
    }

    @Test
    void getLevelsForLanguage_success() {
        Long languageId = 1L;
        List<LanguageLevel> levels = Arrays.asList(new LanguageLevel(), new LanguageLevel());
        when(languageLevelRepository.findByLanguageId(languageId)).thenReturn(levels);

        List<LanguageLevel> result = languageService.getLevelsForLanguage(languageId);

        assertEquals(2, result.size());
        verify(languageLevelRepository, times(1)).findByLanguageId(languageId);
    }

    @Test
    void updateLanguageLevel_success() {
        Long languageId = 1L;
        Level level = Level.A1;
        boolean active = false;
        LanguageLevel languageLevel = new LanguageLevel();
        when(languageLevelRepository.findByLanguageIdAndLevel(languageId, level)).thenReturn(Optional.of(languageLevel));
        when(languageLevelRepository.save(any(LanguageLevel.class))).thenReturn(languageLevel);

        languageService.updateLanguageLevel(languageId, level, active);

        assertFalse(languageLevel.isActive());
        verify(languageLevelRepository, times(1)).save(languageLevel);
    }

    @Test
    void updateLanguageLevel_notFound() {
        Long languageId = 1L;
        Level level = Level.A1;
        when(languageLevelRepository.findByLanguageIdAndLevel(languageId, level)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            languageService.updateLanguageLevel(languageId, level, true);
        });

        assertEquals("Уровень " + level + " для языка " + languageId + " не найден", exception.getMessage());
        verify(languageLevelRepository, never()).save(any(LanguageLevel.class));
    }

    @Test
    void getAllLevels_success() {
        List<LanguageLevel> levels = Arrays.asList(new LanguageLevel(), new LanguageLevel());
        when(languageLevelRepository.findAll()).thenReturn(levels);

        List<LanguageLevel> result = languageService.getAllLevels();

        assertEquals(2, result.size());
        verify(languageLevelRepository, times(1)).findAll();
    }

    @Test
    void getLanguageByName_success() {
        String name = "English";
        Language language = new Language();
        language.setName(name);
        when(languageRepository.findByName(name)).thenReturn(Optional.of(language));

        Language result = languageService.getLanguageByName(name);

        assertEquals(name, result.getName());
        verify(languageRepository, times(1)).findByName(name);
    }

    @Test
    void getLanguageByName_notFound() {
        String name = "Unknown";
        when(languageRepository.findByName(name)).thenReturn(Optional.empty());

        Language result = languageService.getLanguageByName(name);

        assertNull(result);
        verify(languageRepository, times(1)).findByName(name);
    }
}