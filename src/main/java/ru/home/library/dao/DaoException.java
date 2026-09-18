package ru.home.library.dao;

/**
 * Исключение слоя доступа к данным.
 * <p>
 * Обёртка над {@link java.sql.SQLException} в виде непроверяемого исключения:
 * позволяет не загромождать код верхних слоёв блоками {@code try/catch}
 * при каждой операции с базой данных.
 */
public class DaoException extends RuntimeException {

    public DaoException(String message, Throwable cause) {
        super(message, cause);
    }

    public DaoException(Throwable cause) {
        super(cause);
    }
}