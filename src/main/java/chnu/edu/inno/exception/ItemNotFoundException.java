package chnu.edu.inno.exception;

/** Запису з таким ідентифікатором немає. Перетворюється на 404. */
public class ItemNotFoundException extends RuntimeException {

    public ItemNotFoundException(String id) {
        super("Запису з id " + id + " не знайдено");
    }
}
