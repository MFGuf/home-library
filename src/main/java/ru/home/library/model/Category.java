package ru.home.library.model;

/**
 * Раздел библиотеки (категория литературы):
 * беллетристика, специальная литература, хобби, домашнее хозяйство и т.п.
 */
public class Category extends Entity {

    /** Название раздела (обязательное поле, уникальное). */
    private String name;

    /** Необязательное описание раздела. */
    private String description;

    public Category() {
    }

    public Category(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return name == null ? "" : name;
    }
}