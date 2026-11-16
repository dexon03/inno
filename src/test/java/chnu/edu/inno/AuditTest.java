package chnu.edu.inno;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import chnu.edu.inno.model.Item;
import chnu.edu.inno.repository.ItemRepository;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.javers.core.Javers;
import org.javers.repository.jql.QueryBuilder;
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
 * Перевіряє, що історія змін справді ведеться і що в ній немає зайвого.
 *
 * <p>Аудит легко зламати непомітно: він нічого не повертає викликачеві, тож якщо перестане
 * записуватися, жоден інший тест цього не побачить.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Аудит змін")
class AuditTest {

    @Autowired private WebApplicationContext context;
    @Autowired private ItemRepository itemRepository;
    @Autowired private Javers javers;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        itemRepository.deleteAll();
    }

    private String createItem(String name, String code, int quantity) throws Exception {
        String body = """
                {"name":"%s","code":"%s","quantity":%d}
                """.formatted(name, code, quantity);
        String json =
                mockMvc.perform(post("/api/v1/items").contentType(MediaType.APPLICATION_JSON).content(body))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return JsonPath.read(json, "$.data[0].id");
    }

    private void updateItem(String id, String name, String code, int quantity) throws Exception {
        String body = """
                {"name":"%s","code":"%s","quantity":%d}
                """.formatted(name, code, quantity);
        mockMvc.perform(put("/api/v1/items/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("створення запису потрапляє в історію")
    void createIsAudited() throws Exception {
        String id = createItem("Кава", "000500", 5);

        assertThat(javers.findSnapshots(QueryBuilder.byInstanceId(id, Item.class).build()))
                .hasSize(1)
                .first()
                .satisfies(s -> assertThat(s.getType().name()).isEqualTo("INITIAL"));
    }

    @Test
    @DisplayName("кожна правка додає версію")
    void eachUpdateAddsVersion() throws Exception {
        String id = createItem("Кава", "000500", 5);
        updateItem(id, "Кава мелена", "000500", 9);
        updateItem(id, "Кава мелена", "000500", 2);

        mockMvc.perform(get("/api/v1/audit/items/{id}/snapshots", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].version").value(3))
                .andExpect(jsonPath("$.data[0].quantity").value(2))
                .andExpect(jsonPath("$.data[2].version").value(1))
                .andExpect(jsonPath("$.data[2].quantity").value(5));
    }

    @Test
    @DisplayName("видалення теж потрапляє в історію")
    void deleteIsAudited() throws Exception {
        String id = createItem("Кава", "000500", 5);
        mockMvc.perform(delete("/api/v1/items/{id}", id)).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/audit/items/{id}/snapshots", id))
                .andExpect(jsonPath("$.data[0].changeType").value("TERMINAL"));
    }

    @Test
    @DisplayName("історія зберігає старе значення поля, а не лише нове")
    void changeKeepsOldValue() throws Exception {
        String id = createItem("Кава", "000500", 5);
        updateItem(id, "Кава", "000500", 9);

        mockMvc.perform(get("/api/v1/audit/items/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].property").value("quantity"))
                .andExpect(jsonPath("$.data[0].oldValue").value("5"))
                .andExpect(jsonPath("$.data[0].newValue").value("9"));
    }

    @Test
    @DisplayName("службові поля аудиту не потрапляють в історію як зміни")
    void metadataFieldsAreNotChanges() throws Exception {
        // Без @DiffIgnore кожна правка давала б іще два рядки, lastModifiedDate і
        // lastModifiedBy, а також фальшиву зміну createdDate через різну точність часу
        // в пам'яті та в Mongo.
        String id = createItem("Кава", "000500", 5);
        updateItem(id, "Кава", "000500", 9);

        String json =
                mockMvc.perform(get("/api/v1/audit/items/{id}", id))
                        .andReturn().getResponse().getContentAsString();
        List<String> properties = JsonPath.read(json, "$.data[*].property");

        assertThat(properties)
                .doesNotContain("createdDate", "createdBy", "lastModifiedDate", "lastModifiedBy");
    }

    @Test
    @DisplayName("правка без фактичних змін не додає версію")
    void noOpUpdateAddsNothing() throws Exception {
        String id = createItem("Кава", "000500", 5);
        updateItem(id, "Кава", "000500", 5);

        mockMvc.perform(get("/api/v1/audit/items/{id}/snapshots", id))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @DisplayName("історії різних записів не змішуються")
    void historiesAreSeparate() throws Exception {
        String coffee = createItem("Кава", "000500", 5);
        String tea = createItem("Чай", "000501", 2);
        updateItem(coffee, "Кава мелена", "000500", 7);

        mockMvc.perform(get("/api/v1/audit/items/{id}/snapshots", tea))
                .andExpect(jsonPath("$.data.length()").value(1));
        mockMvc.perform(get("/api/v1/audit/items/{id}/snapshots", coffee))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("кожна зміна підписана автором")
    void changesHaveAuthor() throws Exception {
        String id = createItem("Кава", "000500", 5);
        updateItem(id, "Кава", "000500", 9);

        mockMvc.perform(get("/api/v1/audit/items/{id}", id))
                .andExpect(jsonPath("$.data[0].author").isNotEmpty())
                .andExpect(jsonPath("$.data[0].changedAt").isNotEmpty());
    }

    @Test
    @DisplayName("запис і його історія підписані тим самим іменем")
    void recordAndHistoryAgreeOnAuthor() throws Exception {
        // У взірці лекції Spring Data підписувала запис іменем користувача системи, а Javers
        // підписував історію жорстко вписаним "ADMIN" -- тобто одна правка мала двох авторів.
        String id = createItem("Кава", "000500", 5);

        String json =
                mockMvc.perform(get("/api/v1/audit/items/{id}", id))
                        .andReturn().getResponse().getContentAsString();
        String historyAuthor = ((List<String>) JsonPath.read(json, "$.data[*].author")).get(0);
        String recordAuthor = itemRepository.findById(id).orElseThrow().getCreatedBy();

        assertThat(historyAuthor).isEqualTo(recordAuthor);
    }

    @Test
    @DisplayName("відповіді аудиту мають ту саму оболонку, що й решта API")
    void auditUsesSameEnvelope() throws Exception {
        String id = createItem("Кава", "000500", 5);

        mockMvc.perform(get("/api/v1/audit/items/{id}", id))
                .andExpect(jsonPath("$.meta.code").value(200))
                .andExpect(jsonPath("$.meta.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("завелике обмеження відхиляється")
    void oversizedLimitRejected() throws Exception {
        mockMvc.perform(get("/api/v1/audit/items").param("limit", "10000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.meta.success").value(false));
    }

    @Test
    @DisplayName("текстовий звіт віддається як текст")
    void prettyReportIsPlainText() throws Exception {
        String id = createItem("Кава", "000500", 5);
        updateItem(id, "Кава", "000500", 9);

        mockMvc.perform(get("/api/v1/audit/items/{id}/pretty", id))
                .andExpect(status().isOk())
                .andExpect(content -> assertThat(
                        content.getResponse().getContentAsString()).contains("quantity"));
    }
}
