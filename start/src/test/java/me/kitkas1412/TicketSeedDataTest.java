package me.kitkas1412;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Seed local (R__seed_ticket_data.sql) chay duoc tren PostgreSQL that. Profile test khong bat db/seed
 * nen locations duoc override tai day. Container rieng de seed khong lam lech cac test khac.
 */
@SpringBootTest(
        classes = StartApplication.class,
        properties = "spring.flyway.locations=classpath:db/migration,classpath:db/seed")
@ActiveProfiles("test")
@Testcontainers
class TicketSeedDataTest {

    private static final String T1212 = "Đợt Mở Bán Vé Ngày 12/12";
    private static final String T0101 = "Đợt Mở Bán Vé Ngày 01/01";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.12");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    @Test
    void seedMigrationIsRecordedAsSuccessfulRepeatable() {
        Boolean success = jdbc.queryForObject(
                "select success from flyway_schema_history where script = 'R__seed_ticket_data.sql'",
                Boolean.class);
        assertThat(success).isTrue();
    }

    @Test
    void seedInsertsExactlyTwoTicketsWithDescriptionAndStatusOne() {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select name, description, status from ticket order by name");
        assertThat(rows).hasSize(2);
        assertThat(rows).extracting(r -> r.get("name")).containsExactlyInAnyOrder(T1212, T0101);
        assertThat(rows).allSatisfy(r -> {
            assertThat(r.get("description")).isNotNull();
            assertThat(r.get("status")).isEqualTo(1);
        });
    }

    @Test
    void seedInsertsFourItemsLinkedToTheirParentTicketId() {
        assertThat(jdbc.queryForObject("select count(*) from ticket_item", Integer.class)).isEqualTo(4);

        Long id1212 = jdbc.queryForObject("select id from ticket where name = ?", Long.class, T1212);
        Long id0101 = jdbc.queryForObject("select id from ticket where name = ?", Long.class, T0101);
        assertThat(id1212).isNotEqualTo(id0101);

        assertThat(jdbc.queryForList("select activity_id from ticket_item where name like 'Vé Sự Kiện 12/12%'",
                Long.class)).containsExactly(id1212, id1212);
        assertThat(jdbc.queryForList("select activity_id from ticket_item where name like 'Vé Sự Kiện 01/01%'",
                Long.class)).containsExactly(id0101, id0101);
    }

    @Test
    void seededItemsHaveExpectedStocksPricesAndFlags() {
        assertItem("Vé Sự Kiện 12/12 - Hạng Phổ Thông", 1000, 100_000L, 10_000L);
        assertItem("Vé Sự Kiện 12/12 - Hạng VIP", 500, 200_000L, 15_000L);
        assertItem("Vé Sự Kiện 01/01 - Hạng Phổ Thông", 2000, 100_000L, 10_000L);
        assertItem("Vé Sự Kiện 01/01 - Hạng VIP", 1000, 200_000L, 15_000L);
    }

    @Test
    void rerunningSeedScriptIsIdempotent() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                new ClassPathResource("db/seed/R__seed_ticket_data.sql"));
        populator.execute(dataSource);
        populator.execute(dataSource);

        assertThat(jdbc.queryForObject("select count(*) from ticket", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from ticket_item", Integer.class)).isEqualTo(4);
        assertThat(jdbc.queryForObject(
                "select count(*) from ticket_item ti join ticket t on t.id = ti.activity_id", Integer.class))
                .isEqualTo(4);
    }

    @Test
    void seededTicketsHaveExactDescriptionsAndTimeWindows() {
        assertTicket(T1212, "Sự kiện mở bán vé đặc biệt cho ngày 12/12",
                LocalDateTime.of(2024, 12, 12, 0, 0, 0), LocalDateTime.of(2024, 12, 12, 23, 59, 59));
        assertTicket(T0101, "Sự kiện mở bán vé cho ngày đầu năm mới 01/01",
                LocalDateTime.of(2025, 1, 1, 0, 0, 0), LocalDateTime.of(2025, 1, 1, 23, 59, 59));
    }

    @Test
    void seededItemsHaveExactDescriptionsAndSaleWindows() {
        assertItemText("Vé Sự Kiện 12/12 - Hạng Phổ Thông", "Vé phổ thông cho sự kiện ngày 12/12",
                LocalDateTime.of(2024, 12, 12, 0, 0, 0), LocalDateTime.of(2024, 12, 12, 23, 59, 59));
        assertItemText("Vé Sự Kiện 12/12 - Hạng VIP", "Vé VIP cho sự kiện ngày 12/12",
                LocalDateTime.of(2024, 12, 12, 0, 0, 0), LocalDateTime.of(2024, 12, 12, 23, 59, 59));
        assertItemText("Vé Sự Kiện 01/01 - Hạng Phổ Thông", "Vé phổ thông cho sự kiện ngày 01/01",
                LocalDateTime.of(2025, 1, 1, 0, 0, 0), LocalDateTime.of(2025, 1, 1, 23, 59, 59));
        assertItemText("Vé Sự Kiện 01/01 - Hạng VIP", "Vé VIP cho sự kiện ngày 01/01",
                LocalDateTime.of(2025, 1, 1, 0, 0, 0), LocalDateTime.of(2025, 1, 1, 23, 59, 59));
    }

    private void assertTicket(String name, String desc, LocalDateTime start, LocalDateTime end) {
        Map<String, Object> r = jdbc.queryForMap(
                "select description, start_time, end_time from ticket where name = ?", name);
        assertThat(r.get("description")).isEqualTo(desc);
        assertThat(((Timestamp) r.get("start_time")).toLocalDateTime()).isEqualTo(start);
        assertThat(((Timestamp) r.get("end_time")).toLocalDateTime()).isEqualTo(end);
    }

    private void assertItemText(String name, String desc, LocalDateTime start, LocalDateTime end) {
        Map<String, Object> r = jdbc.queryForMap(
                "select description, sale_start_time, sale_end_time from ticket_item where name = ?", name);
        assertThat(r.get("description")).isEqualTo(desc);
        assertThat(((Timestamp) r.get("sale_start_time")).toLocalDateTime()).isEqualTo(start);
        assertThat(((Timestamp) r.get("sale_end_time")).toLocalDateTime()).isEqualTo(end);
    }

    private void assertItem(String name, int stock, long original, long flash) {
        Map<String, Object> r = jdbc.queryForMap(
                "select stock_initial, stock_available, is_stock_prepared, price_original, price_flash, status "
                        + "from ticket_item where name = ?", name);
        assertThat(r.get("stock_initial")).isEqualTo(stock);
        assertThat(r.get("stock_available")).isEqualTo(stock);
        assertThat(r.get("is_stock_prepared")).isEqualTo(false);
        assertThat(((Number) r.get("price_original")).longValue()).isEqualTo(original);
        assertThat(((Number) r.get("price_flash")).longValue()).isEqualTo(flash);
        assertThat(r.get("status")).isEqualTo(1);
    }
}
