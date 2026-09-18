package ru.home.library.database;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Управление подключением к базе данных SQLite.
 * <p>
 * Класс реализован в виде одиночки ({@code singleton}): всё приложение
 * работает с одним подключением к одному файлу БД. Это допустимо для
 * настольного приложения и упрощает транзакции.
 * <p>
 * База данных по умолчанию хранится в файле
 * {@code <домашний каталог пользователя>/.home-library/library.db}.
 * Расположение можно переопределить системным свойством
 * {@code home.library.db.dir}.
 */
public final class Database {

    /** Единственный экземпляр (для обычного запуска приложения). */
    private static volatile Database instance;

    /** JDBC-адрес подключения. */
    private final String url;

    /** Одно общее подключение на всё приложение. */
    private final Connection connection;

    private Database(String url) {
        this.url = url;
        this.connection = openConnection();
    }

    /**
     * Создаёт экземпляр для файловой базы данных по умолчанию.
     * При необходимости создаёт каталог для файла БД.
     */
    private static Database createDefault() {
        String baseDir = System.getProperty(
                "home.library.db.dir",
                Paths.get(System.getProperty("user.home"), ".home-library").toString());
        Path dir = Paths.get(baseDir);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось создать каталог для базы данных: " + dir, e);
        }
        String file = dir.resolve("library.db").toString();
        return new Database("jdbc:sqlite:" + file);
    }

    /**
     * Возвращает экземпляр базы данных по умолчанию.
     * Используется контроллерами графического интерфейса.
     */
    public static synchronized Database getInstance() {
        if (instance == null) {
            instance = createDefault();
        }
        return instance;
    }

    /**
     * Создаёт базу данных, расположенную в оперативной памяти.
     * Применяется в юнит-тестах, чтобы не затрагивать файл БД пользователя.
     */
    public static Database inMemory() {
        return new Database("jdbc:sqlite::memory:");
    }

    /** Открывает подключение и включает проверку внешних ключей. */
    private Connection openConnection() {
        try {
            Connection connection = DriverManager.getConnection(url);
            try (Statement statement = connection.createStatement()) {
                // Без этого SQLite не выполняет ON DELETE CASCADE.
                statement.execute("PRAGMA foreign_keys = ON");
            }
            return connection;
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось подключиться к базе данных: " + url, e);
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public String getUrl() {
        return url;
    }
}