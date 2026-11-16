package chnu.edu.inno.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Запис складу — документ у базі.
 *
 * <p>Назовні не віддається: для цього є {@code ItemResponse}. Такий поділ дозволяє міняти
 * сховище, не змінюючи контракт API.
 *
 * <p>Успадковує {@link AuditMetadata}, тож разом із записом зберігається, хто і коли змінив
 * його востаннє. Повна історія змін живе окремо, в сховищі Javers.
 */
@Document(collection = "items")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Item extends AuditMetadata {

    @Id
    private String id;

    private String name;

    /**
     * Код запису.
     *
     * <p>Унікальний індекс на рівні бази. Перевірки в коді мало: між «перевірили, що коду немає»
     * і «зберегли» встигає пройти паралельний запит, і два записи з однаковим кодом усе одно
     * опиняться в базі. Індекс — єдине місце, де цю умову можна втримати напевно.
     */
    @Indexed(unique = true)
    private String code;

    private int quantity;

    public Item(String name, String code, int quantity) {
        this.name = name;
        this.code = code;
        this.quantity = quantity;
    }

    /**
     * Рівність за ідентифікатором.
     *
     * <p>{@code final}, бо Lombok та проксі Spring Data інакше можуть згенерувати власну версію
     * і зламати узгодженість із {@link #hashCode()}.
     */
    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Item item)) {
            return false;
        }
        return id != null && id.equals(item.id);
    }

    @Override
    public final int hashCode() {
        return id == null ? 0 : id.hashCode();
    }
}
