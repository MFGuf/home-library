package ru.home.library.database;

import ru.home.library.model.Author;
import ru.home.library.model.Availability;
import ru.home.library.model.Book;
import ru.home.library.model.BookCopy;
import ru.home.library.model.Category;
import ru.home.library.model.Origin;
import ru.home.library.model.ReadingStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Инициализация базы данных: создание схемы и заполнение демонстрационными
 * данными при первом запуске (чтобы приложение сразу было «живым»).
 */
public final class DbInitializer {

    private DbInitializer() {
    }

    /** SQL-скрипт, описывающий схему базы данных. */
    private static final String[] SCHEMA = {
            "CREATE TABLE IF NOT EXISTS categories ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "name TEXT NOT NULL UNIQUE,"
                    + "description TEXT"
                    + ")",
            "CREATE TABLE IF NOT EXISTS authors ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "full_name TEXT NOT NULL UNIQUE,"
                    + "birth_year TEXT"
                    + ")",
            "CREATE TABLE IF NOT EXISTS books ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "title TEXT NOT NULL,"
                    + "publisher TEXT,"
                    + "publication_year TEXT,"
                    + "isbn TEXT,"
                    + "category_id INTEGER,"
                    + "origin TEXT,"
                    + "availability TEXT,"
                    + "reading_status TEXT,"
                    + "rating INTEGER,"
                    + "FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE SET NULL"
                    + ")",
            "CREATE TABLE IF NOT EXISTS book_authors ("
                    + "book_id INTEGER NOT NULL,"
                    + "author_id INTEGER NOT NULL,"
                    + "PRIMARY KEY (book_id, author_id),"
                    + "FOREIGN KEY (book_id) REFERENCES books (id) ON DELETE CASCADE,"
                    + "FOREIGN KEY (author_id) REFERENCES authors (id) ON DELETE CASCADE"
                    + ")",
            "CREATE TABLE IF NOT EXISTS book_copies ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "book_id INTEGER NOT NULL,"
                    + "inventory_label TEXT,"
                    + "condition TEXT,"
                    + "year_acquired TEXT,"
                    + "FOREIGN KEY (book_id) REFERENCES books (id) ON DELETE CASCADE"
                    + ")",
            "CREATE INDEX IF NOT EXISTS idx_books_category ON books (category_id)",
            "CREATE INDEX IF NOT EXISTS idx_copies_book ON book_copies (book_id)"
    };

    /** Создаёт таблицы (вызывается при каждом запуске приложения). */
    public static void createSchema(Database database) {
        // ВАЖНО: закрывать можно только Statement, но не само общее подключение
        // database.getConnection() — оно используется всем приложением.
        for (String statement : SCHEMA) {
            try (Statement st = database.getConnection().createStatement()) {
                st.execute(statement);
            } catch (SQLException e) {
                throw new IllegalStateException("Ошибка создания схемы базы данных", e);
            }
        }
    }

    /**
     * Полная инициализация: создание схемы и наполнение демонстрационными данными,
     * если база данных пуста.
     */
    public static void init(Database database) {
        createSchema(database);
        if (countRows(database, "books") == 0) {
            seedDemoData(database);
        }
    }

    /** Счётчик строк в таблице (для проверки пустоты БД). */
    private static long countRows(Database database, String table) {
        try (Statement st = database.getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Ошибка подсчёта строк в таблице " + table, e);
        }
    }

    /**
     * Демонстрационные данные. Категории добавляются только в том случае,
     * если их ещё нет (проверка по имени).
     */
    private static void seedDemoData(Database database) {
        Category fiction = createCategory(database, "Беллетристика",
                "Художественная литература");
        Category professional = createCategory(database, "Специальная литература",
                "Учебная и профессиональная литература");
        createCategory(database, "Домашнее хозяйство", "Кулинария, ремонт, дом");
        createCategory(database, "Хобби", "Книги об увлечениях и досуге");

        Author rowling = createAuthor(database, "Джоан Роулинг", "1965");
        Author tolstoy = createAuthor(database, "Лев Толстой", "1828");
        Author eckel = createAuthor(database, "Брюс Эккель", "1961");

        Book potter = createBook(database, "Гарри Поттер и философский камень",
                "Росмэн", "2002", "5-353-00820-8", fiction,
                Origin.PURCHASED, Availability.AVAILABLE, ReadingStatus.READ, 9);
        addAuthor(database, potter, rowling);
        addCopy(database, potter, "Б-001", "Отличное", "2002");
        addCopy(database, potter, "Б-002", "Хорошее", "2012");

        Book javaBook = createBook(database, "Философия Java",
                "Питер", "2013", "978-5-496-00566-7", professional,
                Origin.PURCHASED, Availability.AVAILABLE, ReadingStatus.READING, 8);
        addAuthor(database, javaBook, eckel);
        addCopy(database, javaBook, "Б-003", "Отличное", "2013");

        Book karenina = createBook(database, "Анна Каренина",
                "Эксмо", "2010", "978-5-699-44837-0", fiction,
                Origin.INHERITED, Availability.LOANED_OUT, ReadingStatus.NOT_READ, null);
        addAuthor(database, karenina, tolstoy);
        addCopy(database, karenina, "Б-004", "Потрёпанная", "2010");

        Book cooking = createBook(database, "Кулинария для дома",
                "АСТ", "2015", "978-5-17-091234-5", null,
                Origin.GIFT, Availability.AVAILABLE, ReadingStatus.READ, 7);
        addCopy(database, cooking, "Б-005", "Хорошее", "2015");
    }

