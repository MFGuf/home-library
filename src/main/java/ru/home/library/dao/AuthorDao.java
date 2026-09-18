package ru.home.library.dao;

import ru.home.library.database.Database;
import ru.home.library.model.Author;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO для сущности «Автор»: поиск, сохранение и удаление записей
 * таблицы {@code authors}.
 */
public class AuthorDao implements CrudDao<Author> {

    private final Database database;

    public AuthorDao(Database database) {
        this.database = database;
    }

    @Override
    public List<Author> findAll() {
        String sql = "SELECT id, full_name, birth_year FROM authors ORDER BY full_name COLLATE NOCASE";
        List<Author> authors = new ArrayList<>();
        try (PreparedStatement ps = database.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                authors.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DaoException("Ошибка чтения списка авторов", e);
        }
        return authors;
    }

    @Override
    public Optional<Author> findById(long id) {
        String sql = "SELECT id, full_name, birth_year FROM authors WHERE id = ?";
        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DaoException("Ошибка поиска автора по id=" + id, e);
        }
    }

    /**
     * Поиск автора по точному имени. Учитывается для предотвращения
     * дубликатов авторов (имя в таблице уникально).
     */
    public Optional<Author> findByFullName(String fullName) {
        String sql = "SELECT id, full_name, birth_year FROM authors WHERE full_name = ? COLLATE NOCASE";
        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setString(1, fullName.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DaoException("Ошибка поиска автора по имени: " + fullName, e);
        }
    }

    @Override
    public Author save(Author author) {
        String name = author.getFullName() == null ? null : author.getFullName().trim();
        try {
            if (author.isNew()) {
                String sql = "INSERT INTO authors (full_name, birth_year) VALUES (?, ?)";
                try (PreparedStatement ps = database.getConnection()
                        .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, name);
                    ps.setString(2, emptyToNull(author.getBirthYear()));
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            author.setId(keys.getLong(1));
                        }
                    }
                }
            } else {
                String sql = "UPDATE authors SET full_name = ?, birth_year = ? WHERE id = ?";
                try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
                    ps.setString(1, name);
                    ps.setString(2, emptyToNull(author.getBirthYear()));
                    ps.setLong(3, author.getId());
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Ошибка сохранения автора: " + name, e);
        }
        author.setFullName(name);
        return author;
    }

    @Override
    public void delete(long id) {
        try (PreparedStatement ps = database.getConnection()
                .prepareStatement("DELETE FROM authors WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Ошибка удаления автора id=" + id, e);
        }
    }

    /** Сколько книг связано с автором (используется при удалении автора). */
    public long countBooksOf(long authorId) {
        String sql = "SELECT COUNT(*) FROM book_authors WHERE author_id = ?";
        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setLong(1, authorId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        } catch (SQLException e) {
            throw new DaoException("Ошибка подсчёта книг автора id=" + authorId, e);
        }
    }

    /** Преобразует строку результата запроса в объект {@link Author}. */
    private Author mapRow(ResultSet rs) throws SQLException {
        Author author = new Author();
        author.setId(rs.getLong("id"));
        author.setFullName(rs.getString("full_name"));
        author.setBirthYear(rs.getString("birth_year"));
        return author;
    }

    private String emptyToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}