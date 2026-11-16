package chnu.edu.inno.config;

import java.util.Optional;
import org.javers.spring.auditable.AuthorProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * Вмикає аудит і каже обом механізмам, хто саме вносить зміни.
 *
 * <p>Механізмів два, і кожен питає про автора по-своєму: Spring Data через
 * {@link AuditorAware} заповнює поля {@code createdBy} і {@code lastModifiedBy} у самому записі,
 * Javers через {@link AuthorProvider} підписує кожен знімок в історії. Джерело імені має бути
 * одне, інакше запис і його історія казатимуть різне.
 */
@Configuration
@EnableMongoAuditing
public class AuditConfiguration {

    /**
     * Хто вносить зміни.
     *
     * <p>У проєкті немає автентифікації, тож ім'я береться з налаштування, а типово — з
     * користувача системи. У взірці лекції тут стояло жорстко вписане {@code "ADMIN"} для Javers
     * і {@code System.getProperty("user.name")} для Spring Data, тобто той самий запис
     * підписувався двома різними іменами.
     *
     * <p>Коли з'явиться автентифікація, міняти треба буде лише цей метод: замість налаштування
     * тут стане {@code SecurityContextHolder.getContext().getAuthentication().getName()}.
     */
    private String currentUser() {
        return System.getProperty("inno.audit.author", System.getProperty("user.name", "system"));
    }

    /** Для Spring Data: заповнює createdBy і lastModifiedBy у записі. */
    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.of(currentUser());
    }

    /** Для Javers: підписує кожен знімок в історії. */
    @Bean
    public AuthorProvider javersAuthorProvider() {
        return this::currentUser;
    }
}