    private static Category createCategory(Database database, String name, String description) {
        String sql = "SELECT id, name, description FROM categories WHERE name = ? COLLATE NOCASE";
        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Category existing = new Category();
                    existing.setId(rs.getLong("id"));
                    existing.setName(rs.getString("name"));
                    existing.setDescription(rs.getString("description"));
                    return existing;
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Ошибка поиска категории " + name, e);
        }
        Category category = new Category(name);
        category.setDescription(description);
        insertCategory(database, category);
        return category;
    }

    private static void insertCategory(Database database, Category category) {
        String sql = "INSERT INTO categories (name, description) VALUES (?, ?)";
        try (PreparedStatement ps = database.getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, category.getName());
            ps.setString(2, category.getDescription());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    category.setId(keys.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Ошибка вставки категории " + category.getName(), e);
        }
    }

    private static Author createAuthor(Database database, String name, String birthYear) {
        String sql = "SELECT id FROM authors WHERE full_name = ? COLLATE NOCASE";
        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Author existing = new Author();
                    existing.setId(rs.getLong("id"));
                    existing.setFullName(name);
                    return existing;
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Ошибка поиска автора " + name, e);
        }
        Author author = new Author(name);
        author.setBirthYear(birthYear);
        try (PreparedStatement ps = database.getConnection()
                .prepareStatement("INSERT INTO authors (full_name, birth_year) VALUES (?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, birthYear);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    author.setId(keys.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Ошибка вставки автора " + name, e);
        }
        return author;
    }

    private static Book createBook(Database database, String title, String publisher,
                                   String year, String isbn, Category category,
                                   Origin origin, Availability availability,
                                   ReadingStatus status, Integer rating) {
        Book book = new Book();
        book.setTitle(title);
        book.setPublisher(publisher);
        book.setPublicationYear(year);
        book.setIsbn(isbn);
        book.setCategory(category);
        book.setOrigin(origin);
        book.setAvailability(availability);
        book.setReadingStatus(status);
        book.setRating(rating);
        String sql = "INSERT INTO books (title, publisher, publication_year, isbn, category_id, "
                   + "origin, availability, reading_status, rating) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = database.getConnection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, title);
            ps.setString(2, publisher);
            ps.setString(3, year);
            ps.setString(4, isbn);
            ps.setObject(5, category == null ? null : category.getId());
            ps.setString(6, origin.getLabel());
            ps.setString(7, availability.getLabel());
            ps.setString(8, status.getLabel());
            ps.setObject(9, rating);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    book.setId(keys.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Ошибка вставки книги " + title, e);
        }
        return book;
    }

    private static void addAuthor(Database database, Book book, Author author) {
        String sql = "INSERT INTO book_authors (book_id, author_id) VALUES (?, ?)";
        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setLong(1, book.getId());
            ps.setLong(2, author.getId());
            ps.executeUpdate();
            book.getAuthors().add(author);
        } catch (SQLException e) {
            throw new IllegalStateException("Ошибка связывания книги и автора", e);
        }
    }

    private static void addCopy(Database database, Book book, String label,
                                String condition, String year) {
        String sql = "INSERT INTO book_copies (book_id, inventory_label, condition, year_acquired) "
                   + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setLong(1, book.getId());
            ps.setString(2, label);
            ps.setString(3, condition);
            ps.setString(4, year);
            ps.executeUpdate();
            BookCopy copy = new BookCopy();
            copy.setBookId(book.getId());
            copy.setInventoryLabel(label);
            copy.setCondition(condition);
            copy.setYearAcquired(year);
            book.getCopies().add(copy);
        } catch (SQLException e) {
            throw new IllegalStateException("Ошибка вставки экземпляра книги", e);
        }
    }
}