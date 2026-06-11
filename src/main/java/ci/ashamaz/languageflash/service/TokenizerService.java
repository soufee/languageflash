package ci.ashamaz.languageflash.service;

import org.springframework.stereotype.Service;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Токенизация текста на абзацы, предложения и слова (ТЗ 3.2.3).
 * Используется java.text.BreakIterator (Unicode-правила сегментации);
 * при необходимости заменяется на Apache OpenNLP без изменения контракта.
 */
@Service
public class TokenizerService {

    public record Token(String text, boolean isWord) {}
    public record Sentence(String text, List<Token> tokens) {}
    public record Paragraph(List<Sentence> sentences) {}

    public List<Paragraph> parse(String text) {
        List<Paragraph> paragraphs = new ArrayList<>();
        for (String para : text.split("\\n\\s*\\n|\\r\\n\\s*\\r\\n")) {
            String trimmed = para.replaceAll("\\s+", " ").trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            paragraphs.add(new Paragraph(splitSentences(trimmed)));
        }
        return paragraphs;
    }

    private List<Sentence> splitSentences(String paragraph) {
        List<Sentence> sentences = new ArrayList<>();
        BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.ROOT);
        iterator.setText(paragraph);
        int start = iterator.first();
        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            String sentence = paragraph.substring(start, end).trim();
            if (!sentence.isEmpty()) {
                sentences.add(new Sentence(sentence, tokenize(sentence)));
            }
        }
        return sentences;
    }

    private List<Token> tokenize(String sentence) {
        List<Token> tokens = new ArrayList<>();
        BreakIterator iterator = BreakIterator.getWordInstance(Locale.ROOT);
        iterator.setText(sentence);
        int start = iterator.first();
        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            String token = sentence.substring(start, end);
            if (token.isBlank()) {
                continue;
            }
            boolean isWord = token.codePoints().anyMatch(Character::isLetter);
            tokens.add(new Token(token, isWord));
        }
        return tokens;
    }
}
