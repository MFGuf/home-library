package ru.home.library.ui;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import ru.home.library.model.Author;

/**
 * Контроллер формы добавления/редактирования автора.
 * Содержит лишь два поля — имя и год рождения.
 */
public class AuthorFormController implements FormController<Author> {

    @FXML
    private TextField nameField;
    @FXML
    private TextField birthYearField;

    private Author original;

    /** Предзаполняет поля формы при редактировании существующего автора. */
    public void init(Author author) {
        this.original = author;
        if (author != null) {
            nameField.setText(author.getFullName());
            birthYearField.setText(author.getBirthYear());
        }
    }

    @Override
    public String validate() {
        if (nameField.getText() == null || nameField.getText().isBlank()) {
            return "Укажите имя автора.";
        }
        return null;
    }

    @Override
    public Author collect() {
        Author author = original == null ? new Author() : original;
        author.setFullName(nameField.getText().trim());
        author.setBirthYear(birthYearField.getText().trim());
        return author;
    }
}