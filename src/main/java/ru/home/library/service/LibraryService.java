package ru.home.library.service;

import ru.home.library.dao.AuthorDao;
import ru.home.library.dao.BookCopyDao;
import ru.home.library.dao.BookDao;
import ru.home.library.dao.CategoryDao;
import ru.home.library.database.Database;
import ru.home.library.model.Author;
import ru.home.library.model.Availability;
import ru.home.library.model.Book;
import ru.home.library.model.BookCopy;
import ru.home.library.model.Category;
import ru.home.library.model.Origin;
import ru.home.library.model.ReadingStatus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * Сервисный слой приложения (фасад).
 * <p>
 * Содержит бизнес-логику и правила целостности данных. Контроллеры
 * графического интерфейса работают только с этим классом и не обращаются
 * к базе данных напрямую, что обеспечивает модульность: при замене
 * хранилища или способа доступа к данным UI менять не придётся.
 */
public class LibraryService {

    private final Database database;
    private final AuthorDao authorDao;
    private final CategoryDao categoryDao;
    private final BookDao bookDao;
    private final BookCopyDao bookCopyDao;

    public LibraryService(Database database) {
        this.database = database;
        this.authorDao = new AuthorDao(database);
        this.categoryDao = new CategoryDao(database);
        this.bookDao = new BookDao(database);
        this.bookCopyDao = new BookCopyDao(database);
    }

    // ==================== КАТЕГОРИИ ====================

    public List<Category> getAllCategories() {
        return categoryDao.findAll();
    }

