package ci.ashamaz.languageflash.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TokenizerServiceTest {

    private final TokenizerService tokenizer = new TokenizerService();

    @Test
    void splitsParagraphsSentencesAndWords() {
        String text = "Hello world. This is a test!\n\nSecond paragraph here.";
        List<TokenizerService.Paragraph> result = tokenizer.parse(text);

        assertEquals(2, result.size());
        assertEquals(2, result.get(0).sentences().size());
        assertEquals("Hello world.", result.get(0).sentences().get(0).text());
    }

    @Test
    void distinguishesWordsFromPunctuation() {
        var result = tokenizer.parse("Hello, world!");
        var tokens = result.get(0).sentences().get(0).tokens();

        long words = tokens.stream().filter(TokenizerService.Token::isWord).count();
        long punctuation = tokens.stream().filter(t -> !t.isWord()).count();
        assertEquals(2, words);
        assertTrue(punctuation >= 2);
    }

    @Test
    void handlesEmptyText() {
        assertTrue(tokenizer.parse("   \n\n  ").isEmpty());
    }

    @Test
    void handlesCyrillicText() {
        var result = tokenizer.parse("Привет, мир! Как дела?");
        assertEquals(1, result.size());
        assertEquals(2, result.get(0).sentences().size());
    }
}
