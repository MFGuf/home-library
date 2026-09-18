package ru.home.library.model;

/**
 * Автор книги. Один автор может написать несколько книг,
 * одна книга может иметь несколько авторов (связь «многие ко многим»).
 */
public class Author extends Entity {

    /** Полное имя автора (обязательное поле). */
    private String fullName;

    /** Год рождения (может быть пустым или приблизительным, например «1828»). */
    private String birthYear;

    public Author() {
    }

    public Author(String fullName) {
        this.fullName = fullName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getBirthYear() {
        return birthYear;
    }

    public void setBirthYear(String birthYear) {
        this.birthYear = birthYear;
    }

    @Override
    public String toString() {
        return fullName == null ? "" : fullName;
    }
}