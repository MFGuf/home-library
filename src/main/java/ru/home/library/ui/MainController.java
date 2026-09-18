package ru.home.library.ui;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import ru.home.library.database.Database;
import ru.home.library.model.Author;
import ru.home.library.model.Availability;
import ru.home.library.model.Book;
import ru.home.library.model.BookCopy;
import ru.home.library.model.Category;
import ru.home.library.model.ReadingStatus;
import ru.home.library.service.LibraryService;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Контроллер главного окна приложения (паттерн MVC, роль Controller).
 * <p>
 * В одном главном окне организованы все разделы приложения:
 * «Книги», «Авторы», «Разделы» и «Инвентаризация» (вкладки TabPane).
 * Контроллер отвечает за отображение данных в таблицах, обработку
 * действий пользователя (добавление, изменение, удаление, поиск,
 * фильтрация, инвентаризация) и делегирование бизнес-логики сервису
 * {@link LibraryService}.
 */
public class MainController implements Initializable {

    // ---- Поле поиска/фильтры на вкладке «Книги» ----
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> categoryFilter;
    @FXML
    private ComboBox<String> statusFilter;
    @FXML
    private ComboBox<String> availabilityFilter;
    @FXML
    private ComboBox<String> ratingFilter;
    @FXML
    private Label resultInfoLabel;

    // ---- Таблица книг ----
    @FXML
    private TableView<Book> bookTable;
    @FXML
    private TableColumn<Book, String> bookTitleCol;
    @FXML
    private TableColumn<Book, String> bookAuthorsCol;
    @FXML
    private TableColumn<Book, String> bookCategoryCol;
    @FXML
    private TableColumn<Book, String> bookPublisherCol;
    @FXML
    private TableColumn<Book, String> bookYearCol;
    @FXML
    private TableColumn<Book, String> bookStatusCol;
    @FXML
    private TableColumn<Book, String> bookAvailabilityCol;
    @FXML
    private TableColumn<Book, Integer> bookRatingCol;
    @FXML
    private TableColumn<Book, Integer> bookCopiesCol;

    // ---- Панель подробных сведений о книге ----
    @FXML
    private Label detailTitle;
    @FXML
    private Label detailAuthors;
    @FXML
    private Label detailPublisherYear;
    @FXML
    private Label detailIsbn;
    @FXML
    private Label detailCategory;
    @FXML
    private Label detailOrigin;
    @FXML
    private Label detailAvailability;
    @FXML
    private Label detailStatus;
    @FXML
    private Label detailRating;
    @FXML
    private Label detailCopies;

    // ---- Вкладка «Авторы» ----
    @FXML
    private TableView<Author> authorTable;
    @FXML
    private TableColumn<Author, String> authorNameCol;
    @FXML
    private TableColumn<Author, String> authorYearCol;
    @FXML
    private TableColumn<Author, Integer> authorBooksCol;
    @FXML
    private Label authorInfoLabel;

    // ---- Вкладка «Разделы» ----
    @FXML
    private TableView<Category> categoryTable;
    @FXML
    private TableColumn<Category, String> categoryNameCol;
    @FXML
    private TableColumn<Category, String> categoryDescriptionCol;
    @FXML
    private TableColumn<Category, Integer> categoryBooksCol;
    @FXML
    private Label categoryInfoLabel;

    // ---- Вкладка «Инвентаризация» ----
    @FXML
    private Label inventorySummaryLabel;
    @FXML
    private TableView<Book> inventoryTable;
    @FXML
    private TableColumn<Book, Integer> invNumberCol;
    @FXML
    private TableColumn<Book, String> invTitleCol;
    @FXML
    private TableColumn<Book, String> invAuthorsCol;
    @FXML
    private TableColumn<Book, String> invCategoryCol;
    @FXML
    private TableColumn<Book, String> invOriginCol;
    @FXML
    private TableColumn<Book, Integer> invCopiesCol;
    @FXML
    private TableColumn<Book, String> invAvailabilityCol;
    @FXML
    private TableColumn<Book, String> invRatingCol;
    @FXML
    private TableView<BookCopy> inventoryCopyTable;
    @FXML
    private TableColumn<BookCopy, String> invCopyLabelCol;
    @FXML
    private TableColumn<BookCopy, String> invCopyConditionCol;
    @FXML
    private TableColumn<BookCopy, String> invCopyYearCol;

