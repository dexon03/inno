package chnu.edu.inno.service;

import chnu.edu.inno.model.Item;
import chnu.edu.inno.model.dto.response.AuditChangeResponse;
import chnu.edu.inno.model.dto.response.AuditSnapshotResponse;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.javers.core.Javers;
import org.javers.core.commit.CommitMetadata;
import org.javers.core.diff.Change;
import org.javers.core.diff.changetype.NewObject;
import org.javers.core.diff.changetype.ObjectRemoved;
import org.javers.core.diff.changetype.PropertyChange;
import org.javers.core.metamodel.object.CdoSnapshot;
import org.javers.repository.jql.JqlQuery;
import org.javers.repository.jql.QueryBuilder;
import org.springframework.stereotype.Service;

/**
 * Читає історію змін записів зі сховища Javers.
 *
 * <p>Переводить внутрішні типи Javers у власні DTO. Сенс той самий, що й з {@code ItemResponse}
 * для записів: контракт API не має змінюватися разом із версією бібліотеки.
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private static final int DEFAULT_LIMIT = 100;

    private final Javers javers;

    /** Усі зміни всіх записів, найновіші першими. */
    public List<AuditChangeResponse> getAllChanges(int limit) {
        return toChanges(javers.findChanges(query(QueryBuilder.byClass(Item.class), limit)));
    }

    /** Зміни одного запису. */
    public List<AuditChangeResponse> getChangesOf(String itemId, int limit) {
        return toChanges(
                javers.findChanges(query(QueryBuilder.byInstanceId(itemId, Item.class), limit)));
    }

    /** Усі версії одного запису, найновіші першими. */
    public List<AuditSnapshotResponse> getSnapshotsOf(String itemId, int limit) {
        return javers
                .findSnapshots(query(QueryBuilder.byInstanceId(itemId, Item.class), limit))
                .stream()
                .map(AuditService::toSnapshot)
                .toList();
    }

    /**
     * Текстовий звіт про зміни запису.
     *
     * <p>Готується самим Javers і призначений для людини, а не для програми.
     */
    public String getPrettyChangesOf(String itemId) {
        return javers
                .findChanges(query(QueryBuilder.byInstanceId(itemId, Item.class), DEFAULT_LIMIT))
                .prettyPrint();
    }

    /**
     * Обмеження обов'язкове.
     *
     * <p>Типово Javers віддає сто записів, але взірець лекції на це не зважав і міг віддати
     * усю історію одним шматком. Для аудиту це найгірший випадок: історія лише росте, тож
     * запит, який сьогодні повертає десяток рядків, за рік покладе відповідь.
     */
    private static JqlQuery query(QueryBuilder builder, int limit) {
        return builder.limit(limit).withNewObjectChanges().build();
    }

    private static List<AuditChangeResponse> toChanges(List<Change> changes) {
        return changes.stream().map(AuditService::toChange).flatMap(Optional::stream).toList();
    }

    /**
     * Перетворює зміну Javers на відповідь.
     *
     * <p>Цікавлять лише зміни полів. {@link NewObject} і {@link ObjectRemoved} описують появу й
     * зникнення запису цілком — їх видно у знімках як {@code INITIAL} і {@code TERMINAL}, тож
     * у переліку змін полів вони лише створювали б рядки без значень.
     */
    private static Optional<AuditChangeResponse> toChange(Change change) {
        if (!(change instanceof PropertyChange<?> propertyChange)) {
            return Optional.empty();
        }
        CommitMetadata commit = change.getCommitMetadata().orElse(null);

        return Optional.of(
                new AuditChangeResponse(
                        localId(change.getAffectedGlobalId().value()),
                        propertyChange.getPropertyName(),
                        text(propertyChange.getLeft()),
                        text(propertyChange.getRight()),
                        commit == null ? null : commit.getAuthor(),
                        commit == null ? null : commit.getCommitDate().toInstant(ZoneOffset.UTC),
                        commit == null ? 0 : commit.getId().getMajorId()));
    }

    private static AuditSnapshotResponse toSnapshot(CdoSnapshot snapshot) {
        return new AuditSnapshotResponse(
                localId(snapshot.getGlobalId().value()),
                snapshot.getVersion(),
                snapshot.getType().name(),
                snapshot.getCommitMetadata().getAuthor(),
                snapshot.getCommitMetadata().getCommitDate().toInstant(ZoneOffset.UTC),
                text(snapshot.getPropertyValue("name")),
                text(snapshot.getPropertyValue("code")),
                snapshot.getPropertyValue("quantity") instanceof Number number
                        ? number.intValue()
                        : null);
    }

    /** Javers зберігає ідентифікатор як {@code chnu.edu.inno.model.Item/6afb...}; беремо хвіст. */
    private static String localId(String globalId) {
        int slash = globalId.lastIndexOf('/');
        return slash < 0 ? globalId : globalId.substring(slash + 1);
    }

    private static String text(Object value) {
        return value == null ? null : value.toString();
    }
}
