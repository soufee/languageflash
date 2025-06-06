package ci.ashamaz.languageflash.service;

import ci.ashamaz.languageflash.config.TestSecurityConfig;
import ci.ashamaz.languageflash.model.*;
import ci.ashamaz.languageflash.repository.WordProgressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ContextConfiguration(classes = {TestSecurityConfig.class})
class WordProgressServiceTest {

    @InjectMocks
    private WordProgressService wordProgressService;

    @Mock
    private WordProgressRepository wordProgressRepository;

    @Mock
    private UserService userService;

    @Mock
    private WordService wordService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // Вспомогательный метод для создания тестового слова
    private Word createWord(Long id) {
        Word word = new Word();
        word.setId(id);
        word.setWord("word" + id);
        word.setTranslation("перевод" + id);
        return word;
    }

    private CustomWord createCustomWord(Long id) {
        CustomWord word = new CustomWord();
        word.setId(id);
        word.setWord("custom" + id);
        word.setTranslation("кастом" + id);
        return word;
    }

    // Тесты для initializeProgress
    @Test
    void initializeProgress_success() {
        Long userId = 1L;
        List<Word> words = Arrays.asList(createWord(1L), createWord(2L));
        User user = new User();
        user.setId(userId);
        when(userService.getUserById(userId)).thenReturn(user);
        when(wordProgressRepository.findActiveByUserId(userId)).thenReturn(Collections.emptyList());

        wordProgressService.initializeProgress(userId, words);

        ArgumentCaptor<List<WordProgress>> captor = ArgumentCaptor.forClass(List.class);
        verify(wordProgressRepository, times(1)).saveAll(captor.capture());
        List<WordProgress> savedProgress = captor.getValue();
        assertEquals(2, savedProgress.size());
        assertEquals(1L, savedProgress.get(0).getWord().getId());
        assertEquals(2L, savedProgress.get(1).getWord().getId());
        assertEquals(1.0f, savedProgress.get(0).getKnowledgeFactor());
        assertFalse(savedProgress.get(0).isLearned());
    }

    @Test
    void initializeProgress_noWords() {
        Long userId = 1L;
        List<Word> words = Collections.emptyList();

        wordProgressService.initializeProgress(userId, words);

        verify(wordProgressRepository, never()).saveAll(anyList());
    }

    @Test
    void initializeProgress_existingWords() {
        Long userId = 1L;
        List<Word> words = Arrays.asList(createWord(1L), createWord(2L));
        User user = new User();
        user.setId(userId);
        WordProgress existingProgress = new WordProgress();
        existingProgress.setWord(createWord(1L));
        when(userService.getUserById(userId)).thenReturn(user);
        when(wordProgressRepository.findActiveByUserId(userId)).thenReturn(Collections.singletonList(existingProgress));

        wordProgressService.initializeProgress(userId, words);

        ArgumentCaptor<List<WordProgress>> captor = ArgumentCaptor.forClass(List.class);
        verify(wordProgressRepository, times(1)).saveAll(captor.capture());
        List<WordProgress> savedProgress = captor.getValue();
        assertEquals(1, savedProgress.size()); // Только word2, так как word1 уже есть
        assertEquals(2L, savedProgress.get(0).getWord().getId());
    }

    @Test
    void initializeProgress_nullUser() {
        Long userId = 1L;
        List<Word> words = Arrays.asList(createWord(1L));
        when(userService.getUserById(userId)).thenReturn(null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            wordProgressService.initializeProgress(userId, words);
        });