    /**
     * Сохраняет категорию. Если категория с таким именем уже существует,
     * новая запись не создаётся, а возвращается уже имеющаяся —
     * так запрещаются дубликаты разделов.
     */
    public Category saveCategory(Category category) {
        validateName(category.getName(), "Название раздела");
        if (category.isNew()) {
            Optional<Category> existing = categoryDao.findByName(category.getName());
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        return categoryDao.save(category);
    }

    /** Удаляет категорию; запрещено удалять раздел, в котором есть книги. */
    public void deleteCategory(Category category) {
        if (bookDao.countByCategory(category.getId()) > 0) {
            throw new IllegalStateException(
                    "Нельзя удалить раздел «" + category.getName()
                            + "»: в нём находятся книги. Сначала перенесите или удалите их.");
        }
        categoryDao.delete(category.getId());
    }

    // ==================== АВТОРЫ ====================

    public List<Author> getAllAuthors() {
        return authorDao.findAll();
    }

    public List<Author> getAuthorsOf(Book book) {
        return List.copyOf(book.getAuthors());
    }

    /** Сохраняет автора; при совпадении имени возвращает существующего. */
    public Author saveAuthor(Author author) {
        validateName(author.getFullName(), "Имя автора");
        if (author.isNew()) {
            Optional<Author> existing = authorDao.findByFullName(author.getFullName());
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        return authorDao.save(author);
    }

    /** Удаляет автора; запрещено удалять автора, имеющего книги. */
    public void deleteAuthor(Author author) {
        if (authorDao.countBooksOf(author.getId()) > 0) {
            throw new IllegalStateException(
                    "Нельзя удалить автора «" + author.getFullName()
                            + "»: с ним связаны книги. Сначала отвяжите автора от книг.");
        }
        authorDao.delete(author.getId());
    }

    /** Сколько книг связано с автором (для таблицы на вкладке «Авторы»). */
    public long countBooksOf(Author author) {
        return authorDao.countBooksOf(author.getId());
    }

    /** Сколько книг относится к разделу (для таблицы на вкладке «Разделы»). */
    public long countBooksOf(Category category) {
        return bookDao.countByCategory(category.getId());
    }

    /** Адрес используемого файла базы данных (для строки состояния окна). */
    public String getDatabaseUrl() {
        return database.getUrl();
    }

    // ==================== КНИГИ ====================

    public List<Book> getAllBooks() {
        return bookDao.findAll();
    }

    /**
     * Поиск книг по произвольному запросу и набору фильтров (любой параметр может быть null).
     * <p>
     * Структурные фильтры (раздел, статус, наличие, оценка) выполняются на
     * уровне базы данных, а свободный текстовый запрос — в памяти приложения:
     * стандартные средства LIKE в SQLite сравнивают без учёта регистра
     * только латиницу, а здесь поиск корректен и для русских слов.
     */
    public List<Book> searchBooks(String query, Category category, ReadingStatus status,
                                  Availability availability, Integer minRating) {
        List<Book> filtered = bookDao.search(category, status, availability, minRating);
        String text = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (text.isBlank()) {
            return filtered;
        }
        return filtered.stream()
                .filter(book -> matchesTextQuery(book, text))
                .collect(Collectors.toList());
    }

    /** Проверяет, встречается ли поисковый текст в данных книги (без учёта регистра). */
    private boolean matchesTextQuery(Book book, String text) {
        return contains(book.getTitle(), text)
                || contains(book.getPublisher(), text)
                || contains(book.getPublicationYear(), text)
                || contains(book.getIsbn(), text)
                || book.getAuthors().stream().anyMatch(author -> contains(author.getFullName(), text));
    }

    private boolean contains(String value, String text) {
        if (value == null) {
            return false;
        }
        return value.toLowerCase(Locale.ROOT).contains(text);
    }

    /**
     * Сохраняет книгу вместе с её авторами и экземплярами.
     * Сначала сохраняется сама запись книги и связи с авторами,
     * затем экземпляры полностью заменяются на переданный список.
     */
    public void saveBook(Book book) {
        validateName(book.getTitle(), "Название книги");
        bookDao.save(book);
        bookCopyDao.deleteByBookId(book.getId());
        for (BookCopy copy : book.getCopies()) {
            copy.setBookId(book.getId());
            bookCopyDao.save(copy);
        }
    }

    /** Удаляет книгу вместе с её экземплярами (каскадно). */
    public void deleteBook(Book book) {
        bookCopyDao.deleteByBookId(book.getId());
        bookDao.delete(book.getId());
    }

    // ==================== ИНВЕНТАРИЗАЦИЯ ====================

    /**
     * Формирует сводку по состоянию библиотеки для экрана инвентаризации.
     * Результат — строковый отчёт: количество книг, экземпляров,
     * книг в наличии / выданных / утерянных.
     */
    public String buildInventorySummary(List<Book> books) {
        long totalCopies = books.stream().mapToLong(b -> b.getCopies().size()).sum();
        long available = countByAvailability(books, Availability.AVAILABLE);
        long loaned = countByAvailability(books, Availability.LOANED_OUT);
        long lost = countByAvailability(books, Availability.LOST);
        return String.format(
                "Книг в картотеке: %d · Экземпляров всего: %d · В наличии: %d · Выданы: %d · Утеряны: %d",
                books.size(), totalCopies, available, loaned, lost);
    }

    private long countByAvailability(List<Book> books, Availability availability) {
        return books.stream().filter(b -> b.getAvailability() == availability).count();
    }

    /**
     * Экспорт картотеки в CSV-файл (разделитель «;», кодировка UTF-8).
     * Пробел в начале добавляется, чтобы Excel корректно открывал
     * кириллицу (BOM-маркер).
     */
    public int exportCsv(Path path, List<Book> books) throws IOException {
        StringBuilder csv = new StringBuilder();
        csv.append("Название;Авторы;Раздел;Издательство;Год;ISBN;Происхождение;Наличие;Статус чтения;Оценка;Экземпляров\n");
        for (Book book : books) {
            StringJoiner row = new StringJoiner(";");
            row.add(nvl(book.getTitle()));
            row.add(nvl(joinAuthorNames(book)));
            row.add(nvl(book.getCategory() == null ? null : book.getCategory().getName()));
            row.add(nvl(book.getPublisher()));
            row.add(nvl(book.getPublicationYear()));
            row.add(nvl(book.getIsbn()));
            row.add(nvl(book.getOrigin() == null ? null : book.getOrigin().getLabel()));
            row.add(nvl(book.getAvailability() == null ? null : book.getAvailability().getLabel()));
            row.add(nvl(book.getReadingStatus() == null ? null : book.getReadingStatus().getLabel()));
            row.add(book.getRating() == null ? "" : String.valueOf(book.getRating()));
            row.add(String.valueOf(book.getCopies().size()));
            csv.append(row).append('\n');
        }
        // BOM для корректного отображения русских символов в Excel.
        Files.write(path, ("\uFEFF" + csv).getBytes(StandardCharsets.UTF_8));
        return books.size();
    }

    /** Объединяет имена авторов книги в одну строку с разделителем «, ». */
    public String joinAuthorNames(Book book) {
        return book.getAuthors().stream()
                .map(Author::getFullName)
                .collect(Collectors.joining(", "));
    }

    // ==================== ВСПОМОГАТЕЛЬНОЕ ====================

    private void validateName(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " не может быть пустым.");
        }
    }

    private static String nvl(String value) {
        return value == null ? "" : value;
    }
}