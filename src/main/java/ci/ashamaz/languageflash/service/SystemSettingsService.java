package ci.ashamaz.languageflash.service;

import ci.ashamaz.languageflash.model.SystemSetting;
import ci.ashamaz.languageflash.repository.SystemSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Динамическая конфигурация платформы «ключ-значение» (ТЗ 4.1.7). */
@Service
public class SystemSettingsService {

    public static final String FREE_DICTIONARY_LIMIT = "free_dictionary_limit";
    public static final String ADS_ENABLED_WEB = "ads_enabled_web";
    public static final String ADS_ENABLED_MOBILE = "ads_enabled_mobile";
    public static final String PROFANITY_WORDS = "profanity_words";

    private final SystemSettingRepository repository;

    public SystemSettingsService(SystemSettingRepository repository) {
        this.repository = repository;
    }

    public String get(String key, String defaultValue) {
        return repository.findById(key).map(SystemSetting::getValue).orElse(defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(get(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return Boolean.parseBoolean(get(key, String.valueOf(defaultValue)));
    }

    public int freeDictionaryLimit() {
        return getInt(FREE_DICTIONARY_LIMIT, 100);
    }

    public List<SystemSetting> getAll() {
        return repository.findAll();
    }

    public Map<String, String> getAllAsMap() {
        return repository.findAll().stream()
                .collect(Collectors.toMap(SystemSetting::getKey,
                        s -> s.getValue() == null ? "" : s.getValue()));
    }

    @Transactional
    public void update(Map<String, String> updates) {
        updates.forEach((key, value) -> {
            SystemSetting setting = repository.findById(key).orElseGet(() -> {
                SystemSetting s = new SystemSetting();
                s.setKey(key);
                s.setType("STRING");
                return s;
            });
            setting.setValue(value);
            repository.save(setting);
        });
    }
}
