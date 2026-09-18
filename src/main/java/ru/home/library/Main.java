package ru.home.library;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ru.home.library.database.Database;
import ru.home.library.database.DbInitializer;

/**
 * Точка входа в приложение.
 * <p>
 * Наследование от {@link Application} и метод {@code start(Stage)}
 * являются требованиями JavaFX: графическая платформа сама создаёт
 * главное окно (stage) и передаёт его приложению.
 * <p>
 * Порядок запуска:
 * <ol>
 *     <li>открывается / создаётся база данных;</li>
 *     <li>создаётся схема и при необходимости наполняется демоданными;</li>
 *     <li>загружается главное окно из FXML-разметки;</li>
 *     <li>окно показывается пользователю.</li>
 * </ol>
 */
public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // 1. База данных.
        Database database = Database.getInstance();
        DbInitializer.init(database);

        // 2. Главное окно из FXML.
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-view.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1180, 720);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

        // 3. Конфигурация и показ окна.
        stage.setTitle("Картотека домашней библиотеки");
        stage.setMinWidth(1000);
        stage.setMinHeight(640);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}