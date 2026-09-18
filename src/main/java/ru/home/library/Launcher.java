package ru.home.library;

/**
 * Точка входа для запуска приложения «по classpath» (обычный запуск из IDE).
 * <p>
 * Зачем нужен этот класс? Если запускать напрямую класс, наследующий
 * {@link javafx.application.Application} (наш {@link Main}), без настройки
 * модульного пути JavaFX, JVM выводит ошибку:
 * {@code JavaFX runtime components are missing, and are required to run this application}.
 * <p>
 * Универсальное решение — главный класс, который НЕ наследует Application,
 * а лишь вызывает метод {@code main} приложения. Тогда JavaFX-платформа
 * загружается с обычного classpath, и приложение работает и в IDE
 * (IntelliJ IDEA и др.), и из командной строки, и через {@code mvn javafx:run}.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        Main.main(args);
    }
}