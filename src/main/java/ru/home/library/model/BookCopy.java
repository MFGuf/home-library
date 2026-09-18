package ru.home.library.model;

/**
 * Физический экземпляр книги.
 * <p>
 * Одна и та же книга может существовать в нескольких экземплярах.
 * Экземпляр связан с книгой и описывается инвентарным номером
 * (условным обозначением), состоянием и годом поступления —
 * эти сведения нужны для инвентаризации библиотеки.
 */
public class BookCopy extends Entity {

    /** Идентификатор книги, к которой относится экземпляр. */
    private long bookId;

    /** Инвентарный номер или условное обозначение экземпляра. */
    private String inventoryLabel;

    /** Состояние экземпляра, например «Отличное», «Хорошее», «Потрёпанное». */
    private String condition;

    /** Год поступления экземпляра в библиотеку. */
    private String yearAcquired;

    public long getBookId() {
        return bookId;
    }

    public void setBookId(long bookId) {
        this.bookId = bookId;
    }

    public String getInventoryLabel() {
        return inventoryLabel;
    }

    public void setInventoryLabel(String inventoryLabel) {
        this.inventoryLabel = inventoryLabel;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getYearAcquired() {
        return yearAcquired;
    }

    public void setYearAcquired(String yearAcquired) {
        this.yearAcquired = yearAcquired;
    }

    @Override
    public String toString() {
        return inventoryLabel == null ? "" : inventoryLabel;
    }
}