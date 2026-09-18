package ru.home.library.dao;

import ru.home.library.database.Database;
import ru.home.library.model.Author;
import ru.home.library.model.Availability;
import ru.home.library.model.Book;
import ru.home.library.model.BookCopy;
import ru.home.library.model.Category;
import ru.home.library.model.Origin;
import ru.home.library.model.ReadingStatus;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO для сущности «Книга».
 * <p>
 * Основная сложность этого класса — работа со связями: книга связана
 * с категорией (один ко многим), с авторами (многие ко многим через
 * таблицу {@code book_authors}) и с экземплярами (один ко многим).
 * Поэтому после чтения основной записи дополнительно загружаются списки
 * авторов и экземпляров книги.
 */
public class BookDao implements CrudDao<Book> {

    private final Database database;

    public BookDao(Database database) {
        this.database = database;
    }

    /** Базовый запрос книги вместе с её категорией (LEFT JOIN). */
    private static final String BASE_SELECT =
            "SELECT b.id, b.title, b.publisher, b.publication_year, b.isbn, " +
            "b.category_id, b.origin, b.availability, b.reading_status, b.rating, " +
            "c.id AS c_id, c.name AS c_name, c.description AS c_description " +
            "FROM books b LEFT JOIN categories c ON c.id = b.category_id ";

    @Override
    public List<Book> findAll() {
        return search(null, null, null, null);
    }

