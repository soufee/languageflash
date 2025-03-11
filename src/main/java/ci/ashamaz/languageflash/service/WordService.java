package ci.ashamaz.languageflash.service;

import ci.ashamaz.languageflash.model.Dictionary;
import ci.ashamaz.languageflash.model.Language;
import ci.ashamaz.languageflash.model.Word;
import ci.ashamaz.languageflash.repository.WordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class WordService {

    @Autowired
    private WordRepository wordRepository;

    @Autowired
    private LanguageService languageService;

    @Autowired
    private DictionaryService dictionaryService;

    @Transactional
    public Word addWord(String word, String translation, String exampleSentence, String exampleTranslation, Long languageId) {
        log.info("Adding word: {} for languageId: {}", word, languageId);
        Language language = languageService.getLanguageById(languageId);
        Word newWord = new Word();
        newWord.setWord(word);
        newWord.setTranslation(translation);
        newWord.setExampleSentence(exampleSentence);
        newWord.setExampleTranslation(exampleTranslation);
        newWord.setLanguage(language);
        Word savedWord = wordRepository.save(newWord);
        log.info("Word added: {}", savedWord);
        return savedWord;
    }

    public List<Word> getWordsByLanguage(Long languageId) {
        log.info("Retrieving words for languageId: {}", languageId);
        List<Word> words = wordRepository.findByLanguageId(languageId);
        log.debug("Found {} words for languageId: {}", words.size(), languageId);
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

    @Transactional
    public void addWordToDictionary(Long wordId, Long dictionaryId) {
        log.info("Adding wordId: {} to dictionaryId: {}", wordId, dictionaryId);
        Word word = getWordById(wordId);
        Dictionary dictionary = dictionaryService.getDictionary(dictionaryId);
        word.getDictionaries().add(dictionary);
        wordRepository.save(word);
        log.info("Word {} added to dictionary {}", wordId, dictionaryId);
    }

    public List<Word> getAllWords() {
        log.info("Retrieving all words");
        return wordRepository.findAll();
    }

    @Transactional
    public void save(Word word) { // Замена updateWord
        log.info("Saving word: {}", word);
        wordRepository.save(word);
    }

}