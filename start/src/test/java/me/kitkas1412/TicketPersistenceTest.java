package me.kitkas1412;

import me.kitkas1412.tixora.domain.model.entity.TicketDetail;
import me.kitkas1412.tixora.domain.repository.TicketDetailRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = StartApplication.class)
@ActiveProfiles("test")
@Testcontainers
@Transactional
class TicketPersistenceTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.12");

    @Autowired
    private TicketDetailRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoadsWithFlywayMigrationAndHibernateValidate() {
        assertThat(repository).isNotNull();
        assertThat(POSTGRES.isRunning()).isTrue();
    }

    @Test
    void flywayAppliedVersionOneSuccessfully() {
        Boolean success = jdbcTemplate.queryForObject(
                "select success from flyway_schema_history where version = '1'", Boolean.class);
        assertThat(success).isTrue();
    }

    @Test
    void savedTicketDetailIsReadBackWithGeneratedIdAndTimestamps() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 1, 12, 0);

        TicketDetail saved = repository.save(newDetail("Early bird", "desc", start, end));

        entityManager.flush();
        entityManager.clear();
        assertThat(saved.getId()).isNotNull();
        TicketDetail found = repository.findById(saved.getId()).orElseThrow();
        assertThat(found.getName()).isEqualTo("Early bird");
        assertThat(found.getDescription()).isEqualTo("desc");
        assertThat(found.getStockInitial()).isEqualTo(100);
        assertThat(found.getStockAvailable()).isEqualTo(90);
        assertThat(found.isStockPrepared()).isTrue();
        assertThat(found.getPriceOriginal()).isEqualTo(500_000L);
        assertThat(found.getPriceFlash()).isEqualTo(250_000L);
        assertThat(found.getSaleStartTime()).isEqualTo(start);
        assertThat(found.getSaleEndTime()).isEqualTo(end);
        assertThat(found.getStatus()).isEqualTo(1);
        assertThat(found.getActivityId()).isEqualTo(42L);
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
        assertThat(repository.findAll()).extracting(TicketDetail::getId).contains(saved.getId());
    }

    @Test
    void vietnameseTextRoundTripsIntact() {
        String name = "Vé Tết";
        String description = "Vé ưu đãi dịp Tết Nguyên Đán – Hà Nội, Sài Gòn";

        TicketDetail saved = repository.save(newDetail(name, description,
                LocalDateTime.of(2026, 2, 1, 0, 0), LocalDateTime.of(2026, 2, 2, 0, 0)));

        entityManager.flush();
        entityManager.clear();
        TicketDetail found = repository.findById(saved.getId()).orElseThrow();
        assertThat(found.getName()).isEqualTo(name);
        assertThat(found.getDescription()).isEqualTo(description);
    }

    private static TicketDetail newDetail(String name, String description,
                                          LocalDateTime start, LocalDateTime end) {
        return new TicketDetail(name, description, 100, 90, true, 500_000L, 250_000L, start, end, 1, 42L);
    }
}
