package chnu.edu.inno;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import chnu.edu.inno.model.Item;
import chnu.edu.inno.repository.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Перевіряє контракт API: форму відповіді, коди статусів і поведінку на некоректних даних.
 *
 * <p>Тести ходять через увесь застосунок до справжньої бази, бо саме на стиках і ламається те,
 * що тут перероблено: обробник помилок, перевірка запиту, унікальний індекс. Мок репозиторію
 * показав би, що контролер викликає сервіс, і нічого не сказав би про те, що бачить клієнт.
 *
 * <p>Потрібна MongoDB на {@code localhost:27017}: {@code docker run -d -p 27017:27017 mongo:7}.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("API записів складу")
class ItemControllerTest {

    @Autowired private WebApplicationContext context;
    @Autowired private ItemRepository itemRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        itemRepository.deleteAll();
    }

    private Item saved(String name, String code, int quantity) {
        return itemRepository.save(new Item(name, code, quantity));
    }

    // ---------- форма відповіді ----------

    @Test
    @DisplayName("успішна відповідь має meta і data")
    void successHasEnvelope() throws Exception {
        Item item = saved("Milk", "000001", 3);

        mockMvc.perform(get("/api/v1/items/{id}", item.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.code").value(200))
                .andExpect(jsonPath("$.meta.success").value(true))
                .andExpect(jsonPath("$.meta.errorMessage").doesNotExist())
                .andExpect(jsonPath("$.data[0].name").value("Milk"))
                .andExpect(jsonPath("$.data[0].code").value("000001"))
                .andExpect(jsonPath("$.data[0].quantity").value(3));
    }

    @Test
    @DisplayName("відповідь на помилку має ту саму оболонку, що й успішна")
    void errorHasSameEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/items/{id}", "000000000000000000000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.meta.code").value(404))
                .andExpect(jsonPath("$.meta.success").value(false))
                .andExpect(jsonPath("$.meta.errorMessage").isNotEmpty())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("data завжди список, навіть коли запис один")
    void dataIsAlwaysArray() throws Exception {
        Item item = saved("Milk", "000001", 1);

        mockMvc.perform(get("/api/v1/items/{id}", item.getId()))
                .andExpect(jsonPath("$.data").isArray());
        mockMvc.perform(get("/api/v1/items"))
                .andExpect(jsonPath("$.data").isArray());
    }

    // ---------- читання ----------

    @Test
    @DisplayName("список повертає всі записи")
    void listReturnsAll() throws Exception {
        saved("Milk", "000001", 1);
        saved("Bread", "000002", 2);

        mockMvc.perform(get("/api/v1/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("порожній список -- це успіх із порожніми даними, а не помилка")
    void emptyListIsSuccess() throws Exception {
        mockMvc.perform(get("/api/v1/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    // ---------- сторінки ----------

    @Test
    @DisplayName("сторінка повертає дані про навігацію")
    void pageReturnsPaginationMeta() throws Exception {
        for (int i = 1; i <= 8; i++) {
            saved("Item" + i, String.format("%06d", i), i);
        }

        mockMvc.perform(get("/api/v1/items/page").param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.number").value(0))
                .andExpect(jsonPath("$.meta.size").value(5))
                .andExpect(jsonPath("$.meta.totalElements").value(8))
                .andExpect(jsonPath("$.meta.totalPages").value(2))
                .andExpect(jsonPath("$.meta.first").value(true))
                .andExpect(jsonPath("$.meta.last").value(false))
                .andExpect(jsonPath("$.data.length()").value(5));
    }

    @Test
    @DisplayName("остання сторінка позначена як остання")
    void lastPageIsMarked() throws Exception {
        for (int i = 1; i <= 8; i++) {
            saved("Item" + i, String.format("%06d", i), i);
        }

        mockMvc.perform(get("/api/v1/items/page").param("page", "1").param("size", "5"))
                .andExpect(jsonPath("$.meta.last").value(true))
                .andExpect(jsonPath("$.data.length()").value(3));
    }

    @Test
    @DisplayName("від'ємний номер сторінки відхиляється, а не падає з 500")
    void negativePageRejected() throws Exception {
        mockMvc.perform(get("/api/v1/items/page").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.meta.success").value(false));
    }

    @Test
    @DisplayName("завеликий розмір сторінки відхиляється")
    void oversizedPageRejected() throws Exception {
        mockMvc.perform(get("/api/v1/items/page").param("size", "1000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.meta.success").value(false));
    }

    // ---------- створення ----------

    @Test
    @DisplayName("створення повертає 201 і заголовок Location")
    void createReturns201AndLocation() throws Exception {
        String body = """
                {"name":"Milk","code":"000001","quantity":3}
                """;

        mockMvc.perform(post("/api/v1/items").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.meta.code").value(201))
                .andExpect(jsonPath("$.data[0].name").value("Milk"));

        assertThat(itemRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("створення з порожньою назвою відхиляється з поясненням")
    void createRejectsBlankName() throws Exception {
        String body = """
                {"name":"","code":"000001","quantity":1}
                """;

        mockMvc.perform(post("/api/v1/items").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.meta.success").value(false))
                .andExpect(jsonPath("$.meta.errorMessage").value(org.hamcrest.Matchers.containsString("name")));

        assertThat(itemRepository.count()).isZero();
    }

    @Test
    @DisplayName("створення з від'ємною кількістю відхиляється")
    void createRejectsNegativeQuantity() throws Exception {
        String body = """
                {"name":"Milk","code":"000001","quantity":-5}
                """;

        mockMvc.perform(post("/api/v1/items").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        assertThat(itemRepository.count()).isZero();
    }

    @Test
    @DisplayName("створення з неправильним кодом відхиляється")
    void createRejectsBadCode() throws Exception {
        String body = """
                {"name":"Milk","code":"ABC","quantity":1}
                """;

        mockMvc.perform(post("/api/v1/items").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("повторний код дає 409, а не другий запис")
    void duplicateCodeRejected() throws Exception {
        saved("Milk", "000001", 1);
        String body = """
                {"name":"Інше молоко","code":"000001","quantity":2}
                """;

        mockMvc.perform(post("/api/v1/items").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.meta.code").value(409))
                .andExpect(jsonPath("$.meta.success").value(false));

        assertThat(itemRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("id з тіла запиту ігнорується: створення не може перезаписати чужий запис")
    void createCannotOverwriteById() throws Exception {
        Item existing = saved("Milk", "000001", 1);
        String body = String.format("""
                {"id":"%s","name":"Підміна","code":"000099","quantity":9}
                """, existing.getId());

        mockMvc.perform(post("/api/v1/items").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        assertThat(itemRepository.count()).isEqualTo(2);
        assertThat(itemRepository.findById(existing.getId()).orElseThrow().getName())
                .isEqualTo("Milk");
    }

    @Test
    @DisplayName("зіпсований JSON дає 400 у тій самій оболонці")
    void malformedJsonRejected() throws Exception {
        mockMvc.perform(post("/api/v1/items").contentType(MediaType.APPLICATION_JSON).content("{не json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.meta.success").value(false));
    }

    // ---------- оновлення ----------

    @Test
    @DisplayName("оновлення змінює запис")
    void updateChangesItem() throws Exception {
        Item item = saved("Milk", "000001", 1);
        String body = """
                {"name":"Молоко","code":"000001","quantity":7}
                """;

        mockMvc.perform(put("/api/v1/items/{id}", item.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Молоко"))
                .andExpect(jsonPath("$.data[0].quantity").value(7));

        assertThat(itemRepository.findById(item.getId()).orElseThrow().getQuantity()).isEqualTo(7);
    }

    @Test
    @DisplayName("оновлення неіснуючого запису дає 404 і нічого не створює")
    void updateMissingGives404() throws Exception {
        String body = """
                {"name":"Молоко","code":"000001","quantity":7}
                """;

        mockMvc.perform(put("/api/v1/items/{id}", "000000000000000000000000")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.meta.success").value(false));

        assertThat(itemRepository.count()).isZero();
    }

    @Test
    @DisplayName("оновлення на чужий код дає 409")
    void updateToTakenCodeRejected() throws Exception {
        saved("Milk", "000001", 1);
        Item bread = saved("Bread", "000002", 2);
        String body = """
                {"name":"Bread","code":"000001","quantity":2}
                """;

        mockMvc.perform(put("/api/v1/items/{id}", bread.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("запис може лишити собі свій же код")
    void updateKeepsOwnCode() throws Exception {
        Item item = saved("Milk", "000001", 1);
        String body = """
                {"name":"Молоко","code":"000001","quantity":5}
                """;

        mockMvc.perform(put("/api/v1/items/{id}", item.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    // ---------- видалення ----------

    @Test
    @DisplayName("видалення прибирає запис і повертає оболонку")
    void deleteRemovesItem() throws Exception {
        Item item = saved("Milk", "000001", 1);

        mockMvc.perform(delete("/api/v1/items/{id}", item.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.success").value(true));

        assertThat(itemRepository.count()).isZero();
    }

    @Test
    @DisplayName("видалення неіснуючого запису дає 404, а не мовчазний успіх")
    void deleteMissingGives404() throws Exception {
        mockMvc.perform(delete("/api/v1/items/{id}", "000000000000000000000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.meta.success").value(false));
    }

    // ---------- невідомий шлях ----------

    @Test
    @DisplayName("невідомий шлях теж повертає оболонку, а не сторінку Spring")
    void unknownPathUsesEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/items/page/unknown/path"))
                .andExpect(jsonPath("$.meta.success").value(false));
    }
}
