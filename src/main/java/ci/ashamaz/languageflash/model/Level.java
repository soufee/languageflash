package ci.ashamaz.languageflash.model;

public enum Level {
    A1, A2, B1, B2, C1, C2;

    public int order() {
        return ordinal() + 1;
    }

    public boolean isPremium() {
        return this == C1 || this == C2;
    }

    public static Level fromString(String value) {
        try {
            return valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Недопустимый уровень: " + value);
        }
    }
}
