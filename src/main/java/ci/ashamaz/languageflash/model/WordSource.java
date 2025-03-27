package ci.ashamaz.languageflash.model;

/**
 * Перечисление для указания источника слова в прогрессе пользователя
 */
public enum WordSource {
    PROGRAM("Программа обучения"),
    CUSTOM("Мой словарь"),
    TEXT("Из текста");
    
    private final String russianName;
    
    WordSource(String russianName) {
        this.russianName = russianName;
    }
    
    public String getRussianName() {
        return russianName;
    }
} 