package ru.home.library.ui;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import ru.home.library.model.Author;
import ru.home.library.model.Availability;
import ru.home.library.model.Book;
import ru.home.library.model.BookCopy;
import ru.home.library.model.Category;
import ru.home.library.model.Origin;
import ru.home.library.model.ReadingStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Контроллер формы добавления/редактирования книги.
 * <p>
 * Форма объединяет все сведения о книге: выходные данные (название,
 * издательство, год, ISBN), раздел библиотеки, происхождение, наличие,
 * статус чтения и субъективную оценку, а также списки авторов (перенос
 * авторов между двумя списками) и физических экземпляров.
 */
public class BookFormController implements FormController<Book> {

    /** Маркер «значение не выбрано» для выпадающих списков с необязательными полями. */
    private static final String NOT_SET = "— не указано —";
    /** Маркер «книга не оценена» в списке оценок. */
    private static final String RATING_EMPTY = "—";

    @FXML
    private TextField titleField;
    @FXML
    private TextField publisherField;
    @FXML
    private TextField yearField;
    @FXML
    private TextField isbnField;
    @FXML
    private ComboBox<Object> categoryCombo;
    @FXML
    private ComboBox<String> originCombo;
    @FXML
    private ComboBox<String> availabilityCombo;
    @FXML
    private ComboBox<String> statusCombo;
    @FXML
    private ComboBox<String> ratingChoice;

    @FXML
    private TextField authorSearchField;
    @FXML
    private ListView<Author> allAuthorsList;
    @FXML
    private ListView<Author> selectedAuthorsList;

    @FXML
    private TextField copyLabelField;
    @FXML
    private TextField copyConditionField;
    @FXML
    private TextField copyYearField;
    @FXML
    private TableView<BookCopy> copyTable;
    @FXML
    private TableColumn<BookCopy, String> copyLabelCol;
    @FXML
    private TableColumn<BookCopy, String> copyConditionCol;
    @FXML
    private TableColumn<BookCopy, String> copyYearCol;

    /** Редактируемая книга (null для новой книги). */
    private Book original;

    /** Все авторы картотеки (не фильтрованные) — из них строится левый список. */
    private final List<Author> masterAuthors = new ArrayList<>();

    /** Первичная настройка формы: выборы списков и предзаполнение полей. */
    public void init(Book book, List<Category> categories, List<Author> allAuthors) {
        this.original = book;
        configureCopyTable();

        // Раздел библиотеки: первый пункт — «без раздела».
        List<Object> categoryItems = new ArrayList<>();
        categoryItems.add(NOT_SET);
        categoryItems.addAll(categories);
        categoryCombo.getItems().setAll(categoryItems);

        // Перечисления из предметной области для выпадающих списков.
        originCombo.getItems().setAll(NOT_SET,
                Origin.PURCHASED.getLabel(),
                Origin.GIFT.getLabel(),
                Origin.INHERITED.getLabel(),
                Origin.OTHER.getLabel());
        availabilityCombo.getItems().setAll(
                Availability.AVAILABLE.getLabel(),
                Availability.LOANED_OUT.getLabel(),
                Availability.LOST.getLabel());
        statusCombo.getItems().setAll(
                ReadingStatus.NOT_READ.getLabel(),
                ReadingStatus.READING.getLabel(),
                ReadingStatus.READ.getLabel(),
                ReadingStatus.REREAD.getLabel());

        List<String> ratings = new ArrayList<>();
        ratings.add(RATING_EMPTY);
        for (int i = 1; i <= 10; i++) {
            ratings.add(String.valueOf(i));
        }
        ratingChoice.getItems().setAll(ratings);

        // Списки авторов.
        masterAuthors.addAll(allAuthors);
        authorSearchField.textProperty().addListener((observable, oldValue, newValue) -> refreshAllAuthors());
        refreshAllAuthors();
        selectedAuthorsList.setItems(FXCollections.observableArrayList());

        if (book == null) {
            // Значения по умолчанию для новой книги.
            categoryCombo.setValue(NOT_SET);
            originCombo.setValue(NOT_SET);
            availabilityCombo.setValue(Availability.AVAILABLE.getLabel());
            statusCombo.setValue(ReadingStatus.NOT_READ.getLabel());
            ratingChoice.setValue(RATING_EMPTY);
            return;
        }

        // Предзаполнение формы данными редактируемой книги.
        titleField.setText(book.getTitle());
        publisherField.setText(book.getPublisher());
        yearField.setText(book.getPublicationYear());
        isbnField.setText(book.getIsbn());
        categoryCombo.setValue(pickCategory(book));
        originCombo.setValue(book.getOrigin() == null ? NOT_SET : book.getOrigin().getLabel());
        availabilityCombo.setValue(book.getAvailability() == null
                ? Availability.AVAILABLE.getLabel() : book.getAvailability().getLabel());
        statusCombo.setValue(book.getReadingStatus() == null
                ? ReadingStatus.NOT_READ.getLabel() : book.getReadingStatus().getLabel());
        ratingChoice.setValue(book.getRating() == null ? RATING_EMPTY : String.valueOf(book.getRating()));
        selectedAuthorsList.getItems().setAll(book.getAuthors());
        copyTable.getItems().setAll(book.getCopies());
    }

