package ru.home.library.model;

/**
 * Наличие книги в данный момент: хранится ли она сейчас в библиотеке.
 * Например, книга может быть выдана знакомому или утеряна.
 */
public enum Availability {

    AVAILABLE("В наличии"),
    LOANED_OUT("Выдана"),
    LOST("Утеряна");

    private final String label;

    Availability(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }

    public static Availability fromLabel(String label) {
        if (label == null) {
            return null;
        }
        for (Availability availability : values()) {
            if (availability.label.equalsIgnoreCase(label.trim())) {
                return availability;
            }
        }
        return null;
    }
}