package chnu.edu.inno.model.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Стан запису на момент однієї з його версій.
 *
 * <p>Відповідає на питання «яким цей запис був тоді», тоді як {@link AuditChangeResponse} — на
 * питання «що саме змінилося».
 */
@Schema(description = "Стан запису на певній версії")
public record AuditSnapshotResponse(
        @Schema(description = "Ідентифікатор запису") String itemId,
        @Schema(description = "Номер версії", example = "2") long version,
        @Schema(description = "Тип зміни: INITIAL, UPDATE або TERMINAL") String changeType,
        @Schema(description = "Хто змінив") String author,
        @Schema(description = "Коли, UTC") Instant changedAt,
        @Schema(description = "Назва на цій версії") String name,
        @Schema(description = "Код на цій версії") String code,
        @Schema(description = "Кількість на цій версії") Integer quantity) {
}
