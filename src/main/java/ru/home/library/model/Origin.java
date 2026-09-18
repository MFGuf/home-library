package ru.home.library.model;

/**
 * Происхождение книги — откуда она появилась в библиотеке.
 * Хранится в базе данных как текстовая метка ({@link #getLabel()}).
 */
public enum Origin {

    PURCHASED("Покупка"),
    GIFT("Подарок"),
    INHERITED("По наследству"),
    OTHER("Другое");

    private final String label;

    Origin(String label) {
        this.label = label;
    }

    /** Человекочитаемая метка, которая выводится в интерфейсе и хранится в БД. */
    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }

    /**
     * Обратное преобразование: по метке находим перечисление.
     * Используется при чтении записей из базы данных.
     *
     * @param label текстовая метка из БД или интерфейса
     * @return соответствующее значение перечисления или {@code null}
     */
    public static Origin fromLabel(String label) {
        if (label == null) {
            return null;
        }
        for (Origin origin : values()) {
            if (origin.label.equalsIgnoreCase(label.trim())) {
                return origin;
            }
        }
        return null;
    }
}