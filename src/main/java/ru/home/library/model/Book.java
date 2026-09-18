package ru.home.library.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Книга — центральная сущность картотеки.
 * <p>
 * Содержит выходные данные книги (название, издательство, год, ISBN),
 * ссылку на раздел библиотеки, происхождение, наличие, статус чтения
 * и субъективную оценку пользователя (от 1 до 10).
 * Также хранит списки авторов (агрегация) и физических экземпляров.
 */
public class Book extends Entity {

    /** Название книги (обязательное поле). */
    private String title;

    /** Издательство. */
    private String publisher;

    /** Год издания — строка, чтобы допускать значения вида «около 1990». */
    private String publicationYear;

    /** Международный стандартный книжный номер. */
    private String isbn;

    /** Раздел библиотеки (может отсутствовать). */
    private Category category;

    /** Происхождение книги. */
    private Origin origin;

    /** Наличие книги в данный момент. */
    private Availability availability;

    /** Статус чтения. */
    private ReadingStatus readingStatus;

    /** Субъективная оценка от 1 до 10; {@code null} — книга не оценена. */
    private Integer rating;

    /** Авторы книги (связь «многие ко многим» через таблицу book_authors). */
    private final List<Author> authors = new ArrayList<>();

    /** Физические экземпляры книги (связь «один ко многим», таблица book_copies). */
    private final List<BookCopy> copies = new ArrayList<>();

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getPublicationYear() {
        return publicationYear;
    }

    public void setPublicationYear(String publicationYear) {
        this.publicationYear = publicationYear;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Origin getOrigin() {
        return origin;
    }

    public void setOrigin(Origin origin) {
        this.origin = origin;
    }

    public Availability getAvailability() {
        return availability;
    }

    public void setAvailability(Availability availability) {
        this.availability = availability;
    }

    public ReadingStatus getReadingStatus() {
        return readingStatus;
    }

    public void setReadingStatus(ReadingStatus readingStatus) {
        this.readingStatus = readingStatus;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public List<Author> getAuthors() {
        return authors;
    }

    public List<BookCopy> getCopies() {
        return copies;
    }
}