        assertEquals("Пользователь с ID " + userId + " не найден", exception.getMessage());
        verify(wordProgressRepository, never()).saveAll(anyList());
    }

    // Тесты для initializeSingleProgress
    @Test
    void initializeSingleProgress_success() {
        Long userId = 1L;
        Long wordId = 1L;
        User user = new User();
        user.setId(userId);
        Word word = createWord(wordId);
        when(userService.getUserById(userId)).thenReturn(user);
        when(wordService.getWordById(wordId)).thenReturn(word);
        when(wordProgressRepository.findByUserIdAndWordId(userId, wordId)).thenReturn(Optional.empty());
        // Мокаем сохранение с возвратом объекта
        WordProgress expectedProgress = new WordProgress();
        expectedProgress.setUser(user);
        expectedProgress.setWord(word);
        expectedProgress.setKnowledgeFactor(1.0f);
        expectedProgress.setLearned(false);
        expectedProgress.setLastReviewed(LocalDateTime.now());
        expectedProgress.setNextReviewDate(LocalDateTime.now());
        when(wordProgressRepository.save(any(WordProgress.class))).thenReturn(expectedProgress);

        WordProgress result = wordProgressService.initializeSingleProgress(userId, wordId, WordSource.PROGRAM, null);

        assertNotNull(result);
        assertEquals(userId, result.getUser().getId());
        assertEquals(wordId, result.getWord().getId());
        assertEquals(1.0f, result.getKnowledgeFactor());
        assertFalse(result.isLearned());
        verify(wordProgressRepository, times(1)).save(any(WordProgress.class));
    }

    @Test
    void initializeSingleProgress_existingProgress() {
        Long userId = 1L;
        Long wordId = 1L;
        WordProgress existingProgress = new WordProgress();
        existingProgress.setKnowledgeFactor(0.5f);
        when(wordProgressRepository.findByUserIdAndWordId(userId, wordId)).thenReturn(Optional.of(existingProgress));

        WordProgress result = wordProgressService.initializeSingleProgress(userId, wordId, WordSource.PROGRAM, null);

        assertEquals(existingProgress, result);
        assertEquals(0.5f, result.getKnowledgeFactor());
        verify(wordProgressRepository, never()).save(any(WordProgress.class));
    }

    // Тесты для updateProgress
    @Test
    void updateProgress_knows() {
        Long userId = 1L;
        Long wordId = 1L;
        WordProgress progress = new WordProgress();
        progress.setKnowledgeFactor(1.0f);
        progress.setLastReviewed(LocalDateTime.now().minusDays(1));
        when(wordProgressRepository.findByUserIdAndWordId(userId, wordId)).thenReturn(Optional.of(progress));

        wordProgressService.updateProgress(userId, wordId, true);

        assertEquals(0.75f, progress.getKnowledgeFactor(), 0.001);
        assertNotNull(progress.getNextReviewDate());
        assertTrue(progress.getNextReviewDate().isAfter(progress.getLastReviewed()));
        verify(wordProgressRepository, times(1)).save(progress);
    }

    @Test
    void updateProgress_doesNotKnow() {
        Long userId = 1L;
        Long wordId = 1L;
        WordProgress progress = new WordProgress();
        progress.setKnowledgeFactor(1.0f);
        progress.setLastReviewed(LocalDateTime.now().minusDays(1));
        when(wordProgressRepository.findByUserIdAndWordId(userId, wordId)).thenReturn(Optional.of(progress));

        wordProgressService.updateProgress(userId, wordId, false);

        assertEquals(1.3f, progress.getKnowledgeFactor(), 0.001);
        assertNotNull(progress.getNextReviewDate());
        assertTrue(progress.getNextReviewDate().isAfter(progress.getLastReviewed()));
        verify(wordProgressRepository, times(1)).save(progress);
    }

    @Test
    void updateProgress_learned() {
        Long userId = 1L;
        Long wordId = 1L;
        WordProgress progress = new WordProgress();
        progress.setKnowledgeFactor(0.1f);
        progress.setLastReviewed(LocalDateTime.now().minusDays(1));
        when(wordProgressRepository.findByUserIdAndWordId(userId, wordId)).thenReturn(Optional.of(progress));

        wordProgressService.updateProgress(userId, wordId, true);

        assertEquals(0.0f, progress.getKnowledgeFactor(), 0.001);
        assertTrue(progress.isLearned());
        verify(wordProgressRepository, times(1)).save(progress);
    }

    @Test
    void updateProgress_newProgress() {
        Long userId = 1L;
        Long wordId = 1L;
        User user = new User();
        user.setId(userId);
        Word word = createWord(wordId);
        when(wordProgressRepository.findByUserIdAndWordId(userId, wordId)).thenReturn(Optional.empty());
        when(userService.getUserById(userId)).thenReturn(user);
        when(wordService.getWordById(wordId)).thenReturn(word);
        // Мокаем сохранение для initializeSingleProgress
        WordProgress initialProgress = new WordProgress();
        initialProgress.setUser(user);
        initialProgress.setWord(word);
        initialProgress.setKnowledgeFactor(1.0f);
        initialProgress.setLearned(false);
        initialProgress.setLastReviewed(LocalDateTime.now());
        initialProgress.setNextReviewDate(LocalDateTime.now());
        when(wordProgressRepository.save(any(WordProgress.class))).thenReturn(initialProgress);

        wordProgressService.updateProgress(userId, wordId, true);

        ArgumentCaptor<WordProgress> captor = ArgumentCaptor.forClass(WordProgress.class);
        verify(wordProgressRepository, times(2)).save(captor.capture()); // 1 раз в initialize, 1 раз в update
        List<WordProgress> savedProgressList = captor.getAllValues();
        WordProgress savedProgress = savedProgressList.get(1); // Берем второй вызов (update)
        assertEquals(0.75f, savedProgress.getKnowledgeFactor(), 0.001);
        assertFalse(savedProgress.isLearned());
    }

    @Test
    void updateProgress_nullProgress() {
        Long userId = 1L;
        Long wordId = 1L;
        User user = new User();
        user.setId(userId);
        Word word = createWord(wordId);
        when(wordProgressRepository.findByUserIdAndWordId(userId, wordId)).thenReturn(Optional.empty());
        when(userService.getUserById(userId)).thenReturn(user);
        when(wordService.getWordById(wordId)).thenReturn(word);

        WordProgress initialProgress = new WordProgress();
        initialProgress.setUser(user);
        initialProgress.setWord(word);
        when(wordProgressRepository.save(any(WordProgress.class))).thenReturn(initialProgress);

        wordProgressService.updateProgress(userId, wordId, true);

        verify(wordProgressRepository, times(2)).save(any(WordProgress.class));
    }

    @Test
    void updateProgress_expiredReviewDate() {
        Long userId = 1L;
        Long wordId = 1L;
        WordProgress progress = new WordProgress();
        progress.setKnowledgeFactor(1.0f);
        progress.setLastReviewed(LocalDateTime.now().minusDays(30));
        progress.setNextReviewDate(LocalDateTime.now().minusDays(29));
        when(wordProgressRepository.findByUserIdAndWordId(userId, wordId)).thenReturn(Optional.of(progress));

        wordProgressService.updateProgress(userId, wordId, true);

        assertTrue(progress.getNextReviewDate().isAfter(LocalDateTime.now()));
        verify(wordProgressRepository, times(1)).save(progress);
    }

    @Test
    void updateProgress_maxKnowledgeFactor() {
        Long userId = 1L;
        Long wordId = 1L;
        WordProgress progress = new WordProgress();
        progress.setKnowledgeFactor(10.0f);
        when(wordProgressRepository.findByUserIdAndWordId(userId, wordId)).thenReturn(Optional.of(progress));

        wordProgressService.updateProgress(userId, wordId, false);

        ArgumentCaptor<WordProgress> captor = ArgumentCaptor.forClass(WordProgress.class);
        verify(wordProgressRepository, times(1)).save(captor.capture());
        WordProgress savedProgress = captor.getValue();
        assertEquals(10.0f, savedProgress.getKnowledgeFactor(), 0.001);
        assertFalse(savedProgress.isLearned());
    }

    @Test
    void updateProgress_minKnowledgeFactor() {
        Long userId = 1L;
        Long wordId = 1L;
        WordProgress progress = new WordProgress();
        progress.setKnowledgeFactor(0.1f);
        progress.setLastReviewed(LocalDateTime.now().minusDays(1));
        when(wordProgressRepository.findByUserIdAndWordId(userId, wordId)).thenReturn(Optional.of(progress));

        wordProgressService.updateProgress(userId, wordId, false);

        assertEquals(0.13f, progress.getKnowledgeFactor(), 0.001);
        verify(wordProgressRepository, times(1)).save(progress);
    }

    // Тесты для getActiveProgress
    @Test
    void getActiveProgress_success() {
        Long userId = 1L;
        List<WordProgress> progressList = Arrays.asList(new WordProgress(), new WordProgress());
        when(wordProgressRepository.findActiveByUserId(userId)).thenReturn(progressList);

        List<WordProgress> result = wordProgressService.getActiveProgress(userId);

        assertEquals(2, result.size());
        assertEquals(progressList, result);
        verify(wordProgressRepository, times(1)).findActiveByUserId(userId);
    }

    @Test
    void getActiveProgress_empty() {
        Long userId = 1L;
        when(wordProgressRepository.findActiveByUserId(userId)).thenReturn(Collections.emptyList());

        List<WordProgress> result = wordProgressService.getActiveProgress(userId);

        assertTrue(result.isEmpty());
        verify(wordProgressRepository, times(1)).findActiveByUserId(userId);
    }

    // Тесты для getLearnedProgress
    @Test
    void getLearnedProgress_success() {
        Long userId = 1L;
        List<WordProgress> progressList = Arrays.asList(new WordProgress(), new WordProgress());
        when(wordProgressRepository.findLearnedByUserId(userId)).thenReturn(progressList);

        List<WordProgress> result = wordProgressService.getLearnedProgress(userId);

        assertEquals(2, result.size());
        assertEquals(progressList, result);
        verify(wordProgressRepository, times(1)).findLearnedByUserId(userId);
    }

    @Test
    void getLearnedProgress_empty() {
        Long userId = 1L;
        when(wordProgressRepository.findLearnedByUserId(userId)).thenReturn(Collections.emptyList());

        List<WordProgress> result = wordProgressService.getLearnedProgress(userId);

        assertTrue(result.isEmpty());
        verify(wordProgressRepository, times(1)).findLearnedByUserId(userId);
    }

    // Тесты для getProgress
    @Test
    void getProgress_success() {
        Long userId = 1L;
        Long wordId = 1L;
        WordProgress progress = new WordProgress();
        when(wordProgressRepository.findByUserIdAndWordId(userId, wordId)).thenReturn(Optional.of(progress));

        WordProgress result = wordProgressService.getProgress(userId, wordId);

        assertEquals(progress, result);
        verify(wordProgressRepository, times(1)).findByUserIdAndWordId(userId, wordId);
    }

    @Test
    void getProgress_notFound() {
        Long userId = 1L;
        Long wordId = 1L;
        when(wordProgressRepository.findByUserIdAndWordId(userId, wordId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            wordProgressService.getProgress(userId, wordId);
        });

        assertEquals("Прогресс для слова " + wordId + " и пользователя " + userId + " не найден", exception.getMessage());
        verify(wordProgressRepository, times(1)).findByUserIdAndWordId(userId, wordId);
    }

    // Тесты для resetProgress
    @Test
    void resetProgress_success() {
        Long userId = 1L;
        List<WordProgress> progressList = Arrays.asList(new WordProgress(), new WordProgress());
        when(wordProgressRepository.findByUserId(userId)).thenReturn(progressList);

        wordProgressService.resetProgress(userId);

        verify(wordProgressRepository, times(1)).deleteAll(progressList);
    }

    @Test
    void resetProgress_empty() {
        Long userId = 1L;
        when(wordProgressRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

        wordProgressService.resetProgress(userId);

        verify(wordProgressRepository, times(1)).deleteAll(Collections.emptyList());
    }

    // Тесты для save
    @Test
    void save_success() {
        WordProgress progress = new WordProgress();
        User user = new User();
        user.setId(1L);
        Word word = createWord(1L);
        progress.setUser(user);
        progress.setWord(word);

        wordProgressService.save(progress);

        verify(wordProgressRepository, times(1)).save(progress);
    }

    // Тесты для getCustomWordsProgress
    @Test
    void getCustomWordsProgress_success() {
        Long userId = 1L;
        WordProgress customProgress = new WordProgress();
        customProgress.setWord(createCustomWord(1L));
        WordProgress regularProgress = new WordProgress();
        regularProgress.setWord(createWord(2L));
        List<WordProgress> progressList = Arrays.asList(customProgress, regularProgress);
        when(wordProgressRepository.findByUserId(userId)).thenReturn(progressList);

        List<WordProgress> result = wordProgressService.getCustomWordsProgress(userId);

        assertEquals(1, result.size());
        assertEquals(customProgress, result.get(0));
        verify(wordProgressRepository, times(1)).findByUserId(userId);
    }

    @Test
    void getCustomWordsProgress_noCustomWords() {
        Long userId = 1L;
        WordProgress regularProgress = new WordProgress();
        regularProgress.setWord(createWord(1L));
        List<WordProgress> progressList = Collections.singletonList(regularProgress);
        when(wordProgressRepository.findByUserId(userId)).thenReturn(progressList);

        List<WordProgress> result = wordProgressService.getCustomWordsProgress(userId);

        assertTrue(result.isEmpty());
        verify(wordProgressRepository, times(1)).findByUserId(userId);
    }

    @Test
    void getCustomWordsProgress_empty() {
        Long userId = 1L;
        when(wordProgressRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

        List<WordProgress> result = wordProgressService.getCustomWordsProgress(userId);

        assertTrue(result.isEmpty());
        verify(wordProgressRepository, times(1)).findByUserId(userId);
    }
}