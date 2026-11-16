package chnu.edu.inno.model.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Службова частина відповіді: чи вдався запит і що пішло не так.
 *
 * <p>Клієнт читає {@code success} і не мусить розбирати тіло, щоб зрозуміти результат. Код
 * дублює статус HTTP навмисно: відповідь часто логують або кладуть у чергу окремо від
 * транспорту, і тоді статус із заголовка втрачається.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Schema(description = "Службові дані відповіді")
public class BaseMetaData {

    @Builder.Default
    @Schema(description = "Код HTTP, що дублює статус відповіді", example = "200")
    private int code = 200;

    @Builder.Default
    @Schema(description = "Чи виконано запит успішно", example = "true")
    private boolean success = true;

    @Builder.Default
    @Schema(description = "Повідомлення про помилку; null, якщо помилки не було")
    private String errorMessage = null;

    public BaseMetaData(int code, boolean success) {
        this.code = code;
        this.success = success;
    }
}
