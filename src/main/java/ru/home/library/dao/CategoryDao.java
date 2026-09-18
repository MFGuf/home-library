package ru.home.library.dao;

import ru.home.library.database.Database;
import ru.home.library.model.Category;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO для сущности «Категория» (раздел библиотеки).
 */
public class CategoryDao implements CrudDao<Category> {

    private final Database database;

    public CategoryDao(Database database) {
        this.database = database;
    }

    @Override
    public List<Category> findAll() {
        String sql = "SELECT id, name, description FROM categories ORDER BY name COLLATE NOCASE";
        List<Category> categories = new ArrayList<>();
        try (PreparedStatement ps = database.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                categories.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DaoException("Ошибка чтения списка категорий", e);
        }
        return categories;
    }

    @Override
    public Optional<Category> findById(long id) {
        String sql = "SELECT id, name, description FROM categories WHERE id = ?";
        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DaoException("Ошибка поиска категории id=" + id, e);
        }
    }

    public Optional<Category> findByName(String name) {
        String sql = "SELECT id, name, description FROM categories WHERE name = ? COLLATE NOCASE";
        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setString(1, name.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DaoException("Ошибка поиска категории по имени: " + name, e);
        }
    }

    @Override
    public Category save(Category category) {
        String name = category.getName() == null ? null : category.getName().trim();
        try {
            if (category.isNew()) {
                String sql = "INSERT INTO categories (name, description) VALUES (?, ?)";
                try (PreparedStatement ps = database.getConnection()
                        .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, name);
                    ps.setString(2, emptyToNull(category.getDescription()));
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            category.setId(keys.getLong(1));
                        }
                    }
                }
            } else {
                String sql = "UPDATE categories SET name = ?, description = ? WHERE id = ?";
                try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
                    ps.setString(1, name);
                    ps.setString(2, emptyToNull(category.getDescription()));
                    ps.setLong(3, category.getId());
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Ошибка сохранения категории: " + name, e);
        }
        category.setName(name);
        return category;
    }

    @Override
    public void delete(long id) {
        try (PreparedStatement ps = database.getConnection()
                .prepareStatement("DELETE FROM categories WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Ошибка удаления категории id=" + id, e);
        }
    }

    /** Сколько книг отнесено к категории (используется при удалении). */
    public long countBooksOf(long categoryId) {
        String sql = "SELECT COUNT(*) FROM books WHERE category_id = ?";
        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setLong(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        } catch (SQLException e) {
            throw new DaoException("Ошибка подсчёта книг категории id=" + categoryId, e);
        }
    }

    private Category mapRow(ResultSet rs) throws SQLException {
        Category category = new Category();
        category.setId(rs.getLong("id"));
        category.setName(rs.getString("name"));
        category.setDescription(rs.getString("description"));
        return category;
    }

    private String emptyToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}