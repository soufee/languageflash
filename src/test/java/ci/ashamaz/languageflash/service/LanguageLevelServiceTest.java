package ci.ashamaz.languageflash.service;

import ci.ashamaz.languageflash.model.Language;
import ci.ashamaz.languageflash.model.LanguageLevel;
import ci.ashamaz.languageflash.model.Level;
import ci.ashamaz.languageflash.repository.LanguageLevelRepository;
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

class LanguageLevelServiceTest {

    @InjectMocks
    private LanguageLevelService languageLevelService;

    @Mock
    private LanguageLevelRepository languageLevelRepository;

    @Mock
    private LanguageService languageService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void addLanguageLevel_success() {
        Long languageId = 1L;
        Level level = Level.A1;
        boolean active = true;
        Language language = new Language();
        language.setId(languageId);
        when(languageService.getLanguageById(languageId)).thenReturn(language);
        LanguageLevel savedLevel = new LanguageLevel();
        savedLevel.setLanguage(language);
        savedLevel.setLevel(level);
        savedLevel.setActive(active);
        when(languageLevelRepository.save(any(LanguageLevel.class))).thenReturn(savedLevel);

        LanguageLevel result = languageLevelService.addLanguageLevel(languageId, level, active);

        assertEquals(languageId, result.getLanguage().getId());
        assertEquals(level, result.getLevel());
        assertTrue(result.isActive());
        verify(languageLevelRepository, times(1)).save(any(LanguageLevel.class));
    }

    @Test
    void getLanguageLevelsByLanguage_success() {
        Long languageId = 1L;
        List<LanguageLevel> levels = Arrays.asList(new LanguageLevel(), new LanguageLevel());
        when(languageLevelRepository.findByLanguageId(languageId)).thenReturn(levels);

        List<LanguageLevel> result = languageLevelService.getLanguageLevelsByLanguage(languageId);

        assertEquals(2, result.size());
        verify(languageLevelRepository, times(1)).findByLanguageId(languageId);
    }

    @Test
    void getLanguageLevelById_success() {
        Long id = 1L;
        LanguageLevel level = new LanguageLevel();
        level.setId(id);
        when(languageLevelRepository.findById(id)).thenReturn(Optional.of(level));

        LanguageLevel result = languageLevelService.getLanguageLevelById(id);

        assertEquals(id, result.getId());
        verify(languageLevelRepository, times(1)).findById(id);
    }

    @Test
    void getLanguageLevelById_notFound() {
        Long id = 1L;
        when(languageLevelRepository.findById(id)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            languageLevelService.getLanguageLevelById(id);
        });

        assertEquals("Уровень языка с ID " + id + " не найден", exception.getMessage());
        verify(languageLevelRepository, times(1)).findById(id);
    }

    @Test
    void getLanguageLevelByLanguageAndLevel_success() {
        Long languageId = 1L;
        Level level = Level.A1;
        LanguageLevel languageLevel = new LanguageLevel();
        when(languageLevelRepository.findByLanguageIdAndLevel(languageId, level)).thenReturn(Optional.of(languageLevel));

        LanguageLevel result = languageLevelService.getLanguageLevelByLanguageAndLevel(languageId, level);

        assertEquals(languageLevel, result);
        verify(languageLevelRepository, times(1)).findByLanguageIdAndLevel(languageId, level);
    }

    @Test
    void getLanguageLevelByLanguageAndLevel_notFound() {
        Long languageId = 1L;
        Level level = Level.A1;
        when(languageLevelRepository.findByLanguageIdAndLevel(languageId, level)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            languageLevelService.getLanguageLevelByLanguageAndLevel(languageId, level);
        });

        assertEquals("Уровень " + level + " для языка с ID " + languageId + " не найден", exception.getMessage());
        verify(languageLevelRepository, times(1)).findByLanguageIdAndLevel(languageId, level);
    }

    @Test
    void updateLanguageLevel_success() {
        Long id = 1L;
        boolean active = false;
        LanguageLevel level = new LanguageLevel();
        level.setId(id);
        when(languageLevelRepository.findById(id)).thenReturn(Optional.of(level));
        when(languageLevelRepository.save(any(LanguageLevel.class))).thenReturn(level);

        languageLevelService.updateLanguageLevel(id, active);

        assertFalse(level.isActive());
        verify(languageLevelRepository, times(1)).save(level);
    }

    @Test
    void getLevelsForLanguage_success() {
        Long languageId = 1L;
        List<LanguageLevel> levels = Arrays.asList(new LanguageLevel(), new LanguageLevel());
        when(languageLevelRepository.findByLanguageId(languageId)).thenReturn(levels);

        List<LanguageLevel> result = languageLevelService.getLevelsForLanguage(languageId);

        assertEquals(2, result.size());
        verify(languageLevelRepository, times(1)).findByLanguageId(languageId);
    }

    @Test
    void getAllLanguageLevels_success() {
        List<LanguageLevel> levels = Arrays.asList(new LanguageLevel(), new LanguageLevel());
        when(languageLevelRepository.findAll()).thenReturn(levels);

        List<LanguageLevel> result = languageLevelService.getAllLanguageLevels();

        assertEquals(2, result.size());
        verify(languageLevelRepository, times(1)).findAll();
    }
}