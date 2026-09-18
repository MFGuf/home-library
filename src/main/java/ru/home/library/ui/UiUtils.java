package ru.home.library.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.stage.Window;
import ru.home.library.model.Author;
import ru.home.library.model.Book;
import ru.home.library.model.Category;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Вспомогательный класс интерфейса: общие приёмы работы с окнами.
 * <p>
 * Вынесены типовые операции — диалоги ошибок/подтверждения и открытие
 * форм добавления/редактирования — чтобы контроллеры не дублировали
 * один и тот же код загрузки FXML.
 */
public final class UiUtils {

    private UiUtils() {
    }

    /** Путь к таблице стилей, который подключается и к главному окну, и к диалогам. */
    private static final String CSS_URL = "/css/style.css";

    /** Показывает окно с сообщением об ошибке. */
    public static void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /** Показывает информационное окно. */
    public static void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /** Показывает окно подтверждения; возвращает {@code true} при согласии пользователя. */
    public static boolean showConfirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait().filter(button -> button == ButtonType.OK).isPresent();
    }

    /**
     * Универсальный метод открытия диалога с формой.
     * <p>
     * Загружает указанный FXML, вызывает {@code configure} для настройки
     * контроллера (например, для предзаполнения полей при редактировании),
     * а затем показывает модальный диалог. При нажатии «Сохранить» вначале
     * выполняется {@code validate()}: если форма заполнена некорректно,
     * диалог не закрывается и показывается сообщение об ошибке.
     *
     * @param <C>       тип контроллера формы
     * @param <T>       тип сущности, которую собирает форма
     * @param title     заголовок диалога
     * @param fxml      путь к FXML-файлу (от корня classpath, вида /fxml/...)
     * @param owner     родительское окно или {@code null}
     * @param configure настройка контроллера перед показом
     * @return собранную сущность либо {@code Optional.empty()}, если пользователь отменил
     */
    private static <C extends FormController<T>, T> Optional<T> showForm(
            String title, String fxml, Window owner, Consumer<C> configure) {
        try {
            FXMLLoader loader = new FXMLLoader(UiUtils.class.getResource(fxml));
            Node content = loader.load();
            C controller = loader.getController();
            configure.accept(controller);

            Dialog<T> dialog = new Dialog<>();
            dialog.setTitle(title);
            dialog.initOwner(owner);
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getStylesheets().add(CSS_URL);

            ButtonType saveType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

            Node saveButton = dialog.getDialogPane().lookupButton(saveType);
            saveButton.addEventFilter(ActionEvent.ACTION, event -> {
                String error = controller.validate();
                if (error != null) {
                    showError(error);
                    event.consume();
                }
            });

            dialog.setResultConverter(button -> button == saveType ? controller.collect() : null);
            return dialog.showAndWait();
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось загрузить форму: " + fxml, e);
        }
    }

    /** Диалог добавления/редактирования книги. */
    public static Optional<Book> showBookDialog(Window owner, Book book,
                                                List<Category> categories, List<Author> allAuthors) {
        return showForm(book == null ? "Новая книга" : "Редактирование книги",
                "/fxml/book-form.fxml", owner,
                (Consumer<BookFormController>) c -> c.init(book, categories, allAuthors));
    }

    /** Диалог добавления/редактирования автора. */
    public static Optional<Author> showAuthorDialog(Window owner, Author author) {
        return showForm(author == null ? "Новый автор" : "Редактирование автора",
                "/fxml/author-form.fxml", owner,
                (Consumer<AuthorFormController>) c -> c.init(author));
    }

    /** Диалог добавления/редактирования категории. */
    public static Optional<Category> showCategoryDialog(Window owner, Category category) {
        return showForm(category == null ? "Новый раздел" : "Редактирование раздела",
                "/fxml/category-form.fxml", owner,
                (Consumer<CategoryFormController>) c -> c.init(category));
    }
}