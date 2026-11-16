package chnu.edu.inno.model.dto.request;

import chnu.edu.inno.model.Item;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Тіло запиту на створення запису.
 *
 * <p>Окремий тип від {@link Item} тут не формальність. В оригіналі створення приймало сам
 * документ, тобто клієнт міг надіслати {@code id} — і якби цей {@code id} уже існував,
 * {@code save} мовчки перезаписав би чужий запис замість створити новий. Тут поля {@code id}
 * просто немає, тож така відповідь неможлива за побудовою.
 */
@Schema(description = "Дані для створення запису")
public record ItemCreateRequest(
        @NotBlank(message = "назва обов'язкова")
        @Size(max = 100, message = "назва не довша за 100 символів")
        @Schema(description = "Назва", example = "Milk", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,

        @NotBlank(message = "код обов'язковий")
        @Pattern(regexp = "\\d{4,10}", message = "код — від 4 до 10 цифр")
        @Schema(description = "Код, від 4 до 10 цифр", example = "000001",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String code,

        @Min(value = 0, message = "кількість не може бути від'ємною")
        @Schema(description = "Кількість", example = "3")
        int quantity) {

    public Item toItem() {
        return Item.builder().name(name).code(code).quantity(quantity).build();
    }
}