    /** Находит элемент в списке разделов, совпадающий с категорией книги по id. */
    private Object pickCategory(Book book) {
        if (book.getCategory() == null) {
            return NOT_SET;
        }
        return categoryCombo.getItems().stream()
                .filter(item -> item instanceof Category
                        && ((Category) item).getId() == book.getCategory().getId())
                .findFirst()
                .orElse(NOT_SET);
    }

    /** Настройка колонок таблицы экземпляров. */
    private void configureCopyTable() {
        copyLabelCol.setCellValueFactory(cd -> new javafx.beans.property.ReadOnlyStringWrapper(display(cd.getValue().getInventoryLabel())));
        copyConditionCol.setCellValueFactory(cd -> new javafx.beans.property.ReadOnlyStringWrapper(display(cd.getValue().getCondition())));
        copyYearCol.setCellValueFactory(cd -> new javafx.beans.property.ReadOnlyStringWrapper(display(cd.getValue().getYearAcquired())));
    }

    /** Обновляет левый список авторов с учётом текста фильтра. */
    private void refreshAllAuthors() {
        String query = authorSearchField.getText() == null ? "" : authorSearchField.getText().toLowerCase(Locale.ROOT);
        List<Author> shown = new ArrayList<>();
        for (Author author : masterAuthors) {
            if (query.isBlank() || author.getFullName().toLowerCase(Locale.ROOT).contains(query)) {
                shown.add(author);
            }
        }
        allAuthorsList.setItems(FXCollections.observableArrayList(shown));
    }

    // ==================== ОБРАБОТЧИКИ КНОПОК ====================

    /** Переносит выбранного автора из общего списка в список авторов книги. */
    @FXML
    private void onMoveAuthorToBook() {
        Author author = allAuthorsList.getSelectionModel().getSelectedItem();
        if (author != null && !selectedAuthorsList.getItems().contains(author)) {
            selectedAuthorsList.getItems().add(author);
        }
    }

    /** Возвращает выбранного автора из списка авторов книги в общий список. */
    @FXML
    private void onMoveAuthorBack() {
        Author author = selectedAuthorsList.getSelectionModel().getSelectedItem();
        if (author != null) {
            selectedAuthorsList.getItems().remove(author);
        }
    }

    /** Создаёт нового автора через отдельный диалог и сразу добавляет его к книге. */
    @FXML
    private void onCreateAuthor() {
        UiUtils.showAuthorDialog(copyTable.getScene().getWindow(), null)
                .ifPresent(author -> {
                    masterAuthors.add(author);
                    selectedAuthorsList.getItems().add(author);
                    refreshAllAuthors();
                });
    }

    /** Добавляет экземпляр книги по полям из панели копий. */
    @FXML
    private void onAddCopy() {
        if (copyLabelField.getText().isBlank() && copyConditionField.getText().isBlank()
                && copyYearField.getText().isBlank()) {
            UiUtils.showError("Заполните хотя бы один из параметров экземпляра (например, инвентарный номер).");
            return;
        }
        BookCopy copy = new BookCopy();
        copy.setInventoryLabel(copyLabelField.getText().trim());
        copy.setCondition(copyConditionField.getText().trim());
        copy.setYearAcquired(copyYearField.getText().trim());
        copyTable.getItems().add(copy);
        copyLabelField.clear();
        copyConditionField.clear();
        copyYearField.clear();
    }

    /** Удаляет выбранный экземпляр из списка. */
    @FXML
    private void onRemoveCopy() {
        BookCopy selected = copyTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            copyTable.getItems().remove(selected);
        }
    }

    // ==================== КОНТРАКТ FormController ====================

    @Override
    public String validate() {
        if (titleField.getText() == null || titleField.getText().isBlank()) {
            return "Укажите название книги.";
        }
        return null;
    }

    @Override
    public Book collect() {
        Book book = original == null ? new Book() : original;
        // Списки пересобираются заново из формы при каждом сохранении.
        book.getAuthors().clear();
        book.getCopies().clear();

        book.setTitle(titleField.getText().trim());
        book.setPublisher(trim(publisherField.getText()));
        book.setPublicationYear(trim(yearField.getText()));
        book.setIsbn(trim(isbnField.getText()));

        Object categorySelection = categoryCombo.getValue();
        book.setCategory(categorySelection instanceof Category ? (Category) categorySelection : null);

        String originLabel = originCombo.getValue();
        book.setOrigin(NOT_SET.equals(originLabel) ? null : Origin.fromLabel(originLabel));
        book.setAvailability(Availability.fromLabel(availabilityCombo.getValue()));
        book.setReadingStatus(ReadingStatus.fromLabel(statusCombo.getValue()));

        String rating = ratingChoice.getValue();
        book.setRating(RATING_EMPTY.equals(rating) ? null : Integer.valueOf(rating));

        book.getAuthors().addAll(selectedAuthorsList.getItems());
        book.getCopies().addAll(copyTable.getItems());
        return book;
    }

    private String display(String value) {
        return value == null ? "" : value;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}