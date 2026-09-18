package ru.home.library.model;

/**
 * Статус чтения книги: прочитана ли она пользователем и в каком состоянии.
 */
public enum ReadingStatus {

    NOT_READ("Не прочитана"),
    READING("Читается"),
    READ("Прочитана"),
    REREAD("Перечитана");

    private final String label;

    ReadingStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }

    public static ReadingStatus fromLabel(String label) {
        if (label == null) {
            return null;
        }
        for (ReadingStatus status : values()) {
            if (status.label.equalsIgnoreCase(label.trim())) {
                return status;
            }
        }
        return null;
    }
}