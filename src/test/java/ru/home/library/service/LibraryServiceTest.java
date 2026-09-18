package ru.home.library.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.home.library.database.Database;
import ru.home.library.database.DbInitializer;
import ru.home.library.model.Author;
import ru.home.library.model.Availability;
import ru.home.library.model.Book;
import ru.home.library.model.BookCopy;
import ru.home.library.model.Category;
import ru.home.library.model.Origin;
import ru.home.library.model.ReadingStatus;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Юнит-тесты сервисного слоя и DAO.
 * Тесты используют базу данных в оперативной памяти, поэтому
 * не затрагивают файл БД, с которым работает само приложение.
 */
class LibraryServiceTest {

    private Database database;
    private LibraryService service;

    @BeforeEach
    void setUp() {
        database = Database.inMemory();
        DbInitializer.createSchema(database);
        service = new LibraryService(database);
    }

    /** Вспомогательный метод: создаёт книгу с автором и экземпляром. */
    private Book buildBook(String title, Author author, Category category) {
        Book book = new Book();
        book.setTitle(title);
        book.setPublisher("Питер");
        book.setPublicationYear("2020");
        book.setIsbn("978-5-00000-000-0");
        book.setCategory(category);
        book.setOrigin(Origin.PURCHASED);
        book.setAvailability(Availability.AVAILABLE);
        book.setReadingStatus(ReadingStatus.NOT_READ);
        book.setRating(8);
        book.getAuthors().add(author);

        BookCopy copy = new BookCopy();
        copy.setInventoryLabel("Б-100");
        copy.setCondition("Отличное");
        copy.setYearAcquired("2021");
        book.getCopies().add(copy);
        return book;
    }

    @Test
    void saveAndFindBookWithAuthorAndCopies() {
        Author author = service.saveAuthor(new Author("Тестовый Автор"));
        Category category = service.saveCategory(new Category("Учебная литература"));

        Book book = buildBook("Тестовая книга", author, category);
        service.saveBook(book);
        assertTrue(book.getId() > 0, "После сохранения книге должен быть присвоен id");

        List<Book> books = service.getAllBooks();
        assertEquals(1, books.size());

        Book loaded = books.get(0);
        assertEquals("Тестовая книга", loaded.getTitle());
        assertEquals(1, loaded.getAuthors().size());
        assertEquals("Тестовый Автор", loaded.getAuthors().get(0).getFullName());
        assertEquals(1, loaded.getCopies().size());
        assertEquals("Б-100", loaded.getCopies().get(0).getInventoryLabel());
        assertEquals(Origin.PURCHASED, loaded.getOrigin());
        assertEquals(8, loaded.getRating());
    }

    @Test
    void updateBookReplacesAuthorsAndCopies() {
        Author author1 = service.saveAuthor(new Author("Автор Первый"));
        Author author2 = service.saveAuthor(new Author("Автор Второй"));
        Category category = service.saveCategory(new Category("Хобби"));

        Book book = buildBook("Книга", author1, category);
        service.saveBook(book);

        // Изменяем авторов и экземпляры.
        book.getAuthors().clear();
        book.getAuthors().add(author2);
        book.getCopies().clear();
        BookCopy newCopy = new BookCopy();
        newCopy.setInventoryLabel("Б-200");
        book.getCopies().add(newCopy);
        service.saveBook(book);

        List<Book> books = service.getAllBooks();
        assertEquals(1, books.size());
        Book loaded = books.get(0);
        assertEquals(1, loaded.getAuthors().size());
        assertEquals("Автор Второй", loaded.getAuthors().get(0).getFullName());
        assertEquals(1, loaded.getCopies().size());
        assertEquals("Б-200", loaded.getCopies().get(0).getInventoryLabel());
    }

    @Test
    void deleteBookRemovesItsCopies() {
        Book book = buildBook("Книга для удаления",
                service.saveAuthor(new Author("Автор Три")), null);
        service.saveBook(book);
        assertEquals(1, service.getAllBooks().size());

        service.deleteBook(book);
        assertTrue(service.getAllBooks().isEmpty());

        // После удаления книги её экземпляры также должны исчезнуть.
        List<BookCopy> orphans = new ru.home.library.dao.BookCopyDao(database)
                .findAllByBookId(book.getId());
        assertTrue(orphans.isEmpty(), "Экземпляры удалённых книг не должны оставаться в БД");
    }

