package chnu.edu.inno.repository;

import chnu.edu.inno.model.Item;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemRepository extends MongoRepository<Item, String> {

    Optional<Item> findByCode(String code);

    boolean existsByCode(String code);
}
