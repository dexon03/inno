package chnu.edu.inno.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.javers.core.metamodel.annotation.DiffIgnore;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

/**
 * Хто і коли востаннє чіпав запис.
 *
 * <p>Заповнює Spring Data Mongo: потрібні {@code @EnableMongoAuditing} і бін
 * {@link org.springframework.data.domain.AuditorAware}, обидва в {@code AuditConfiguration}.
 *
 * <p>Це не те саме, що історія змін із Javers, і одне одного не замінює. Тут — поточний стан:
 * останній, хто змінив, і коли. Javers зберігає всю послідовність версій і дифи між ними. Разом
 * вони відповідають на різні питання: «хто востаннє торкався цього запису» видно одразу в самому
 * записі, «що саме змінилося три правки тому» — лише в історії.
 *
 * <p>Усі чотири поля позначені {@code @DiffIgnore}, тож Javers не вважає їх змінами. Без цього
 * кожна правка запису давала б у історії три рядки замість одного: саму зміну плюс оновлення
 * {@code lastModifiedDate} і {@code lastModifiedBy}. Гірше інше: Mongo зберігає час із точністю
 * до мілісекунд, а в пам'яті він мікросекундний, тож перше ж читання-запис показувало зміну
 * {@code createdDate} з {@code ...191099Z} на {@code ...191Z} — правку, якої ніхто не робив.
 *
 * <p>Час зберігається в {@link Instant}, тобто в UTC. {@code LocalDateTime}, як у взірці лекції,
 * для аудиту не годиться: він не має зони, тож при переїзді сервера чи переході на літній час
 * записи стають непорівнюваними між собою.
 */
@Getter
@Setter
@Schema(description = "Службові дані про зміни запису")
public abstract class AuditMetadata {

    @DiffIgnore
    @CreatedDate
    @Schema(description = "Коли створено, UTC")
    private Instant createdDate;

    @DiffIgnore
    @CreatedBy
    @Schema(description = "Хто створив")
    private String createdBy;

    @DiffIgnore
    @LastModifiedDate
    @Schema(description = "Коли змінено востаннє, UTC")
    private Instant lastModifiedDate;

    @DiffIgnore
    @LastModifiedBy
    @Schema(description = "Хто змінив востаннє")
    private String lastModifiedBy;
}
