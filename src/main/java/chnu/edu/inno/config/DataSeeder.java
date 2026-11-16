package chnu.edu.inno.config;

import chnu.edu.inno.model.Item;
import chnu.edu.inno.repository.ItemRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Наповнює базу прикладами для ручної перевірки.
 *
 * <p>В оригіналі це робив метод із {@code @PostConstruct} у сервісі, який починався з
 * {@code deleteAll()}. Тобто кожен запуск застосунку стирав базу — поведінка, прийнятна на
 * занятті й руйнівна будь-де ще, причому сховати її в сервісі означало, що помітити її можна
 * лише прочитавши код.
 *
 * <p>Тут наповнення винесене в конфігурацію, увімкнене лише під профілем {@code dev} і нічого
 * не стирає: якщо записи вже є, воно просто нічого не робить.
 */
@Configuration
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DataSeeder {

    @Bean
    public ApplicationRunner seedItems(ItemRepository itemRepository) {
        return args -> {
            if (itemRepository.count() > 0) {
                log.info("База вже містить записи, наповнення пропущено");
                return;
            }
            List<Item> items =
                    List.of(
                            new Item("Milk", "000001", 1),
                            new Item("Bread", "000002", 2),
                            new Item("Sugar", "000003", 3),
                            new Item("Salt", "000004", 1),
                            new Item("Meat", "000005", 1),
                            new Item("Chips", "000006", 3),
                            new Item("Coca-Cola", "000007", 1),
                            new Item("Hot dog", "000008", 1));
            itemRepository.saveAll(items);
            log.info("Додано {} прикладів", items.size());
        };
    }
}
