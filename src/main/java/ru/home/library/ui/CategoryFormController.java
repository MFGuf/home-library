package ru.home.library.ui;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import ru.home.library.model.Category;

/**
 * Контроллер формы добавления/редактирования раздела библиотеки (категории).
 */
public class CategoryFormController implements FormController<Category> {

    @FXML
    private TextField nameField;
    @FXML
    private TextArea descriptionField;

    private Category original;

    /** Предзаполняет поля формы при редактировании существующей категории. */
    public void init(Category category) {
        this.original = category;
        if (category != null) {
            nameField.setText(category.getName());
            descriptionField.setText(category.getDescription());
        }
    }

    @Override
    public String validate() {
        if (nameField.getText() == null || nameField.getText().isBlank()) {
            return "Укажите название раздела.";
        }
        return null;
    }

    @Override
    public Category collect() {
        Category category = original == null ? new Category() : original;
        category.setName(nameField.getText().trim());
        category.setDescription(descriptionField.getText().trim());
        return category;
    }
}