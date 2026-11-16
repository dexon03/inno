package chnu.edu.inno.model.dto.response;

import chnu.edu.inno.model.Item;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Запис у тому вигляді, в якому його віддає API.
 *
 * <p>Окремий тип від {@link Item} потрібен, щоб форма відповіді не змінювалася щоразу, коли
 * змінюється документ у базі. В оригіналі назовні віддавався сам документ, тож будь-яке нове
 * поле — навіть службове — одразу потрапляло б у відповідь, а перейменування поля в базі
 * ламало б клієнтів.
 */
@Schema(description = "Запис складу")
public record ItemResponse(
        @Schema(description = "Ідентифікатор", example = "66f1c0a8e13b2c0f9c8d4e21") String id,
        @Schema(description = "Назва", example = "Milk") String name,
        @Schema(description = "Код", example = "000001") String code,
        @Schema(description = "Кількість", example = "3") int quantity) {

    public static ItemResponse from(Item item) {
        return new ItemResponse(item.getId(), item.getName(), item.getCode(), item.getQuantity());
    }
}