    @Test
    void searchFindsByTitleAuthorAndFilters() {
        Category fiction = service.saveCategory(new Category("Беллетристика"));
        Author author = service.saveAuthor(new Author("Лев Примеров"));
        Book book = buildBook("Приключения в лесу", author, fiction);
        book.setReadingStatus(ReadingStatus.READ);
        book.setAvailability(Availability.LOANED_OUT);
        service.saveBook(book);

        // Поиск по подстроке слова из названия.
        assertEquals(1, service.searchBooks("прикл", null, null, null, null).size());
        // Поиск по имени автора.
        assertEquals(1, service.searchBooks("Примеров", null, null, null, null).size());
        // Фильтр по разделу.
        assertEquals(1, service.searchBooks(null, fiction, null, null, null).size());
        // Фильтр по статусу чтения.
        assertEquals(1, service.searchBooks(null, null, ReadingStatus.READ, null, null).size());
        // Фильтр по наличию — совпадение с «выдана».
        assertEquals(1, service.searchBooks(null, null, null, Availability.LOANED_OUT, null).size());
        // Фильтр по минимальной оценке — нет книг с оценкой 10.
        assertTrue(service.searchBooks(null, null, null, null, 10).isEmpty());
        // Комбинированный фильтр с заведомо неверным разделом.
        assertTrue(service.searchBooks(null, new Category("Несуществующий раздел"), null, null, null)
                .isEmpty());
    }

    @Test
    void categoryInUseCannotBeDeleted() {
        Category category = service.saveCategory(new Category("Специальная литература"));
        Book book = buildBook("Книга в разделе", service.saveAuthor(new Author("Автор Четыре")), category);
        service.saveBook(book);

        assertThrows(IllegalStateException.class, () -> service.deleteCategory(category));
        assertEquals(1, service.getAllCategories().size());
    }

    @Test
    void duplicateAuthorAndCategoryNamesAreMerged() {
        Author first = service.saveAuthor(new Author("Дубликат Авторов"));
        Author second = service.saveAuthor(new Author("Дубликат Авторов"));
        assertEquals(first.getId(), second.getId(), "Автор с таким именем должен найтись, а не создаваться повторно");

        Category firstCat = service.saveCategory(new Category("Поэзия"));
        Category secondCat = service.saveCategory(new Category("Поэзия"));
        assertEquals(firstCat.getId(), secondCat.getId());
    }

    @Test
    void demoDataIsSeededOnInit() {
        Database seeded = Database.inMemory();
        DbInitializer.init(seeded);
        LibraryService seededService = new LibraryService(seeded);

        List<Book> books = seededService.getAllBooks();
        assertEquals(4, books.size(), "Демонстрационные данные должны быть добавлены при инициализации");
        assertTrue(seededService.getAllCategories().size() >= 4);
    }

    @Test
    void csvExportWritesFile() throws Exception {
        service.saveBook(buildBook("Книга для экспорта",
                service.saveAuthor(new Author("Автор Экспорт")), null));

        Path tempFile = Files.createTempFile("library", ".csv");
        int exported = service.exportCsv(tempFile, service.getAllBooks());

        assertEquals(1, exported);
        String content = Files.readString(tempFile);
        assertTrue(content.contains("Книга для экспорта"));
        assertTrue(content.contains("Автор Экспорт"));
        Files.deleteIfExists(tempFile);
    }

    @Test
    void validationRejectsEmptyTitle() {
        Book book = new Book();
        book.setTitle("   ");
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> service.saveBook(book));
        assertNotNull(exception.getMessage());
    }

    @Test
    void optionalFieldsMayBeNull() {
        Book book = new Book();
        book.setTitle("Книга без подробностей");
        service.saveBook(book);
        assertEquals(1, service.getAllBooks().size());
        Book loaded = service.getAllBooks().get(0);
        assertEquals(null, loaded.getOrigin());
    }
}