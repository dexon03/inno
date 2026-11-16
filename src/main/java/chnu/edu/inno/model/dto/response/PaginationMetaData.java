package chnu.edu.inno.model.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.domain.Page;

/**
 * Службова частина відповіді зі сторінкою даних.
 *
 * <p>Додає до {@link BaseMetaData} все, що потрібно клієнтові, щоб показати посторінкову
 * навігацію й не вгадувати, чи є наступна сторінка.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Schema(description = "Службові дані відповіді зі сторінкою")
public class PaginationMetaData extends BaseMetaData {

    @Schema(description = "Номер сторінки, від нуля", example = "0")
    private int number;

    @Schema(description = "Розмір сторінки", example = "5")
    private int size;

    @Schema(description = "Скільки записів усього", example = "8")
    private long totalElements;

    @Schema(description = "Скільки сторінок усього", example = "2")
    private int totalPages;

    @Schema(description = "Чи це перша сторінка")
    private boolean isFirst;

    @Schema(description = "Чи це остання сторінка")
    private boolean isLast;

    /**
     * Збирає метадані зі сторінки Spring Data.
     *
     * <p>Шість присвоєнь, які в оригіналі стояли в сервісі, зведені в одне місце: інакше їх
     * доводиться повторювати в кожному методі, що повертає сторінку.
     */
    public static PaginationMetaData of(Page<?> page) {
        return PaginationMetaData.builder()
                .number(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isFirst(page.isFirst())
                .isLast(page.isLast())
                .build();
    }
}
