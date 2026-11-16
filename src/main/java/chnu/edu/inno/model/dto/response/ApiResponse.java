package chnu.edu.inno.model.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.domain.Page;

/**
 * Спільна оболонка всіх відповідей API: службова частина плюс дані.
 *
 * <p>Сенс у тому, що клієнт розбирає всі відповіді однаково. В оригіналі частина методів
 * повертала {@code Item}, частина {@code List<Item>}, видалення — взагалі нічого, а помилка
 * приходила як стектрейс Spring у зовсім іншому форматі. Чотири різні форми відповіді на один
 * ресурс означають чотири гілки розбору на клієнті.
 *
 * <p>{@code data} — завжди список, навіть коли запис один. Це теж навмисно: інакше тип поля
 * залежав би від методу, і клієнт мусив би дивитися то на об'єкт, то на масив.
 *
 * @param <M> тип службової частини
 * @param <D> тип корисних даних
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Schema(description = "Оболонка відповіді: службова частина та дані")
public class ApiResponse<M extends BaseMetaData, D> {

    @Schema(description = "Службова частина відповіді")
    private M meta;

    @Schema(description = "Корисні дані; завжди список, навіть якщо запис один")
    private List<D> data;

    public ApiResponse(M meta, D data) {
        this.meta = meta;
        this.data = new ArrayList<>();
        this.data.add(data);
    }

    public ApiResponse(M meta) {
        this.meta = meta;
    }

    /** Успішна відповідь з одним записом. */
    public static <D> ApiResponse<BaseMetaData, D> ok(D item) {
        return new ApiResponse<>(new BaseMetaData(200, true), item);
    }

    /** Успішна відповідь зі списком записів. */
    public static <D> ApiResponse<BaseMetaData, D> ok(List<D> items) {
        return new ApiResponse<>(new BaseMetaData(200, true), items);
    }

    /** Відповідь на створення запису. */
    public static <D> ApiResponse<BaseMetaData, D> created(D item) {
        return new ApiResponse<>(new BaseMetaData(201, true), item);
    }

    /** Відповідь без корисних даних: видалення пройшло успішно. */
    public static ApiResponse<BaseMetaData, Void> noContent() {
        return new ApiResponse<>(new BaseMetaData(204, true), List.of());
    }

    /** Невдала відповідь: код, прапорець і текст помилки. */
    public static <D> ApiResponse<BaseMetaData, D> error(int code, String message) {
        return new ApiResponse<>(new BaseMetaData(code, false, message), List.of());
    }

    /** Сторінка записів разом із даними про посторінкову навігацію. */
    public static <D> ApiResponse<PaginationMetaData, D> page(Page<D> page) {
        return new ApiResponse<>(PaginationMetaData.of(page), page.getContent());
    }
}
