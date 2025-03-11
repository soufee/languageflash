package ci.ashamaz.languageflash.service;

import ci.ashamaz.languageflash.model.Dictionary;
import ci.ashamaz.languageflash.model.LanguageLevel;
import ci.ashamaz.languageflash.model.Program;
import ci.ashamaz.languageflash.model.User;
import ci.ashamaz.languageflash.repository.ProgramRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class ProgramService {

    @Autowired
    private ProgramRepository programRepository;

    @Autowired
    private LanguageLevelService languageLevelService;

    @Autowired
    private UserService userService;
    @Autowired
    private DictionaryService dictionaryService;

    @Transactional
    public Program createProgram(Long userId, Long languageLevelId) {
        log.info("Creating program for userId: {}, languageLevelId: {}", userId, languageLevelId);
        User user = userService.getUserById(userId); // Предполагается, что UserService существует
        LanguageLevel languageLevel = languageLevelService.getLanguageLevelById(languageLevelId);
        Program program = new Program();
        program.setUser(user);
        program.setLanguageLevel(languageLevel);
        Program savedProgram = programRepository.save(program);
        log.info("Program created: {}", savedProgram);
        return savedProgram;
    }

    @Transactional
    public void addDictionaryToProgram(Long programId, Long dictionaryId) {
        log.info("Adding dictionaryId: {} to programId: {}", dictionaryId, programId);
        Program program = getProgramById(programId);
        Dictionary dictionary = dictionaryService.getDictionary(dictionaryId);
        program.getDictionaries().add(dictionary);
        programRepository.save(program);
        log.info("Dictionary {} added to program {}", dictionaryId, programId);
    }


    public List<Program> getProgramsByUser(Long userId) {
        return programRepository.findByUserId(userId);
    }

    public Program getProgramById(Long id) {
        log.info("Retrieving program by id: {}", id);
        return programRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Program with id {} not found", id);
                    return new IllegalArgumentException("Программа с ID " + id + " не найдена");
                });
    }
}