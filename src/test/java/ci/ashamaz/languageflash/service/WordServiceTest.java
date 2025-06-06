package ci.ashamaz.languageflash.service;

import ci.ashamaz.languageflash.model.*;
import ci.ashamaz.languageflash.repository.WordProgressRepository;
import ci.ashamaz.languageflash.repository.WordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WordServiceTest {

    @InjectMocks
    private WordService wordService;

    @Mock
    private WordRepository wordRepository;

    @Mock
    private WordProgressRepository wordProgressRepository;

    @Mock
    private LanguageService languageService;

    @Mock
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }


    // Тесты для getWordById
    @Test
    void getWordById_success() {
        Long wordId = 1L;
        Word word = new Word();
        word.setId(wordId);
        when(wordRepository.findById(wordId)).thenReturn(Optional.of(word));

        AbstractWord result = wordService.getWordById(wordId);

        assertNotNull(result);
        assertEquals(wordId, result.getId());
        verify(wordRepository, times(1)).findById(wordId);
    }

    @Test
    void getWordById_notFound() {
        Long wordId = 1L;
        when(wordRepository.findById(wordId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            wordService.getWordById(wordId);
        });

        assertEquals("Слово с ID " + wordId + " не найдено", exception.getMessage());
        verify(wordRepository, times(1)).findById(wordId);
    }

    // Тесты для getWordByIdAsWord
    @Test
    void getWordByIdAsWord_success() {
        Long wordId = 1L;
        Word word = new Word();
        word.setId(wordId);
        when(wordRepository.findById(wordId)).thenReturn(Optional.of(word));

        Word result = wordService.getWordByIdAsWord(wordId);

        assertNotNull(result);
        assertEquals(wordId, result.getId());
        verify(wordRepository, times(1)).findById(wordId);
    }

    @Test
    void getWordByIdAsWord_customWord() {
        Long wordId = 1L;
        CustomWord customWord = new CustomWord();
        customWord.setId(wordId);
        when(wordRepository.findById(wordId)).thenReturn(Optional.of(customWord));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            wordService.getWordByIdAsWord(wordId);
        });

        assertEquals("Слово с ID " + wordId + " не является общим словом и не может быть отредактировано здесь", exception.getMessage());
        verify(wordRepository, times(1)).findById(wordId);
    }

    @Test
    void getWordByIdAsWord_notFound() {
        Long wordId = 1L;
        when(wordRepository.findById(wordId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            wordService.getWordByIdAsWord(wordId);
        });

        assertEquals("Слово с ID " + wordId + " не найдено", exception.getMessage());
        verify(wordRepository, times(1)).findById(wordId);
    }

    // Тесты для getFilteredWords
    @Test
    void getFilteredWords_bothFilters() {
        String wordFilter = "test";
        String translationFilter = "тест";
        Pageable pageable = PageRequest.of(0, 10);
        List<Word> words = Arrays.asList(createWord(1L), createWord(2L));
        Page<Word> page = new PageImpl<>(words);
        when(wordRepository.findByWordStartingWithAndTranslationStartingWith(wordFilter, translationFilter, pageable)).thenReturn(page);

        Page<Word> result = wordService.getFilteredWords(wordFilter, translationFilter, pageable);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        verify(wordRepository, times(1)).findByWordStartingWithAndTranslationStartingWith(wordFilter, translationFilter, pageable);
    }

    @Test
    void getFilteredWords_wordFilterOnly() {
        String wordFilter = "test";
        String translationFilter = "";
        Pageable pageable = PageRequest.of(0, 10);
        List<Word> words = Arrays.asList(createWord(1L), createWord(2L));
        Page<Word> page = new PageImpl<>(words);
        when(wordRepository.findByWordStartingWith(wordFilter, pageable)).thenReturn(page);

        Page<Word> result = wordService.getFilteredWords(wordFilter, translationFilter, pageable);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        verify(wordRepository, times(1)).findByWordStartingWith(wordFilter, pageable);
    }

    @Test
    void getFilteredWords_translationFilterOnly() {
        String wordFilter = "";
        String translationFilter = "тест";
        Pageable pageable = PageRequest.of(0, 10);
        List<Word> words = Arrays.asList(createWord(1L), createWord(2L));
        Page<Word> page = new PageImpl<>(words);
        when(wordRepository.findByTranslationStartingWith(translationFilter, pageable)).thenReturn(page);

        Page<Word> result = wordService.getFilteredWords(wordFilter, translationFilter, pageable);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        verify(wordRepository, times(1)).findByTranslationStartingWith(translationFilter, pageable);
    }

    @Test
    void getFilteredWords_noFilters() {
        String wordFilter = "";
        String translationFilter = "";
        Pageable pageable = PageRequest.of(0, 10);
        List<Word> words = Arrays.asList(createWord(1L), createWord(2L));
        Page<Word> page = new PageImpl<>(words);
        when(wordRepository.findAllWords(pageable)).thenReturn(page);

        Page<Word> result = wordService.getFilteredWords(wordFilter, translationFilter, pageable);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        verify(wordRepository, times(1)).findAllWords(pageable);
    }

    // Тесты для save
    @Test
    void save_success() {
        Word word = new Word();
        word.setId(1L);
        when(wordRepository.save(word)).thenReturn(word);

        wordService.save(word);

        verify(wordRepository, times(1)).save(word);
    }

    // Тесты для addWord
    @Test
    void addWord_successWithTags() {
        String wordStr = "test";
        String translation = "тест";
        String exampleSentence = "This is a test.";
        String exampleTranslation = "Это тест.";
        Long languageId = 1L;
        String level = Level.A1.name();
        List<String> tags = Arrays.asList(Tag.BUSINESS.name(), Tag.BASIC_VOCABULARY.name());
        Language language = new Language();
        language.setId(languageId);
        when(languageService.getLanguageById(languageId)).thenReturn(language);
        Word savedWord = new Word();
        savedWord.setWord(wordStr);
        savedWord.setTranslation(translation);
        savedWord.setExampleSentence(exampleSentence);
        savedWord.setExampleTranslation(exampleTranslation);
        savedWord.setLanguage(language);
        savedWord.setLevel(level);
        savedWord.setTagsAsSet(tags.stream().map(Tag::valueOf).collect(Collectors.toSet()));
        when(wordRepository.save(any(Word.class))).thenReturn(savedWord);

        Word result = wordService.addWord(wordStr, translation, exampleSentence, exampleTranslation, languageId, level, tags);

        assertNotNull(result);
        assertEquals(wordStr, result.getWord());
        assertEquals(translation, result.getTranslation());
        assertEquals(exampleSentence, result.getExampleSentence());
        assertEquals(exampleTranslation, result.getExampleTranslation());
        assertEquals(level, result.getLevel());
        assertNotNull(result.getTagsAsSet());
        assertEquals(2, result.getTagsAsSet().size());
        assertTrue(result.getTagsAsSet().contains(Tag.BUSINESS));
        assertTrue(result.getTagsAsSet().contains(Tag.BASIC_VOCABULARY));
        verify(wordRepository, times(1)).save(any(Word.class));
    }

    @Test
    void addWord_successWithoutTags() {
        String wordStr = "test";
        String translation = "тест";
        String exampleSentence = "This is a test.";
        String exampleTranslation = "Это тест.";
        Long languageId = 1L;
        String level = "A1";
        List<String> tags = null;
        Language language = new Language();
        language.setId(languageId);
        when(languageService.getLanguageById(languageId)).thenReturn(language);
        Word savedWord = new Word();
        savedWord.setWord(wordStr);
        savedWord.setTranslation(translation);
        savedWord.setExampleSentence(exampleSentence);
        savedWord.setExampleTranslation(exampleTranslation);
        savedWord.setLanguage(language);
        savedWord.setLevel(level);
        when(wordRepository.save(any(Word.class))).thenReturn(savedWord);

        Word result = wordService.addWord(wordStr, translation, exampleSentence, exampleTranslation, languageId, level, tags);

        assertNotNull(result);
        assertEquals(wordStr, result.getWord());
        assertEquals(translation, result.getTranslation());
        assertEquals(exampleSentence, result.getExampleSentence());
        assertEquals(exampleTranslation, result.getExampleTranslation());
        assertEquals(level, result.getLevel());
        assertTrue(result.getTagsAsSet().isEmpty());
        verify(wordRepository, times(1)).save(any(Word.class));
    }

    @Test
    void addWord_invalidLevel() {
        String wordStr = "test";
        String translation = "тест";
        String exampleSentence = "This is a test.";
        String exampleTranslation = "Это тест.";
        Long languageId = 1L;
        String level = "INVALID_LEVEL";
        List<String> tags = Collections.emptyList();
        Language language = new Language();
        language.setId(languageId);
        when(languageService.getLanguageById(languageId)).thenReturn(language);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            wordService.addWord(wordStr, translation, exampleSentence, exampleTranslation, languageId, level, tags);
        });

        assertEquals("Недопустимый уровень: " + level, exception.getMessage());
        verify(wordRepository, never()).save(any(Word.class));
    }

    @Test
    void addWord_languageNotFound() {
        String wordStr = "test";
        String translation = "тест";
        String exampleSentence = "This is a test.";
        String exampleTranslation = "Это тест.";
        Long languageId = 1L;
        String level = Level.A1.name();
        List<String> tags = Collections.emptyList();
        when(languageService.getLanguageById(languageId)).thenReturn(null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            wordService.addWord(wordStr, translation, exampleSentence, exampleTranslation, languageId, level, tags);
        });

        assertEquals("Язык с ID " + languageId + " не найден", exception.getMessage());
        verify(wordRepository, never()).save(any(Word.class));
    }

    @Test
    void addWord_invalidTag() {
        String wordStr = "test";
        String translation = "тест";
        String exampleSentence = "This is a test.";
        String exampleTranslation = "Это тест.";
        Long languageId = 1L;
        String level = Level.A1.name();
        List<String> tags = Arrays.asList("INVALID_TAG");
        Language language = new Language();
        language.setId(languageId);
        when(languageService.getLanguageById(languageId)).thenReturn(language);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            wordService.addWord(wordStr, translation, exampleSentence, exampleTranslation, languageId, level, tags);
        });

        assertEquals("No enum constant ci.ashamaz.languageflash.model.Tag.INVALID_TAG", exception.getMessage());
        verify(wordRepository, never()).save(any(Word.class));
    }

    // Тесты для selectWordsForLearning
    @Test
    void selectWordsForLearning_noWordsNeeded() {
        Long userId = 1L;
        String languageName = "English";
        String minLevel = "A1";
        List<String> tags = Arrays.asList("tag1");
        int currentActiveCount = 50;
        Language language = new Language();
        language.setId(1L);
        language.setName(languageName);
        when(languageService.getLanguageByName(languageName)).thenReturn(language);
        when(userService.getSettings(userId)).thenReturn(Collections.singletonMap("activeWordsCount", 50));

        List<Word> result = wordService.selectWordsForLearning(userId, languageName, minLevel, tags, currentActiveCount);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(wordProgressRepository, never()).findByUserId(anyLong());
    }

    @Test
    void selectWordsForLearning_withTags() {
        Long userId = 1L;
        String languageName = "English";
        String minLevel = "A1"; // Используем строку, так как метод ожидает String
        List<String> tags = Arrays.asList(Tag.BUSINESS.name(), Tag.BASIC_VOCABULARY.name());
        int currentActiveCount = 10;
        Language language = new Language();
        language.setId(1L);
        language.setName(languageName);
        when(languageService.getLanguageByName(languageName)).thenReturn(language);
        when(userService.getSettings(userId)).thenReturn(Collections.singletonMap("activeWordsCount", 20));
        when(wordProgressRepository.findByUserId(userId)).thenReturn(Collections.emptyList());
        // Увеличиваем количество слов до >= 10
        List<Word> taggedWords1 = Arrays.asList(createWord(1L), createWord(2L), createWord(5L), createWord(6L), createWord(9L));
        List<Word> taggedWords2 = Arrays.asList(createWord(3L), createWord(4L), createWord(7L), createWord(8L), createWord(10L));
        when(wordRepository.findByLanguageIdAndMinLevelAndTag(1L, "A1", "BUSINESS")).thenReturn(taggedWords1);
        when(wordRepository.findByLanguageIdAndMinLevelAndTag(1L, "A1", "BASIC_VOCABULARY")).thenReturn(taggedWords2);

        List<Word> result = wordService.selectWordsForLearning(userId, languageName, minLevel, tags, currentActiveCount);

        assertNotNull(result);
        assertEquals(10, result.size()); // 20 - 10 = 10 слов нужно
        verify(wordRepository, times(2)).findByLanguageIdAndMinLevelAndTag(anyLong(), eq("A1"), anyString());
    }


    @Test
    void selectWordsForLearning_withoutTags() {
        Long userId = 1L;
        String languageName = "English";
        String minLevel = "A1"; // Используем строку вместо Level.A1.name()
        List<String> tags = Collections.emptyList();
        int currentActiveCount = 10;
        Language language = new Language();
        language.setId(1L);
        language.setName(languageName);
        when(languageService.getLanguageByName(languageName)).thenReturn(language);
        when(userService.getSettings(userId)).thenReturn(Collections.singletonMap("activeWordsCount", 20));
        when(wordProgressRepository.findByUserId(userId)).thenReturn(Collections.emptyList());
        // Увеличиваем количество слов до >= 10
        List<Word> levelWords = Arrays.asList(
                createWord(1L), createWord(2L), createWord(3L), createWord(4L),
                createWord(5L), createWord(6L), createWord(7L), createWord(8L),
                createWord(9L), createWord(10L)
        );
        when(wordRepository.findByLanguageIdAndMinLevel(1L, "A1")).thenReturn(levelWords);

        List<Word> result = wordService.selectWordsForLearning(userId, languageName, minLevel, tags, currentActiveCount);

        assertNotNull(result);
        assertEquals(10, result.size());
        verify(wordRepository, times(1)).findByLanguageIdAndMinLevel(1L, "A1");
    }

    @Test
    void selectWordsForLearning_languageNotFound() {
        Long userId = 1L;
        String languageName = "Unknown";
        String minLevel = "A1";
        List<String> tags = Arrays.asList(Tag.INTERNET.name());
        int currentActiveCount = 10;
        when(languageService.getLanguageByName(languageName)).thenReturn(null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            wordService.selectWordsForLearning(userId, languageName, minLevel, tags, currentActiveCount);
        });

        assertEquals("Язык " + languageName + " не найден", exception.getMessage());
        verify(languageService, times(1)).getLanguageByName(languageName);
    }

    @Test
    void selectWordsForLearning_invalidMinLevel() {
        Long userId = 1L;
        String languageName = "English";
        String minLevel = "Invalid";
        List<String> tags = Arrays.asList(Tag.FAMILY_HOME.name());
        int currentActiveCount = 10;
        Language language = new Language();
        language.setId(1L);
        language.setName(languageName);
        when(languageService.getLanguageByName(languageName)).thenReturn(language);
        when(userService.getSettings(userId)).thenReturn(Collections.singletonMap("activeWordsCount", 20));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            wordService.selectWordsForLearning(userId, languageName, minLevel, tags, currentActiveCount);
        });

        assertEquals("Недопустимый уровень: " + minLevel, exception.getMessage());
        verify(languageService, times(1)).getLanguageByName(languageName);
    }

    @Test
    void selectWordsForLearning_existingWordsExcluded() {
        Long userId = 1L;
        String languageName = "English";
        String minLevel = "A1"; // Используем строку вместо Level.A1.name()
        List<String> tags = Arrays.asList(Tag.HISTORY.name());
        int currentActiveCount = 10;
        Language language = new Language();
        language.setId(1L);
        language.setName(languageName);
        when(languageService.getLanguageByName(languageName)).thenReturn(language);
        when(userService.getSettings(userId)).thenReturn(Collections.singletonMap("activeWordsCount", 20));
        WordProgress progress = new WordProgress();
        Word existingWord = createWord(1L);
        progress.setWord(existingWord);
        when(wordProgressRepository.findByUserId(userId)).thenReturn(Collections.singletonList(progress));
        List<Word> levelWords = Arrays.asList(createWord(1L), createWord(2L));
        when(wordRepository.findByLanguageIdAndMinLevelAndTag(1L, "A1", Tag.HISTORY.name())).thenReturn(levelWords);

        List<Word> result = wordService.selectWordsForLearning(userId, languageName, minLevel, tags, currentActiveCount);

        assertNotNull(result);
        assertEquals(1, result.size()); // Только word2, так как word1 уже есть
        assertEquals(2L, result.get(0).getId());
        verify(wordRepository, times(1)).findByLanguageIdAndMinLevelAndTag(1L, "A1", Tag.HISTORY.name());
    }

    @Test
    void selectWordsForLearning_invalidLevel() {
        Long userId = 1L;
        String languageName = "English";
        String minLevel = "INVALID_LEVEL";
        List<String> tags = Collections.emptyList();
        int currentActiveCount = 10;
        Language language = new Language();
        language.setId(1L);
        language.setName(languageName);
        when(languageService.getLanguageByName(languageName)).thenReturn(language);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            wordService.selectWordsForLearning(userId, languageName, minLevel, tags, currentActiveCount);
        });

        assertEquals("Недопустимый уровень: " + minLevel, exception.getMessage());
        verify(wordRepository, never()).findByLanguageIdAndMinLevel(anyLong(), anyString());
    }

    @Test
    void selectWordsForLearning_invalidTag() {
        Long userId = 1L;
        String languageName = "English";
        String minLevel = Level.A1.name();
        List<String> tags = Arrays.asList("INVALID_TAG");
        int currentActiveCount = 0;
        Language language = new Language();
        language.setId(1L);
        language.setName(languageName);
        when(languageService.getLanguageByName(languageName)).thenReturn(language);
        when(userService.getSettings(userId)).thenReturn(Map.of("activeWordsCount", 50));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            wordService.selectWordsForLearning(userId, languageName, minLevel, tags, currentActiveCount);
        });

        assertEquals("No enum constant ci.ashamaz.languageflash.model.Tag.INVALID_TAG", exception.getMessage());
        verify(wordRepository, never()).findByLanguageIdAndMinLevelAndTag(anyLong(), anyString(), anyString());
    }

    private Word createWord(Long id) {
        Word word = new Word();
        word.setId(id);
        word.setWord("word" + id);
        word.setTranslation("перевод" + id);
        return word;
    }
}