    @Override
    public Optional<Book> findById(long id) {
        String sql = BASE_SELECT + " WHERE b.id = ?";
        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Book book = mapRow(rs);
                    loadAuthors(book);
                    loadCopies(book);
                    return Optional.of(book);
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Ошибка чтения книги id=" + id, e);
        }
        return Optional.empty();
    }

    /**
     * Поиск книг по структурным фильтрам.
     * <p>
     * Каждый из фильтров может быть {@code null}, тогда соответствующее
     * условие просто не добавляется в SQL. Свободный текстовый поиск
     * (по названию, авторам и т.п.) выполняется на уровне сервиса,
     * поскольку встроенные средства SQLite корректно сравнивают
     * без учёта регистра только латиницу.
     *
     * @param category     раздел библиотеки или {@code null}
     * @param status       статус чтения или {@code null}
     * @param availability наличие или {@code null}
     * @param minRating    нижняя граница оценки (1..10) или {@code null}
     * @return список найденных книг с загруженными авторами и экземплярами
     */
    public List<Book> search(Category category, ReadingStatus status,
                             Availability availability, Integer minRating) {
        // Запрос собирается динамически: список условий и параметров.
        StringBuilder sql = new StringBuilder(BASE_SELECT).append(" WHERE 1 = 1");
        List<Object> params = new ArrayList<>();

        if (category != null) {
            sql.append(" AND b.category_id = ?");
            params.add(category.getId());
        }
        if (status != null) {
            sql.append(" AND b.reading_status = ?");
            params.add(status.getLabel());
        }
        if (availability != null) {
            sql.append(" AND b.availability = ?");
            params.add(availability.getLabel());
        }
        if (minRating != null) {
            sql.append(" AND b.rating IS NOT NULL AND b.rating >= ?");
            params.add(minRating);
        }
        sql.append(" ORDER BY b.title COLLATE NOCASE");

        List<Book> books = new ArrayList<>();
        try (PreparedStatement ps = database.getConnection().prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Book book = mapRow(rs);
                    loadAuthors(book);
                    loadCopies(book);
                    books.add(book);
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Ошибка поиска книг", e);
        }
        return books;
    }

    @Override
    public Book save(Book book) {
        try {
            if (book.isNew()) {
                String sql = "INSERT INTO books (title, publisher, publication_year, isbn, category_id, "
                           + "origin, availability, reading_status, rating) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = database.getConnection()
                        .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    bindFields(ps, book);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            book.setId(keys.getLong(1));
                        }
                    }
                }
            } else {
                String sql = "UPDATE books SET title = ?, publisher = ?, publication_year = ?, isbn = ?, "
                           + "category_id = ?, origin = ?, availability = ?, reading_status = ?, rating = ? "
                           + "WHERE id = ?";
                try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
                    bindFields(ps, book);
                    ps.setLong(10, book.getId());
                    ps.executeUpdate();
                }
            }
            // Связи «книга–автор» всегда переписываются полностью,
            // поэтому отпадает необходимость их точечного сравнения.
            replaceAuthors(book);
        } catch (SQLException e) {
            throw new DaoException("Ошибка сохранения книги: " + book.getTitle(), e);
        }
        return book;
    }

    @Override
    public void delete(long id) {
        // При включённой проверке внешних ключей связные записи
        // (book_authors, book_copies) удаляются каскадно.
        try (PreparedStatement ps = database.getConnection()
                .prepareStatement("DELETE FROM books WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Ошибка удаления книги id=" + id, e);
        }
    }

    /** Количество книг в разделе (для контроля удаления категории). */
    public long countByCategory(long categoryId) {
        String sql = "SELECT COUNT(*) FROM books WHERE category_id = ?";
        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setLong(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        } catch (SQLException e) {
            throw new DaoException("Ошибка подсчёта книг категории", e);
        }
    }

    /**
     * Сохраняет авторов, связанных с книгой. Метод удаляет все старые
     * связи и вставляет новые — простая и надёжная стратегия замены.
     */
    private void replaceAuthors(Book book) throws SQLException {
        try (PreparedStatement clear = database.getConnection()
                .prepareStatement("DELETE FROM book_authors WHERE book_id = ?")) {
            clear.setLong(1, book.getId());
            clear.executeUpdate();
        }
        if (book.getAuthors().isEmpty()) {
            return;
        }
        try (PreparedStatement insert = database.getConnection()
                .prepareStatement("INSERT INTO book_authors (book_id, author_id) VALUES (?, ?)")) {
            for (Author author : book.getAuthors()) {
                insert.setLong(1, book.getId());
                insert.setLong(2, author.getId());
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    /** Загружает авторов книги (join по таблице book_authors). */
    private void loadAuthors(Book book) {
        String sql = "SELECT a.id, a.full_name, a.birth_year FROM book_authors ba "
                   + "JOIN authors a ON a.id = ba.author_id WHERE ba.book_id = ? "
                   + "ORDER BY a.full_name COLLATE NOCASE";
        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setLong(1, book.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Author author = new Author();
                    author.setId(rs.getLong("id"));
                    author.setFullName(rs.getString("full_name"));
                    author.setBirthYear(rs.getString("birth_year"));
                    book.getAuthors().add(author);
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Ошибка чтения авторов книги id=" + book.getId(), e);
        }
    }

    /** Загружает физические экземпляры книги. */
    private void loadCopies(Book book) {
        String sql = "SELECT id, book_id, inventory_label, condition, year_acquired "
                   + "FROM book_copies WHERE book_id = ?";
        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setLong(1, book.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BookCopy copy = new BookCopy();
                    copy.setId(rs.getLong("id"));
                    copy.setBookId(rs.getLong("book_id"));
                    copy.setInventoryLabel(rs.getString("inventory_label"));
                    copy.setCondition(rs.getString("condition"));
                    copy.setYearAcquired(rs.getString("year_acquired"));
                    book.getCopies().add(copy);
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Ошибка чтения экземпляров книги id=" + book.getId(), e);
        }
    }

    /** Заполняет параметры PreparedStatement из полей книги. */
    private void bindFields(PreparedStatement ps, Book book) throws SQLException {
        ps.setString(1, emptyToNull(book.getTitle()));
        ps.setString(2, emptyToNull(book.getPublisher()));
        ps.setString(3, emptyToNull(book.getPublicationYear()));
        ps.setString(4, emptyToNull(book.getIsbn()));
        ps.setObject(5, book.getCategory() == null ? null : book.getCategory().getId());
        ps.setString(6, book.getOrigin() == null ? null : book.getOrigin().getLabel());
        ps.setString(7, book.getAvailability() == null ? null : book.getAvailability().getLabel());
        ps.setString(8, book.getReadingStatus() == null ? null : book.getReadingStatus().getLabel());
        ps.setObject(9, book.getRating());
    }

    /** Преобразует строку результата в объект {@link Book}. */
    private Book mapRow(ResultSet rs) throws SQLException {
        Book book = new Book();
        book.setId(rs.getLong("id"));
        book.setTitle(rs.getString("title"));
        book.setPublisher(rs.getString("publisher"));
        book.setPublicationYear(rs.getString("publication_year"));
        book.setIsbn(rs.getString("isbn"));
        book.setOrigin(Origin.fromLabel(rs.getString("origin")));
        book.setAvailability(Availability.fromLabel(rs.getString("availability")));
        book.setReadingStatus(ReadingStatus.fromLabel(rs.getString("reading_status")));
        // Оценка может быть NULL («книга не оценена») — читаем её как объект,
        // чтобы отличить NULL от нуля.
        Object ratingObject = rs.getObject("rating");
        book.setRating(ratingObject == null ? null : ((Number) ratingObject).intValue());

        long categoryId = rs.getLong("c_id");
        if (!rs.wasNull()) {
            Category category = new Category();
            category.setId(categoryId);
            category.setName(rs.getString("c_name"));
            category.setDescription(rs.getString("c_description"));
            book.setCategory(category);
        }
        return book;
    }

    private String emptyToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}