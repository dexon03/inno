package chnu.edu.inno.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Опис API для Swagger UI: {@code http://localhost:8080/swagger-ui.html}. */
@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI innoOpenApi() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("Inno Items API")
                                .version("1.0.0")
                                .description(
                                        "CRUD над записами складу. Усі відповіді мають однакову "
                                                + "оболонку: службова частина meta і дані data."));
    }
}
