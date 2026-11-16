package chnu.edu.inno.service;

import chnu.edu.inno.exception.DuplicateItemCodeException;
import chnu.edu.inno.exception.ItemNotFoundException;
import chnu.edu.inno.model.Item;
import chnu.edu.inno.model.dto.request.ItemCreateRequest;
import chnu.edu.inno.model.dto.request.ItemUpdateRequest;
import chnu.edu.inno.repository.ItemRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * Робота із записами складу.
 *
 * <p>Сервіс нічого не знає ні про коди HTTP, ні про оболонку відповіді — він повертає записи або
 * кидає виняток. В оригіналі був метод {@code getItemByIdAsApiResponse}, який будував
 * {@code ApiResponse} з кодом 404 просто в сервісі; через це бізнес-логіка виявлялася прив'язаною
 * до транспорту, і той самий метод не можна було викликати, скажімо, з планувальника.
 *
 * <p>Тут межа проходить так: сервіс каже, що сталося, контролер перекладає це на HTTP.
 */
@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;

    public List<Item> getAllItems() {
        return itemRepository.findAll();
    }

    /**
     * @throws ItemNotFoundException якщо запису немає
     */
    public Item getItemById(String id) {
        return itemRepository.findById(id).orElseThrow(() -> new ItemNotFoundException(id));
    }

    /**
     * @throws DuplicateItemCodeException якщо код уже зайнятий
     */
    public Item createItem(ItemCreateRequest request) {
        if (itemRepository.existsByCode(request.code())) {
            throw new DuplicateItemCodeException(request.code());
        }
        return itemRepository.save(request.toItem());
    }

    /**
     * Оновлює наявний запис.
     *
     * <p>Саме оновлює, а не створює: в оригіналі {@code update} викликав {@code save} з тілом
     * запиту, тож запит на неіснуючий ідентифікатор мовчки створював новий запис, а запит без
     * ідентифікатора — ще один. Тут відсутній запис дає 404, як і має бути.
     *
     * @throws ItemNotFoundException якщо запису немає
     * @throws DuplicateItemCodeException якщо новий код належить іншому запису
     */
    public Item updateItem(String id, ItemUpdateRequest request) {
        Item existing = getItemById(id);

        itemRepository
                .findByCode(request.code())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new DuplicateItemCodeException(request.code());
                });

        existing.setName(request.name());
        existing.setCode(request.code());
        existing.setQuantity(request.quantity());
        return itemRepository.save(existing);
    }

    /**
     * @throws ItemNotFoundException якщо запису немає
     */
    public void deleteItemById(String id) {
        if (!itemRepository.existsById(id)) {
            throw new ItemNotFoundException(id);
        }
        itemRepository.deleteById(id);
    }

    /**
     * Сторінка записів, упорядкована за ідентифікатором.
     *
     * <p>Упорядкування обов'язкове. Без {@code Sort} Mongo не обіцяє сталого порядку між
     * запитами, і той самий запис може трапитися на двох сусідніх сторінках або не трапитися
     * взагалі.
     */
    public Page<Item> getPage(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        return itemRepository.findAll(pageable);
    }
}
