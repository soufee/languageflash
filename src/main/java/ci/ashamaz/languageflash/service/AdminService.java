package ci.ashamaz.languageflash.service;


import ci.ashamaz.languageflash.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AdminService {

    private final UserService userService;
    private final LanguageService languageService;
    private final WordService wordService;
    private final LanguageLevelService languageLevelService;
    private final EmailService emailService;

    public AdminService(UserService userService,
                        LanguageService languageService,
                        WordService wordService,
                        LanguageLevelService languageLevelService,
                        EmailService emailService) {
        this.userService = userService;
        this.languageService = languageService;
        this.wordService = wordService;
        this.languageLevelService = languageLevelService;
        this.emailService = emailService;
    }

    public Map<String, Object> getUserListData(String username) {
        Page<User> users = userService.getAllUsers(PageRequest.of(0, 50));
        User currentUser = userService.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        Map<String, Object> model = new HashMap<>();
        model.put("users", users.getContent());
        model.put("currentUserId", currentUser.getId());
        return model;
    }

    public Map<String, Object> getLanguageListData() {
        List<Language> languages = languageService.getAllLanguages();
        List<Level> levels = Arrays.asList(Level.values());
        Map<Long, Map<Level, Boolean>> languageLevelsMap = new HashMap<>();

        for (Language language : languages) {
            Map<Level, Boolean> levelMap = new HashMap<>();
            List<LanguageLevel> languageLevels = languageLevelService.getLevelsForLanguage(language.getId());
            log.info("Language ID {}: Found {} levels", language.getId(), languageLevels.size());

            for (Level level : levels) {
                levelMap.put(level, false);
            }
            for (LanguageLevel languageLevel : languageLevels) {
                log.info("  Level {}: active = {}", languageLevel.getLevel(), languageLevel.isActive());
                levelMap.put(languageLevel.getLevel(), languageLevel.isActive());
            }
            languageLevelsMap.put(language.getId(), levelMap);
        }

        for (Map.Entry<Long, Map<Level, Boolean>> entry : languageLevelsMap.entrySet()) {
            log.info("languageLevelsMap[{}]:", entry.getKey());
            for (Map.Entry<Level, Boolean> levelEntry : entry.getValue().entrySet()) {
                log.info("  {} = {}", levelEntry.getKey(), levelEntry.getValue());
            }
        }

        Map<String, Object> model = new HashMap<>();
        model.put("languages", languages);
        model.put("levels", levels);
        model.put("languageLevelsMap", languageLevelsMap);
        return model;
    }

    public Map<String, Object> getWordListData(String filter) {
        Page<Word> words = wordService.getFilteredWords(filter, null, PageRequest.of(0, 50));
        Map<Long, Set<Tag>> wordTags = words.getContent().stream()
                .collect(Collectors.toMap(
                        Word::getId,
                        Word::getTagsAsSet
                ));
        Map<String, Object> model = new HashMap<>();
        model.put("words", words.getContent());
        model.put("wordTags", wordTags);
        model.put("languages", languageService.getAllLanguages());
        model.put("levels", Arrays.asList(Level.values()));
        model.put("tags", Arrays.asList(Tag.values()));
        return model;
    }

    public Page<User> getAllUsers(Pageable pageable) {
        return userService.getAllUsers(pageable);
    }

    public Page<User> searchUsersByEmail(String email, Pageable pageable) {
        return userService.searchUsersByEmail(email, pageable);
    }

    public void blockUser(Long userId, boolean blocked, String supportEmail) {
        User user = userService.getUserById(userId);
        if (user != null) {
            user.setBlocked(blocked);
            userService.save(user);
            try {
                emailService.sendHtmlEmail(user.getEmail(), "Статус аккаунта изменен",
                        "Ваш аккаунт был " + (blocked ? "заблокирован" : "разблокирован"));
            } catch (Exception e) {
                log.error("Ошибка отправки email: {}", e.getMessage());
            }
        }
    }

    public void toggleAdmin(Long userId, boolean isAdmin, String supportEmail) {
        User user = userService.getUserById(userId);
        if (user != null) {
            Set<String> roles = new HashSet<>(user.getRoles());
            if (isAdmin) {
                roles.add("ADMIN");
            } else {
                roles.remove("ADMIN");
            }
            user.setRoles(roles);
            userService.save(user);
            try {
                emailService.sendHtmlEmail(user.getEmail(), "Статус администратора изменен",
                        "Ваши права администратора были " + (isAdmin ? "предоставлены" : "отозваны"));
            } catch (Exception e) {
                log.error("Ошибка отправки email: {}", e.getMessage());
            }
        }
    }

    public List<Language> getAllLanguages() {
        return languageService.getAllLanguages();
    }

    public void addLanguage(String name) {
        languageService.addLanguage(name);
    }

    public void updateLanguage(Long id, String name, boolean active) {
        languageService.updateLanguage(id, name, active);
    }

    public void updateLanguageLevel(Long id, Level level, boolean active) {
        try {
            languageLevelService.updateLanguageLevel(id, level, active);
        } catch (IllegalArgumentException e) {
            log.error("Error updating language level: {}", e.getMessage());
            throw e;
        }
    }

    public Page<Word> getFilteredWords(String filter, String language, String level, Pageable pageable) {
        return wordService.getFilteredWords(filter, null, pageable);
    }

    public Map<String, Object> addWord(String word, String translation, String exampleSentence,
                                       String exampleTranslation, Long languageId, String level,
                                       List<String> tags) {
        try {
            Word newWord = wordService.addWord(word, translation, exampleSentence, exampleTranslation,
                    languageId, level, tags);
            Map<String, Object> response = new HashMap<>();
            response.put("id", newWord.getId());
            response.put("status", "success");
            return response;
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return response;
        }
    }

    public Map<String, Object> editWord(Long id, String word, String translation, String exampleSentence,
                                        String exampleTranslation, Long languageId, String level,
                                        List<String> tags) {
        try {
            Word updatedWord = wordService.addWord(word, translation, exampleSentence, exampleTranslation,
                    languageId, level, tags);
            Map<String, Object> response = new HashMap<>();
            response.put("id", updatedWord.getId());
            response.put("status", "success");
            return response;
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return response;
        }
    }
}