    @FXML
    private Label dbInfoLabel;

    // ---- Значения по умолчанию для фильтров ----
    private static final String ALL_CATEGORIES = "Все разделы";
    private static final String ANY_STATUS = "Любой";
    private static final String ANY_AVAILABILITY = "Любое";
    private static final String ANY_RATING = "Любая";

    /** Сервисный слой — единственная точка соприкосновения интерфейса с данными. */
    private LibraryService service;

    /** Количество книг автора (для колонки таблицы авторов). */
    private final Map<Long, Integer> authorBookCounts = new HashMap<>();
    /** Количество книг категории (для колонки таблицы разделов). */
    private final Map<Long, Integer> categoryBookCounts = new HashMap<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        service = new LibraryService(Database.getInstance());
        configureColumns();
        initFilters();
        bindSelectionListeners();
        dbInfoLabel.setText("База данных: " + service.getDatabaseUrl());
        refreshAll();
    }

    // ==================== НАСТРОЙКА ТАБЛИЦ ====================

    /**
     * Настраивает способ отображения значений в колонках таблиц.
     * Для каждой колонки задаётся «фабрика значений ячеек» — функция,
     * которая берёт поле из объекта строки.
     */
    private void configureColumns() {
        // Книги
        bookTitleCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getTitle()));
        bookAuthorsCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(service.joinAuthorNames(cd.getValue())));
        bookCategoryCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
                display(cd.getValue().getCategory() == null ? null : cd.getValue().getCategory().getName())));
        bookPublisherCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(display(cd.getValue().getPublisher())));
        bookYearCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(display(cd.getValue().getPublicationYear())));
        bookStatusCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(display(cd.getValue().getReadingStatus())));
        bookAvailabilityCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(display(cd.getValue().getAvailability())));
        bookRatingCol.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().getRating()));
        bookCopiesCol.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().getCopies().size()));
        center(bookRatingCol, bookCopiesCol, bookYearCol);

        // Авторы
        authorNameCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getFullName()));
        authorYearCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(display(cd.getValue().getBirthYear())));
        authorBooksCol.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(
                authorBookCounts.getOrDefault(cd.getValue().getId(), 0)));

        // Разделы
        categoryNameCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getName()));
        categoryDescriptionCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(display(cd.getValue().getDescription())));
        categoryBooksCol.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(
                categoryBookCounts.getOrDefault(cd.getValue().getId(), 0)));

        // Инвентаризация
        invNumberCol.setCellValueFactory(cd ->
                new ReadOnlyObjectWrapper<>(cd.getTableView().getItems().indexOf(cd.getValue()) + 1));
        invTitleCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getTitle()));
        invAuthorsCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(service.joinAuthorNames(cd.getValue())));
        invCategoryCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
                display(cd.getValue().getCategory() == null ? null : cd.getValue().getCategory().getName())));
        invOriginCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(display(cd.getValue().getOrigin())));
        invCopiesCol.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().getCopies().size()));
        invAvailabilityCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(display(cd.getValue().getAvailability())));
        invRatingCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(displayRating(cd.getValue().getRating())));
        center(invNumberCol, invCopiesCol, invRatingCol);

        // Экземпляры (вкладка инвентаризации)
        invCopyLabelCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(display(cd.getValue().getInventoryLabel())));
        invCopyConditionCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(display(cd.getValue().getCondition())));
        invCopyYearCol.setCellValueFactory(cd -> new ReadOnlyStringWrapper(display(cd.getValue().getYearAcquired())));
    }

    /** Инициализация выпадающих списков фильтров. */
    private void initFilters() {
        statusFilter.getItems().setAll(ANY_STATUS,
                ReadingStatus.NOT_READ.getLabel(),
                ReadingStatus.READING.getLabel(),
                ReadingStatus.READ.getLabel(),
                ReadingStatus.REREAD.getLabel());
        statusFilter.setValue(ANY_STATUS);

        availabilityFilter.getItems().setAll(ANY_AVAILABILITY,
                Availability.AVAILABLE.getLabel(),
                Availability.LOANED_OUT.getLabel(),
                Availability.LOST.getLabel());
        availabilityFilter.setValue(ANY_AVAILABILITY);

        List<String> ratings = new ArrayList<>();
        ratings.add(ANY_RATING);
        for (int i = 1; i <= 10; i++) {
            ratings.add(String.valueOf(i));
        }
        ratingFilter.getItems().setAll(ratings);
        ratingFilter.setValue(ANY_RATING);

        refreshCategoryFilter();
    }

    /** Перестраивает список разделов в фильтре (с сохранением текущего выбора). */
    private void refreshCategoryFilter() {
        String current = categoryFilter.getValue();
        List<String> names = new ArrayList<>();
        names.add(ALL_CATEGORIES);
        for (Category category : service.getAllCategories()) {
            names.add(category.getName());
        }
        categoryFilter.getItems().setAll(names);
        categoryFilter.setValue(names.contains(current) ? current : ALL_CATEGORIES);
    }

    /** Подписка на события выбора строк в таблицах. */
    private void bindSelectionListeners() {
        // Изменение выбора книги -> обновление панели подробностей.
        bookTable.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> showBookDetails(newValue));

        // Изменение выбора позиции в инвентаризации -> показ её экземпляров.
        inventoryTable.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> showInventoryCopies(newValue));

        // Автоматический поиск при изменении любого фильтра.
        categoryFilter.valueProperty().addListener((observable, oldValue, newValue) -> refreshBookTable());
        statusFilter.valueProperty().addListener((observable, oldValue, newValue) -> refreshBookTable());
        availabilityFilter.valueProperty().addListener((observable, oldValue, newValue) -> refreshBookTable());
        ratingFilter.valueProperty().addListener((observable, oldValue, newValue) -> refreshBookTable());

        // Двойной клик по строке открывает форму редактирования.
        installDoubleClickToEdit(bookTable);
        installDoubleClickToEdit(authorTable);
        installDoubleClickToEdit(categoryTable);
    }

    /** Двойной клик по строке таблицы открывает диалог редактирования. */
    private <T> void installDoubleClickToEdit(TableView<T> table) {
        table.setRowFactory(tv -> {
            TableRow<T> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    if (table == bookTable) {
                        onEditBook();
                    } else if (table == authorTable) {
                        onEditAuthor();
                    } else {
                        onEditCategory();
                    }
                }
            });
            return row;
        });
    }

    // ==================== ОБНОВЛЕНИЕ ДАННЫХ ====================

    /** Полное обновление всех вкладок после любого изменения данных. */
    private void refreshAll() {
        refreshBookTable();
        refreshAuthorTable();
        refreshCategoryTable();
        refreshInventory();
    }

    /** Поиск книг по текущему запросу и фильтрам и обновление таблицы. */
    private void refreshBookTable() {
        List<Book> books = service.searchBooks(
                searchField.getText(),
                selectedCategory(),
                selectedReadingStatus(),
                selectedAvailability(),
                selectedMinRating());
        bookTable.setItems(FXCollections.observableArrayList(books));
        resultInfoLabel.setText("Найдено книг: " + books.size());
        showBookDetails(bookTable.getSelectionModel().getSelectedItem());
    }

    private void refreshAuthorTable() {
        authorBookCounts.clear();
        List<Author> authors = service.getAllAuthors();
        for (Author author : authors) {
            authorBookCounts.put(author.getId(), (int) service.countBooksOf(author));
        }
        authorTable.setItems(FXCollections.observableArrayList(authors));
        authorInfoLabel.setText("Авторов в картотеке: " + authors.size());
    }

    private void refreshCategoryTable() {
        categoryBookCounts.clear();
        List<Category> categories = service.getAllCategories();
        for (Category category : categories) {
            categoryBookCounts.put(category.getId(), (int) service.countBooksOf(category));
        }
        categoryTable.setItems(FXCollections.observableArrayList(categories));
        categoryInfoLabel.setText("Разделов в картотеке: " + categories.size());
        refreshCategoryFilter();
    }

    private void refreshInventory() {
        List<Book> books = service.getAllBooks();
        inventoryTable.setItems(FXCollections.observableArrayList(books));
        inventorySummaryLabel.setText(service.buildInventorySummary(books));
        showInventoryCopies(inventoryTable.getSelectionModel().getSelectedItem());
    }

    // ==================== ПРЕОБРАЗОВАНИЕ ФИЛЬТРОВ ====================

    private Category selectedCategory() {
        String value = categoryFilter.getValue();
        if (value == null || ALL_CATEGORIES.equals(value)) {
            return null;
        }
        return service.getAllCategories().stream()
                .filter(c -> value.equals(c.getName()))
                .findFirst().orElse(null);
    }

    private ReadingStatus selectedReadingStatus() {
        String value = statusFilter.getValue();
        return value == null || ANY_STATUS.equals(value) ? null : ReadingStatus.fromLabel(value);
    }

    private Availability selectedAvailability() {
        String value = availabilityFilter.getValue();
        return value == null || ANY_AVAILABILITY.equals(value) ? null : Availability.fromLabel(value);
    }

    private Integer selectedMinRating() {
        String value = ratingFilter.getValue();
        if (value == null || ANY_RATING.equals(value)) {
            return null;
        }
        return Integer.valueOf(value);
    }

    // ==================== ДЕЙСТВИЯ: КНИГИ ====================

    @FXML
    private void onSearch() {
        refreshBookTable();
    }

    @FXML
    private void onResetFilters() {
        searchField.clear();
        categoryFilter.setValue(ALL_CATEGORIES);
        statusFilter.setValue(ANY_STATUS);
        availabilityFilter.setValue(ANY_AVAILABILITY);
        ratingFilter.setValue(ANY_RATING);
        refreshBookTable();
    }

    @FXML
    private void onAddBook() {
        openBookDialog(null);
    }

    @FXML
    private void onEditBook() {
        Book selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            UiUtils.showError("Выберите книгу в таблице.");
            return;
        }
        openBookDialog(selected);
    }

    @FXML
    private void onDeleteBook() {
        Book selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            UiUtils.showError("Выберите книгу в таблице.");
            return;
        }
        if (UiUtils.showConfirm("Удаление книги",
                "Удалить книгу «" + selected.getTitle() + "» вместе с её экземплярами?")) {
            service.deleteBook(selected);
            refreshAll();
        }
    }

    /** Открывает диалог добавления/редактирования книги и сохраняет результат. */
    private void openBookDialog(Book book) {
        UiUtils.showBookDialog(currentWindow(), book,
                        service.getAllCategories(), service.getAllAuthors())
                .ifPresent(edited -> {
                    service.saveBook(edited);
                    refreshAll();
                });
    }

    // ==================== ДЕЙСТВИЯ: АВТОРЫ ====================

    @FXML
    private void onAddAuthor() {
        UiUtils.showAuthorDialog(currentWindow(), null)
                .ifPresent(author -> {
                    try {
                        service.saveAuthor(author);
                        refreshAuthorTable();
                        refreshBookTable();
                    } catch (IllegalArgumentException e) {
                        UiUtils.showError(e.getMessage());
                    }
                });
    }

    @FXML
    private void onEditAuthor() {
        Author selected = authorTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            UiUtils.showError("Выберите автора в таблице.");
            return;
        }
        UiUtils.showAuthorDialog(currentWindow(), selected)
                .ifPresent(author -> {
                    service.saveAuthor(author);
                    refreshAll();
                });
    }

    @FXML
    private void onDeleteAuthor() {
        Author selected = authorTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            UiUtils.showError("Выберите автора в таблице.");
            return;
        }
        if (UiUtils.showConfirm("Удаление автора",
                "Удалить автора «" + selected.getFullName() + "»?")) {
            try {
                service.deleteAuthor(selected);
                refreshAll();
            } catch (IllegalStateException e) {
                UiUtils.showError(e.getMessage());
            }
        }
    }

    // ==================== ДЕЙСТВИЯ: КАТЕГОРИИ ====================

    @FXML
    private void onAddCategory() {
        UiUtils.showCategoryDialog(currentWindow(), null)
                .ifPresent(category -> {
                    try {
                        service.saveCategory(category);
                        refreshCategoryTable();
                        refreshBookTable();
                    } catch (IllegalArgumentException e) {
                        UiUtils.showError(e.getMessage());
                    }
                });
    }

    @FXML
    private void onEditCategory() {
        Category selected = categoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            UiUtils.showError("Выберите раздел в таблице.");
            return;
        }
        UiUtils.showCategoryDialog(currentWindow(), selected)
                .ifPresent(category -> {
                    service.saveCategory(category);
                    refreshAll();
                });
    }

    @FXML
    private void onDeleteCategory() {
        Category selected = categoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            UiUtils.showError("Выберите раздел в таблице.");
            return;
        }
        if (UiUtils.showConfirm("Удаление раздела",
                "Удалить раздел «" + selected.getName() + "»?")) {
            try {
                service.deleteCategory(selected);
                refreshAll();
            } catch (IllegalStateException e) {
                UiUtils.showError(e.getMessage());
            }
        }
    }

    // ==================== ДЕЙСТВИЯ: ИНВЕНТАРИЗАЦИЯ ====================

    @FXML
    private void onRefreshInventory() {
        refreshInventory();
    }

    /** Экспорт инвентаризационного списка в CSV-файл через системный диалог. */
    @FXML
    private void onExportCsv() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Экспорт картотеки в CSV");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV-файл", "*.csv"));
        chooser.setInitialFileName("library_inventory.csv");
        File file = chooser.showSaveDialog(currentWindow());
        if (file == null) {
            return;
        }
        try {
            int exported = service.exportCsv(file.toPath(), inventoryTable.getItems());
            UiUtils.showInfo("Экспорт завершён",
                    "Экспортировано книг: " + exported + "\nФайл: " + file.getAbsolutePath());
        } catch (IOException e) {
            UiUtils.showError("Не удалось сохранить файл: " + e.getMessage());
        }
    }

    // ==================== ОТОБРАЖЕНИЕ ПОДРОБНОСТЕЙ ====================

    /** Заполняет панель сведений о выбранной в картотеке книге. */
    private void showBookDetails(Book book) {
        if (book == null) {
            detailTitle.setText("—");
            detailAuthors.setText("—");
            detailPublisherYear.setText("—");
            detailIsbn.setText("—");
            detailCategory.setText("—");
            detailOrigin.setText("—");
            detailAvailability.setText("—");
            detailStatus.setText("—");
            detailRating.setText("—");
            detailCopies.setText("—");
            return;
        }
        detailTitle.setText(book.getTitle());
        detailAuthors.setText(service.joinAuthorNames(book));
        String publisher = display(book.getPublisher());
        String year = display(book.getPublicationYear());
        detailPublisherYear.setText("—".equals(publisher) ? year : publisher + (year.equals("—") ? "" : ", " + year));
        detailIsbn.setText(display(book.getIsbn()));
        detailCategory.setText(display(book.getCategory() == null ? null : book.getCategory().getName()));
        detailOrigin.setText(display(book.getOrigin()));
        detailAvailability.setText(display(book.getAvailability()));
        detailStatus.setText(display(book.getReadingStatus()));
        detailRating.setText(displayRating(book.getRating()));
        detailCopies.setText(String.valueOf(book.getCopies().size()));
    }

    /** Заполняет таблицу экземпляров выбранной позиции инвентаризации. */
    private void showInventoryCopies(Book book) {
        ObservableList<BookCopy> copies = FXCollections.observableArrayList();
        if (book != null) {
            copies.setAll(book.getCopies());
        }
        inventoryCopyTable.setItems(copies);
    }

    // ==================== ВСПОМОГАТЕЛЬНОЕ ====================

    private Window currentWindow() {
        return bookTable.getScene().getWindow();
    }

    /** Преобразует null в «—» для наглядного отображения пустых значений. */
    private static String display(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    /** Отображение перечислений: null -> «—», иначе текстовая метка. */
    private static String display(Object enumValue) {
        return enumValue == null ? "—" : enumValue.toString();
    }

    /** Отображение оценки: null -> «—», иначе число. */
    private static String displayRating(Integer rating) {
        return rating == null ? "—" : String.valueOf(rating);
    }

    /** Выравнивание колонок по центру (для чисел и коротких значений). */
    @SafeVarargs
    private static void center(TableColumn<?, ?>... columns) {
        for (TableColumn<?, ?> column : columns) {
            column.setStyle("-fx-alignment: CENTER;");
        }
    }
}