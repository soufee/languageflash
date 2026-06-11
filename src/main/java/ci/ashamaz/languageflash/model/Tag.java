package ci.ashamaz.languageflash.model;

import lombok.Getter;

@Getter
public enum Tag {
    IRREGULAR_VERBS("Неправильные глаголы", "#FF6F61"),
    REGULAR_VERBS("Правильные глаголы", "#4CAF50"),
    FAMILY_HOME("Семья, дом и быт", "#FFB300"),
    BUSINESS("Деловое общение", "#3F51B5"),
    TECHNICAL_SCIENCES("Технические науки", "#9C27B0"),
    SONG_LYRICS("Тексты песен", "#E91E63"),
    LITERATURE("Литература", "#2196F3"),
    PHILOSOPHY("Философия", "#795548"),
    MEDICINE("Медицина", "#00BCD4"),
    HISTORY("История", "#607D8B"),
    TRAVEL("Путешествия", "#FF9800"),
    BASIC_VOCABULARY("Базовая лексика", "#8BC34A"),
    MOVIES("Фильмы", "#F44336"),
    SCIENCE("Наука", "#673AB7"),
    JOURNALISM("Публицистика", "#CDDC39"),
    SPORTS("Спорт", "#009688"),
    INTERNET("Интернет", "#FF5722");

    private final String russianName;
    private final String color;

    Tag(String russianName, String color) {
        this.russianName = russianName;
        this.color = color;
    }
}
