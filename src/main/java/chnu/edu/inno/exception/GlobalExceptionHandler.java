package chnu.edu.inno.exception;

import chnu.edu.inno.model.dto.response.ApiResponse;
import chnu.edu.inno.model.dto.response.BaseMetaData;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Перетворює винятки на ту саму оболонку, що й успішні відповіді.
 *
 * <p>Це друга половина роботи: мало привести успішні відповіді до спільного вигляду, якщо на
 * помилці клієнт усе одно отримає щось зовсім інше. В оригіналі помилок не обробляв ніхто, тож
 * на некоректний запит Spring віддавав свою стандартну сторінку з полями {@code timestamp},
 * {@code path} і {@code trace} — тобто формат, якого не було в жодній успішній відповіді.
 *
 * <p>Текст винятку назовні віддається лише там, де його склав цей застосунок. Повідомлення від
 * драйвера бази чи від Spring можуть містити назви колекцій, індексів і фрагменти запитів —
 * такі подробиці йдуть у лог, а клієнт отримує загальний текст.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ItemNotFoundException.class)
    public ResponseEntity<ApiResponse<BaseMetaData, Void>> handleNotFound(ItemNotFoundException e) {
        return respond(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(DuplicateItemCodeException.class)
    public ResponseEntity<ApiResponse<BaseMetaData, Void>> handleDuplicate(
            DuplicateItemCodeException e) {
        return respond(HttpStatus.CONFLICT, e.getMessage());
    }

    /**
     * Унікальний індекс спрацював попри перевірку в сервісі.
     *
     * <p>Так буває, коли два запити з однаковим кодом ідуть одночасно: обидва бачать, що коду
     * немає, і обидва пишуть. Другий отримує помилку від бази, і її треба показати клієнтові так
     * само, як і передбачений конфлікт.
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ApiResponse<BaseMetaData, Void>> handleDuplicateKey(
            DuplicateKeyException e) {
        return respond(HttpStatus.CONFLICT, "Запис із таким кодом уже існує");
    }

    /** Тіло запиту не пройшло перевірку анотаціями. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<BaseMetaData, Void>> handleValidation(
            MethodArgumentNotValidException e) {

        String details =
                e.getBindingResult().getFieldErrors().stream()
                        .map(error -> error.getField() + ": " + error.getDefaultMessage())
                        .sorted()
                        .collect(Collectors.joining("; "));
        return respond(HttpStatus.BAD_REQUEST, details.isEmpty() ? "Некоректний запит" : details);
    }

    /** Параметр запиту не пройшов перевірку, наприклад від'ємний номер сторінки. */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<BaseMetaData, Void>> handleParameterValidation(
            HandlerMethodValidationException e) {

        String details =
                e.getParameterValidationResults().stream()
                        .flatMap(
                                result ->
                                        result.getResolvableErrors().stream()
                                                .map(
                                                        error ->
                                                                result.getMethodParameter().getParameterName()
                                                                        + ": "
                                                                        + error.getDefaultMessage()))
                        .sorted()
                        .collect(Collectors.joining("; "));
        return respond(
                HttpStatus.BAD_REQUEST, details.isEmpty() ? "Некоректні параметри запиту" : details);
    }

    /** Тіло запиту — не той JSON, який очікували. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<BaseMetaData, Void>> handleUnreadable(
            HttpMessageNotReadableException e) {
        return respond(HttpStatus.BAD_REQUEST, "Тіло запиту не вдалося розібрати");
    }

    /** Параметр не того типу: {@code ?page=abc}. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<BaseMetaData, Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException e) {
        return respond(HttpStatus.BAD_REQUEST, "Параметр " + e.getName() + " має неправильний тип");
    }

    /** Такого шляху немає. Без цього 404 на невідомий URL прийшов би в чужому форматі. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<BaseMetaData, Void>> handleNoResource(
            NoResourceFoundException e) {
        return respond(HttpStatus.NOT_FOUND, "Такого ресурсу немає");
    }

    /**
     * Усе інше.
     *
     * <p>Текст назовні загальний: подробиці несподіваного винятку можуть містити внутрішні
     * деталі застосунку, тож їм місце в логу, а не у відповіді.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<BaseMetaData, Void>> handleUnexpected(Exception e) {
        log.error("Необроблений виняток", e);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Внутрішня помилка сервера");
    }

    private ResponseEntity<ApiResponse<BaseMetaData, Void>> respond(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiResponse.error(status.value(), message));
    }
}
