package ci.ashamaz.languageflash.service;

import ci.ashamaz.languageflash.exception.ApiException;
import ci.ashamaz.languageflash.model.Language;
import ci.ashamaz.languageflash.model.Word;
import ci.ashamaz.languageflash.repository.LanguageRepository;
import ci.ashamaz.languageflash.repository.WordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WordImportServiceTest {

    @Mock private WordRepository wordRepository;
    @Mock private LanguageRepository languageRepository;

    private WordImportService service;

    @BeforeEach
    void setUp() {
        service = new WordImportService(wordRepository, languageRepository);
        Language english = new Language();
        english.setId(6L);
        english.setName("English");
        lenient().when(languageRepository.findByName("English")).thenReturn(Optional.of(english));
        lenient().when(wordRepository.findByLanguageIdAndWord(anyLong(), anyString())).thenReturn(List.of());
        lenient().when(wordRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void importsValidFile() {
        String json = """
                {"language":"English","level":"B2","words":[
                  {"word":"ubiquitous","translation":"вездесущий",
                   "exampleSentence":"Mobile phones are ubiquitous.",
                   "exampleTranslation":"Телефоны вездесущи.",
                   "tags":["BUSINESS","BASIC_VOCABULARY"]}]}
                """;
        var result = service.importJson(json.getBytes(StandardCharsets.UTF_8));
        assertEquals(1, result.imported());
        assertEquals(0, result.skipped());
    }

    @Test
    void rejectsUnknownLanguage() {
        String json = """
                {"language":"Klingon","level":"A1","words":[{"word":"a","translation":"б"}]}
                """;
        ApiException e = assertThrows(ApiException.class,
                () -> service.importJson(json.getBytes(StandardCharsets.UTF_8)));
        assertTrue(e.getMessage().contains("Klingon"));
    }

    @Test
    void rejectsInvalidTag() {
        String json = """
                {"language":"English","level":"A1","words":[
                  {"word":"a","translation":"б","tags":["NOT_A_TAG"]}]}
                """;
        ApiException e = assertThrows(ApiException.class,
                () -> service.importJson(json.getBytes(StandardCharsets.UTF_8)));
        assertTrue(e.getMessage().contains("NOT_A_TAG"));
    }

    @Test
    void rejectsTooLongWord() {
        String json = "{\"language\":\"English\",\"level\":\"A1\",\"words\":[{\"word\":\""
                + "x".repeat(101) + "\",\"translation\":\"б\"}]}";
        assertThrows(ApiException.class, () -> service.importJson(json.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void rejectsBrokenJson() {
        assertThrows(ApiException.class, () -> service.importJson("not json".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void skipsDuplicates() {
        Word existing = new Word();
        when(wordRepository.findByLanguageIdAndWord(6L, "ubiquitous")).thenReturn(List.of(existing));
        String json = """
                {"language":"English","level":"B2","words":[
                  {"word":"ubiquitous","translation":"вездесущий"}]}
                """;
        var result = service.importJson(json.getBytes(StandardCharsets.UTF_8));
        assertEquals(0, result.imported());
        assertEquals(1, result.skipped());
    }
}
