package chnu.edu.inno.exception;

/** Код уже зайнятий іншим записом. Перетворюється на 409. */
public class DuplicateItemCodeException extends RuntimeException {

    public DuplicateItemCodeException(String code) {
        super("Запис із кодом " + code + " уже існує");
    }
}
