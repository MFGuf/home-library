package ru.home.library.dao;

import ru.home.library.database.Database;
import ru.home.library.model.BookCopy;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO для сущности «Экземпляр книги».
 * Экземпляры хранятся отдельно от книги и используются при инвентаризации.
 */
public class BookCopyDao {

    private final Database database;

    public BookCopyDao(Database database) {
        this.database = database;
    }

    /** Возвращает все экземпляры, принадлежащие указанной книге. */
    public List<BookCopy> findAllByBookId(long bookId) {
        String sql = "SELECT id, book_id, inventory_label, condition, year_acquired "
                   + "FROM book_copies WHERE book_id = ? ORDER BY id";
        List<BookCopy> copies = new ArrayList<>();
        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setLong(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    copies.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Ошибка чтения экземпляров книги id=" + bookId, e);
        }
        return copies;
    }

    /** Возвращает все экземпляры всех книг (для инвентаризации). */
    public List<BookCopy> findAll() {
        String sql = "SELECT id, book_id, inventory_label, condition, year_acquired "
                   + "FROM book_copies ORDER BY book_id, id";
        List<BookCopy> copies = new ArrayList<>();
        try (PreparedStatement ps = database.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                copies.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DaoException("Ошибка чтения экземпляров книг", e);
        }
        return copies;
    }

    /** Вставляет или обновляет экземпляр. */
    public BookCopy save(BookCopy copy) {
        try {
            if (copy.isNew()) {
                String sql = "INSERT INTO book_copies (book_id, inventory_label, condition, year_acquired) "
                           + "VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = database.getConnection()
                        .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    bindFields(ps, copy);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            copy.setId(keys.getLong(1));
                        }
                    }
                }
            } else {
                String sql = "UPDATE book_copies SET book_id = ?, inventory_label = ?, condition = ?, "
                           + "year_acquired = ? WHERE id = ?";
                try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
                    bindFields(ps, copy);
                    ps.setLong(5, copy.getId());
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Ошибка сохранения экземпляра книги", e);
        }
        return copy;
    }

    /** Удаляет экземпляр по идентификатору. */
    public void delete(long id) {
        try (PreparedStatement ps = database.getConnection()
                .prepareStatement("DELETE FROM book_copies WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Ошибка удаления экземпляра id=" + id, e);
        }
    }

    /** Удаляет все экземпляры книги (используется при пересохранении). */
    public void deleteByBookId(long bookId) {
        try (PreparedStatement ps = database.getConnection()
                .prepareStatement("DELETE FROM book_copies WHERE book_id = ?")) {
            ps.setLong(1, bookId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Ошибка удаления экземпляров книги id=" + bookId, e);
        }
    }

    private BookCopy mapRow(ResultSet rs) throws SQLException {
        BookCopy copy = new BookCopy();
        copy.setId(rs.getLong("id"));
        copy.setBookId(rs.getLong("book_id"));
        copy.setInventoryLabel(rs.getString("inventory_label"));
        copy.setCondition(rs.getString("condition"));
        copy.setYearAcquired(rs.getString("year_acquired"));
        return copy;
    }

    private void bindFields(PreparedStatement ps, BookCopy copy) throws SQLException {
        ps.setLong(1, copy.getBookId());
        ps.setString(2, nullIfBlank(copy.getInventoryLabel()));
        ps.setString(3, nullIfBlank(copy.getCondition()));
        ps.setString(4, nullIfBlank(copy.getYearAcquired()));
    }

    private String nullIfBlank(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}