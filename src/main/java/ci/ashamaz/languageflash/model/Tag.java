package ci.ashamaz.languageflash.model;

public enum Tag {
    IRREGULAR_VERBS("Неправильные глаголы", null, "#FF6F61"),
    REGULAR_VERBS("Правильные глаголы", null, "#4CAF50"),
    FAMILY_HOME("Семья, дом и быт", null, "#FFB300"),
    BUSINESS("Деловое общение", null, "#3F51B5"),
    TECHNICAL_SCIENCES("Технические науки", null, "#9C27B0"),
    SONG_LYRICS("Тексты песен", null, "#E91E63"),
    LITERATURE("Литература", null, "#2196F3"),
    PHILOSOPHY("Философия", null, "#795548"),
    MEDICINE("Медицина", null, "#00BCD4"),
    HISTORY("История", null, "#607D8B"),
    TRAVEL("Путешествия", null, "#FF9800"),
    BASIC_VOCABULARY("Базовая лексика", null, "#8BC34A"),
    MOVIES("Фильмы", null, "#F44336"),
    SCIENCE("Наука", null, "#673AB7"),
    JOURNALISM("Публицистика", null, "#CDDC39"),
    SPORTS("Спорт", null, "#009688"),
    INTERNET("Интернет", null, "#FF5722");

    private final String russianName;
    private final String imageUrl;
    private final String color;

    Tag(String russianName, String imageUrl, String color) {
        this.russianName = russianName;
        this.imageUrl = imageUrl;
        this.color = color;
    }

    public String getRussianName() {
        return russianName;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getColor() {
        return color;
    }
}