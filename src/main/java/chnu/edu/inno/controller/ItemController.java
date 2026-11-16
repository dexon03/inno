package chnu.edu.inno.controller;

import chnu.edu.inno.model.Item;
import chnu.edu.inno.model.dto.request.ItemCreateRequest;
import chnu.edu.inno.model.dto.request.ItemUpdateRequest;
import chnu.edu.inno.model.dto.response.ApiResponse;
import chnu.edu.inno.model.dto.response.BaseMetaData;
import chnu.edu.inno.model.dto.response.ItemResponse;
import chnu.edu.inno.model.dto.response.PaginationMetaData;
import chnu.edu.inno.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * CRUD над записами складу.
 *
 * <p>Кожен метод повертає {@link ApiResponse}: і успіх, і помилка мають однакову форму. Статус
 * HTTP при цьому не підміняється — він лишається в заголовку, а в тілі дублюється, щоб відповідь
 * лишалася самодостатньою, коли її збережуть окремо від транспорту.
 *
 * <p>Контролер не містить логіки: його робота — перекласти HTTP на виклик сервісу й назад.
 *
 * <p>Анотації {@code @Min}/{@code @Max} на параметрах виконує вбудована перевірка Spring, і
 * {@code @Validated} на класі тут не потрібна. Навпаки, вона шкодить: вмикає стару перевірку
 * через AOP, яка загортає контролер у проксі CGLIB і кидає {@code ConstraintViolationException}
 * замість {@code HandlerMethodValidationException} — тобто виняток, якого обробник не чекає, і
 * клієнт отримує 500 замість 400.
 */
@RestController
@RequestMapping("api/v1/items")
@RequiredArgsConstructor
@Tag(name = "Items", description = "Записи складу")
public class ItemController {

    private static final int MAX_PAGE_SIZE = 100;

    private final ItemService itemService;

    @GetMapping("/{id}")
    @Operation(summary = "Запис за ідентифікатором")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "Запис знайдено")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", description = "Запису немає",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<BaseMetaData, ItemResponse>> getItemById(
            @PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(ItemResponse.from(itemService.getItemById(id))));
    }

    @GetMapping
    @Operation(
            summary = "Усі записи",
            description = "Повертає весь список. Для великих обсягів беріть /page.")
    public ResponseEntity<ApiResponse<BaseMetaData, ItemResponse>> getAllItems() {
        List<ItemResponse> items =
                itemService.getAllItems().stream().map(ItemResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(items));
    }

    @GetMapping("/page")
    @Operation(
            summary = "Сторінка записів",
            description = "Упорядковано за ідентифікатором. Типово 5 записів на сторінку.")
    public ResponseEntity<ApiResponse<PaginationMetaData, ItemResponse>> getItemsPage(
            @Parameter(description = "Номер сторінки, від нуля")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Розмір сторінки, не більший за 100")
            @RequestParam(defaultValue = "5") @Min(1) @Max(MAX_PAGE_SIZE) int size) {

        return ResponseEntity.ok(ApiResponse.page(itemService.getPage(page, size).map(ItemResponse::from)));
    }

    /**
     * Створює запис.
     *
     * <p>Віддає 201 і заголовок {@code Location} із адресою створеного ресурсу — так належить
     * відповідати на створення. В оригіналі повертався 200 і сам документ, без адреси.
     */
    @PostMapping
    @Operation(summary = "Створити запис")
    public ResponseEntity<ApiResponse<BaseMetaData, ItemResponse>> createItem(
            @Valid @RequestBody ItemCreateRequest request) {

        Item created = itemService.createItem(request);
        URI location = URI.create("/api/v1/items/" + created.getId());
        return ResponseEntity.created(location).body(ApiResponse.created(ItemResponse.from(created)));
    }

    /**
     * Оновлює запис за ідентифікатором зі шляху.
     *
     * <p>В оригіналі було {@code PUT /api/v1/items} з ідентифікатором у тілі. Тепер адреса
     * ресурсу одна, і вона в шляху.
     */
    @PutMapping("/{id}")
    @Operation(summary = "Оновити запис")
    public ResponseEntity<ApiResponse<BaseMetaData, ItemResponse>> updateItem(
            @PathVariable String id, @Valid @RequestBody ItemUpdateRequest request) {

        return ResponseEntity.ok(
                ApiResponse.ok(ItemResponse.from(itemService.updateItem(id, request))));
    }

    /**
     * Видаляє запис.
     *
     * <p>Повертає 200 з оболонкою, а не 204 без тіла: інакше відповідь на видалення була б
     * єдиною в усьому API, яку клієнт мусив би розбирати окремо. Код 204 лишається всередині
     * {@code meta.code} — для клієнтів, яким він потрібен.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Видалити запис")
    public ResponseEntity<ApiResponse<BaseMetaData, Void>> deleteItem(@PathVariable String id) {
        itemService.deleteItemById(id);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.noContent());
    }
}
