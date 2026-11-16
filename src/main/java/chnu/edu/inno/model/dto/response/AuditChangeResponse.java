package chnu.edu.inno.model.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Одна зміна одного поля.
 *
 * <p>Взірець лекції віддавав назовні {@code javers.getJsonConverter().toJson(changes)} —
 * внутрішню структуру Javers із полями {@code globalId}, {@code commitMetadata},
 * {@code propertyChangeType} і службовими типами. Це по-перше чужий формат, який ніяк не
 * пов'язаний з оболонкою решти API, а по-друге контракт, що зміниться разом із версією
 * бібліотеки.
 *
 * <p>Тут назовні йде те, що справді потрібно: що, коли, ким і з чого на що.
 */
@Schema(description = "Зміна одного поля запису")
public record AuditChangeResponse(
        @Schema(description = "Ідентифікатор запису") String itemId,
        @Schema(description = "Назва поля", example = "quantity") String property,
        @Schema(description = "Значення до зміни", example = "1") String oldValue,
        @Schema(description = "Значення після зміни", example = "7") String newValue,
        @Schema(description = "Хто змінив") String author,
        @Schema(description = "Коли змінено, UTC") Instant changedAt,
        @Schema(description = "Номер коміту аудиту, спільний для всіх змін однієї правки",
                example = "11") long commitId) {
}
