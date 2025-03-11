package ci.ashamaz.languageflash.service;

import ci.ashamaz.languageflash.dto.DictionaryDTO;
import ci.ashamaz.languageflash.model.Dictionary;
import ci.ashamaz.languageflash.model.Language;
import ci.ashamaz.languageflash.model.LanguageLevel;
import ci.ashamaz.languageflash.model.Word;
import ci.ashamaz.languageflash.repository.DictionaryRepository;
import ci.ashamaz.languageflash.repository.WordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DictionaryService {

    @Autowired
    private DictionaryRepository dictionaryRepository;

    @Autowired
    private LanguageLevelService languageLevelService;

    @Autowired
    private WordRepository wordRepository;

    @Transactional
    public Dictionary addDictionary(String name, Long languageLevelId, String theme) {
        log.info("Adding dictionary: {} for languageLevelId: {}, theme: {}", name, languageLevelId, theme);
        LanguageLevel languageLevel = languageLevelService.getLanguageLevelById(languageLevelId);
        Dictionary dictionary = new Dictionary();
        dictionary.setName(name);
        dictionary.setLanguageLevel(languageLevel);
        dictionary.setTheme(theme);
        Dictionary savedDictionary = dictionaryRepository.save(dictionary);
        log.info("Dictionary added: {}", savedDictionary);
        return savedDictionary;
    }

    public List<Dictionary> getDictionariesByLanguageLevel(Long languageLevelId) {
        log.info("Retrieving dictionaries for languageLevelId: {}", languageLevelId);
        List<Dictionary> dictionaries = dictionaryRepository.findByLanguageLevelId(languageLevelId);
        log.debug("Found {} dictionaries for languageLevelId: {}", dictionaries.size(), languageLevelId);
        return dictionaries;
    }

    public Dictionary getDictionary(Long id) {
        log.info("Retrieving dictionary by id: {}", id);
        return dictionaryRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Dictionary with id {} not found", id);
                    return new IllegalArgumentException("Словарь с ID " + id + " не найден");
                });
    }

    public Map<String, Map<String, List<DictionaryDTO>>> getGroupedDictionaries() {
        log.info("Retrieving all dictionaries grouped by language and level");
        List<Dictionary> dictionaries = dictionaryRepository.findAll();
        Map<Long, Long> wordCounts = getWordCounts();

        List<DictionaryDTO> dtos = new ArrayList<>();
        for (Dictionary dict : dictionaries) {
            Long wordCount = wordCounts.getOrDefault(dict.getId(), 0L);
            DictionaryDTO dto = new DictionaryDTO();
            dto.setId(dict.getId());
            dto.setName(dict.getName());
            dto.setLanguageName(dict.getLanguageLevel().getLanguage().getName());
            dto.setLevel(dict.getLanguageLevel().getLevel().name());
            dto.setTheme(dict.getTheme());
            dto.setWordCount(wordCount.intValue());
            dto.setLanguageLevelId(dict.getLanguageLevel().getId());
            dtos.add(dto);
        }

        return dtos.stream()
                .collect(Collectors.groupingBy(
                        DictionaryDTO::getLanguageName,
                        Collectors.groupingBy(DictionaryDTO::getLevel)));
    }

    private Map<Long, Long> getWordCounts() {
        return dictionaryRepository.countWordsByDictionary();
    }

    public Page<Word> getWordsInDictionary(Long dictionaryId, int page, int size) {
        log.info("Retrieving words for dictionaryId: {} with page: {}, size: {}", dictionaryId, page, size);
        Pageable pageable = PageRequest.of(page, size);
        return dictionaryRepository.findWordsByDictionaryId(dictionaryId, pageable);
    }

    public Map<Language, Map<String, List<DictionaryDTO>>> getDictionariesByLanguage() {
        List<Dictionary> dictionaries = dictionaryRepository.findAll();
        Map<Language, Map<String, List<DictionaryDTO>>> result = new TreeMap<>(Comparator.comparing(Language::getName));

        List<String> levelOrder = Arrays.asList("A1", "A2", "B1", "B2", "C1", "C2");
        Comparator<String> levelComparator = Comparator.comparingInt(levelOrder::indexOf);

        for (Dictionary dict : dictionaries) {
            Language language = dict.getLanguageLevel().getLanguage();
            String level = dict.getLanguageLevel().getLevel().name();

            Map<String, List<DictionaryDTO>> levelMap = result.computeIfAbsent(language, k -> new TreeMap<>(levelComparator));
            List<DictionaryDTO> dictList = levelMap.computeIfAbsent(level, k -> new ArrayList<>());

            Long wordCount = dictionaryRepository.countWordsByDictionaryId(dict.getId());

            DictionaryDTO dto = new DictionaryDTO();
            dto.setId(dict.getId());
            dto.setName(dict.getName());
            dto.setLanguageName(language.getName());
            dto.setLevel(level);
            dto.setTheme(dict.getTheme());
            dto.setWordCount(wordCount != null ? wordCount.intValue() : 0);
            dto.setLanguageLevelId(dict.getLanguageLevel().getId());
            dictList.add(dto);
        }
        return result;
    }

    @Transactional
    public void addWordToDictionary(Long dictionaryId, Long wordId) {
        Dictionary dictionary = getDictionary(dictionaryId);
        Word word = wordRepository.findById(wordId)
                .orElseThrow(() -> new IllegalArgumentException("Слово с ID " + wordId + " не найдено"));
        dictionary.getWords().add(word);
        dictionaryRepository.save(dictionary);
    }

    @Transactional
    public void removeWordFromDictionary(Long dictionaryId, Long wordId) {
        Dictionary dictionary = getDictionary(dictionaryId);
        Word word = wordRepository.findById(wordId)
                .orElseThrow(() -> new IllegalArgumentException("Слово с ID " + wordId + " не найдено"));
        dictionary.getWords().remove(word);
        dictionaryRepository.save(dictionary);
    }
}