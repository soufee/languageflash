package ci.ashamaz.languageflash.service;

import ci.ashamaz.languageflash.model.Language;
import ci.ashamaz.languageflash.model.Tag;
import ci.ashamaz.languageflash.model.Word;
import ci.ashamaz.languageflash.repository.WordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class WordService {

    @Autowired
    private WordRepository wordRepository;

    @Autowired
    private LanguageService languageService;

    public List<Word> getWordsByLanguage(Long languageId) {
        log.info("Retrieving words for languageId: {}", languageId);
        List<Word> words = wordRepository.findByLanguageId(languageId);
        log.debug("Found {} words for languageId: {}", words.size(), languageId);
        return words;
    }

    public List<Word> getWordsByLanguageAndMinLevel(Long languageId, String minLevel) {
        log.info("Retrieving words for languageId: {} with minLevel: {}", languageId, minLevel);
        List<Word> words = wordRepository.findByLanguageIdAndMinLevel(languageId, minLevel);
        log.debug("Found {} words for languageId: {} and minLevel: {}", words.size(), languageId, minLevel);
        return words;
    }

    public List<Word> getWordsByLanguageLevelAndTag(Long languageId, String minLevel, String tag) {
        log.info("Retrieving words for languageId: {}, minLevel: {}, tag: {}", languageId, minLevel, tag);
        List<Word> words = wordRepository.findByLanguageIdAndMinLevelAndTag(languageId, minLevel, tag);
        log.debug("Found {} words for languageId: {}, minLevel: {}, tag: {}", words.size(), languageId, minLevel, tag);
        return words;
    }

    public Word getWordById(Long id) {
        log.info("Retrieving word by id: {}", id);
        return wordRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Word with id {} not found", id);
                    return new IllegalArgumentException("Слово с ID " + id + " не найдено");
                });
    }

    public List<Word> getAllWords() {
        log.info("Retrieving all words");
        return wordRepository.findAll();
    }

    @Transactional
    public void save(Word word) {
        log.info("Saving word: {}", word);
        wordRepository.save(word);
    }

    @Transactional
    public Word addWord(String word, String translation, String exampleSentence, String exampleTranslation,
                        Long languageId, String level, List<String> tags) {
        log.info("Adding word: {} for languageId: {}, level: {}, tags: {}", word, languageId, level, tags);
        Language language = languageService.getLanguageById(languageId);
        Word newWord = new Word();
        newWord.setWord(word);
        newWord.setTranslation(translation);
        newWord.setExampleSentence(exampleSentence);
        newWord.setExampleTranslation(exampleTranslation);
        newWord.setLanguage(language);
        newWord.setLevel(level);
        Set<Tag> tagSet = tags != null
                ? tags.stream().map(Tag::valueOf).collect(Collectors.toSet())
                : null;
        newWord.setTagsAsSet(tagSet); // Передаём Set<Tag>
        Word savedWord = wordRepository.save(newWord);
        log.info("Word added: {}", savedWord);
        return savedWord;
    }
}