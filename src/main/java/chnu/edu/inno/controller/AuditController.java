package chnu.edu.inno.controller;

import chnu.edu.inno.model.dto.response.ApiResponse;
import chnu.edu.inno.model.dto.response.AuditChangeResponse;
import chnu.edu.inno.model.dto.response.AuditSnapshotResponse;
import chnu.edu.inno.model.dto.response.BaseMetaData;
import chnu.edu.inno.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Історія змін записів складу.
 *
 * <p>Відповіді мають ту саму оболонку {@code meta} плюс {@code data}, що й решта API. У взірці
 * лекції контролер аудиту віддавав сирий JSON від Javers, тобто ще один формат на додачу до
 * тих, що вже були.
 *
 * <p>Виняток один — {@code /pretty}, і він навмисний: це текстовий звіт для людини, а не дані
 * для програми, тож і тип вмісту в нього звичайний текст.
 */
@RestController
@RequestMapping("api/v1/audit/items")
@RequiredArgsConstructor
@Tag(name = "Audit", description = "Історія змін записів")
public class AuditController {

    private static final int MAX_LIMIT = 500;

    private final AuditService auditService;

    @GetMapping
    @Operation(summary = "Зміни всіх записів", description = "Найновіші першими")
    public ResponseEntity<ApiResponse<BaseMetaData, AuditChangeResponse>> getAllChanges(
            @Parameter(description = "Скільки змін повернути")
            @RequestParam(defaultValue = "100") @Min(1) @Max(MAX_LIMIT) int limit) {

        return ResponseEntity.ok(ApiResponse.ok(auditService.getAllChanges(limit)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Зміни одного запису")
    public ResponseEntity<ApiResponse<BaseMetaData, AuditChangeResponse>> getChanges(
            @PathVariable String id,
            @RequestParam(defaultValue = "100") @Min(1) @Max(MAX_LIMIT) int limit) {

        return ResponseEntity.ok(ApiResponse.ok(auditService.getChangesOf(id, limit)));
    }

    @GetMapping("/{id}/snapshots")
    @Operation(
            summary = "Усі версії одного запису",
            description = "Яким запис був на кожній версії, найновіші першими")
    public ResponseEntity<ApiResponse<BaseMetaData, AuditSnapshotResponse>> getSnapshots(
            @PathVariable String id,
            @RequestParam(defaultValue = "100") @Min(1) @Max(MAX_LIMIT) int limit) {

        return ResponseEntity.ok(ApiResponse.ok(auditService.getSnapshotsOf(id, limit)));
    }

    @GetMapping(value = "/{id}/pretty", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Текстовий звіт про зміни запису, для читання людиною")
    public ResponseEntity<String> getPretty(@PathVariable String id) {
        return ResponseEntity.ok(auditService.getPrettyChangesOf(id));
    }
}
