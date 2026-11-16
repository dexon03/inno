package chnu.edu.inno.repository;

import chnu.edu.inno.model.Item;
import java.util.Optional;
import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Сховище записів складу.
 *
 * <p>{@link JaversSpringDataAuditable} вмикає аудит на рівні репозиторію: Javers перехоплює
 * {@code save}, {@code saveAll} і {@code delete} і сам зберігає знімок після кожної зміни.
 *
 * <p>Взірець лекції ставив {@code @JaversAuditable} на методи сервісу. Це працює, але аудит
 * тоді прив'язаний до того, що хтось не забуде позначити новий метод. Перехоплення на
 * репозиторії ловить усі записи в базу незалежно від того, звідки їх викликали, зокрема й із
 * коду, написаного пізніше.
 */
@Repository
@JaversSpringDataAuditable
public interface ItemRepository extends MongoRepository<Item, String> {

    Optional<Item> findByCode(String code);

    boolean existsByCode(String code);
}
