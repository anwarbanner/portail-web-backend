package portail.web.backend.exemple.portail.web.backend.testcontainers_test;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;
import org.testcontainers.shaded.com.github.dockerjava.core.DefaultDockerClientConfig;
import org.testcontainers.shaded.com.github.dockerjava.core.DockerClientImpl;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Valide les migrations Flyway sur un conteneur PostgreSQL démarré via docker-java
 * avec l'API version 1.47 (Docker Desktop 29.x exige minimum 1.40).
 */
@SpringBootTest(properties = {
        "app.admin.seed.enabled=false",
        "app.storage.norme-pdf-dir=target/test-storage/normes"
})
class FlywayMigrationTest {

    // ── Bootstrap Docker : initialisation statique avant le contexte Spring ──

    private static final DockerClient DOCKER;
    private static final String CONTAINER_ID;
    private static final String JDBC_URL;
    private static final String PG_USER     = "tc_test";
    private static final String PG_PASSWORD = "tc_test";
    private static final String PG_DB       = "tc_testdb";

    static {
        DefaultDockerClientConfig cfg = DefaultDockerClientConfig
                .createDefaultConfigBuilder()
                .withDockerHost("tcp://localhost:2375")
                .withApiVersion("1.47")
                .build();

        ZerodepDockerHttpClient http = new ZerodepDockerHttpClient.Builder()
                .dockerHost(cfg.getDockerHost())
                .build();

        DOCKER = DockerClientImpl.getInstance(cfg, http);

        pullIfAbsent("postgres", "15-alpine");

        Ports portBindings = new Ports();
        portBindings.bind(ExposedPort.tcp(5432), Ports.Binding.empty());

        CreateContainerResponse container = DOCKER.createContainerCmd("postgres:15-alpine")
                .withEnv(
                        "POSTGRES_DB="       + PG_DB,
                        "POSTGRES_USER="     + PG_USER,
                        "POSTGRES_PASSWORD=" + PG_PASSWORD
                )
                .withExposedPorts(ExposedPort.tcp(5432))
                .withHostConfig(HostConfig.newHostConfig().withPortBindings(portBindings))
                .exec();

        CONTAINER_ID = container.getId();
        DOCKER.startContainerCmd(CONTAINER_ID).exec();

        InspectContainerResponse inspect = DOCKER.inspectContainerCmd(CONTAINER_ID).exec();
        Ports.Binding[] bindings = inspect.getNetworkSettings().getPorts()
                .getBindings().get(ExposedPort.tcp(5432));
        int hostPort = Integer.parseInt(bindings[0].getHostPortSpec());

        JDBC_URL = "jdbc:postgresql://localhost:" + hostPort + "/" + PG_DB;
        awaitPostgres(JDBC_URL);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { DOCKER.stopContainerCmd(CONTAINER_ID).withTimeout(10).exec(); } catch (Exception ignored) {}
            try { DOCKER.removeContainerCmd(CONTAINER_ID).withRemoveVolumes(true).exec(); } catch (Exception ignored) {}
        }));
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",                () -> JDBC_URL);
        r.add("spring.datasource.username",           () -> PG_USER);
        r.add("spring.datasource.password",           () -> PG_PASSWORD);
        r.add("spring.datasource.driver-class-name",  () -> "org.postgresql.Driver");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static void pullIfAbsent(String image, String tag) {
        String full = image + ":" + tag;
        try {
            DOCKER.inspectImageCmd(full).exec();
        } catch (Exception e) {
            try {
                DOCKER.pullImageCmd(image).withTag(tag)
                        .exec(new ResultCallback.Adapter<>())
                        .awaitCompletion(5, TimeUnit.MINUTES);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Image pull interrupted", ie);
            }
        }
    }

    private static void awaitPostgres(String url) {
        for (int i = 0; i < 60; i++) {
            try (Connection c = DriverManager.getConnection(url, PG_USER, PG_PASSWORD)) {
                return;
            } catch (SQLException ignored) {}
            try { Thread.sleep(1000); } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(ie);
            }
        }
        throw new RuntimeException("PostgreSQL container did not become ready in 60 s");
    }

    // ── Injections Spring ────────────────────────────────────────────────────

    @Autowired private DataSource dataSource;
    @Autowired private Flyway     flyway;

    // ── Tests : complétude des migrations ────────────────────────────────────

    @Test
    void allSixMigrationsShouldBeAppliedWithSuccess() {
        MigrationInfo[] applied = flyway.info().applied();
        assertThat(applied)
                .hasSize(6)
                .extracting(MigrationInfo::getState)
                .allMatch(state -> state == MigrationState.SUCCESS);
    }

    @Test
    void migrationVersionsShouldFollowSequence() {
        MigrationInfo[] applied = flyway.info().applied();
        assertThat(applied)
                .extracting(info -> info.getVersion().toString())
                .containsExactly("1", "2", "3", "4", "5", "6");
    }

    // ── V1 ───────────────────────────────────────────────────────────────────

    @Test
    void v1_usersTableShouldExistWithRequiredColumns() throws SQLException {
        assertTableExists("users");
        assertColumnExists("users", "id");
        assertColumnExists("users", "username");
        assertColumnExists("users", "password");
        assertColumnExists("users", "role");
    }

    // ── V3 ───────────────────────────────────────────────────────────────────

    @Test
    void v3_normesAndAllLookupTablesShouldExist() throws SQLException {
        for (String t : new String[]{
                "normes","statuts","document_types","collections",
                "industrial_branches","product_families","sub_families",
                "filters1","ics_level1","ics_level2","ics_level3"}) {
            assertTableExists(t);
        }
    }

    @Test
    void v3_normeIndexesShouldExist() throws SQLException {
        for (String idx : new String[]{
                "idx_normes_reference","idx_normes_statut",
                "idx_normes_ics1","idx_normes_ics2","idx_normes_ics3"}) {
            assertIndexExists(idx);
        }
    }

    // ── V4 ───────────────────────────────────────────────────────────────────

    @Test
    void v4_pdfColumnsShouldBeAddedToNormes() throws SQLException {
        assertColumnExists("normes", "pdf_path");
        assertColumnExists("normes", "pdf_original_name");
        assertColumnExists("normes", "pdf_content_type");
        assertColumnExists("normes", "pdf_size");
    }

    // ── V5 ───────────────────────────────────────────────────────────────────

    @Test
    void v5_subscriptionTablesShouldExist() throws SQLException {
        for (String t : new String[]{"plans_abonnement","abonnements","paiements","consultations"}) {
            assertTableExists(t);
        }
    }

    @Test
    void v5_subscriptionIndexesShouldExist() throws SQLException {
        for (String idx : new String[]{
                "idx_abonnements_user_id","idx_abonnements_statut","idx_abonnements_date_fin",
                "idx_paiements_abonnement_id","idx_paiements_user_id",
                "idx_consultations_user_id","idx_consultations_norme_id"}) {
            assertIndexExists(idx);
        }
    }

    // ── V6 ───────────────────────────────────────────────────────────────────

    @Test
    void v6_standardsAndCategoriesTablesShouldBeDropped() throws SQLException {
        assertTableNotExists("standards");
        assertTableNotExists("categories");
    }

    // ── Intégrité FK ─────────────────────────────────────────────────────────

    @Test
    @Transactional
    void fkConstraint_abonnementShouldReferencePlanAndUser() throws SQLException {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO users(username,password,role) VALUES('tc_u1','pwd','USER')");
            stmt.execute("INSERT INTO plans_abonnement(nom,prix,duree_mois,illimite,created_at,updated_at) VALUES('TC-Plan',9.99,1,false,NOW(),NOW())");
            stmt.execute("""
                INSERT INTO abonnements(user_id,plan_id,date_debut,date_fin,statut,created_at,updated_at)
                VALUES(
                  (SELECT id FROM users WHERE username='tc_u1'),
                  (SELECT id FROM plans_abonnement WHERE nom='TC-Plan'),
                  CURRENT_DATE, CURRENT_DATE+INTERVAL '1 month','PENDING',NOW(),NOW())
                """);
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM abonnements WHERE user_id=(SELECT id FROM users WHERE username='tc_u1')");
            rs.next();
            assertThat(rs.getInt(1)).isEqualTo(1);
        }
    }

    @Test
    @Transactional
    void fkConstraint_consultationShouldReferenceNormeAndUser() throws SQLException {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO users(username,password,role) VALUES('tc_u2','pwd','USER')");
            stmt.execute("INSERT INTO normes(reference,included_in_subscription,mandatory,created_at,updated_at) VALUES('TC-ISO-999',false,false,NOW(),NOW())");
            stmt.execute("""
                INSERT INTO consultations(user_id,norme_id,date_consultation)
                VALUES(
                  (SELECT id FROM users WHERE username='tc_u2'),
                  (SELECT id FROM normes WHERE reference='TC-ISO-999'),
                  NOW())
                """);
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM consultations WHERE user_id=(SELECT id FROM users WHERE username='tc_u2')");
            rs.next();
            assertThat(rs.getInt(1)).isEqualTo(1);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void assertTableExists(String tableName) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            ResultSet rs = conn.getMetaData().getTables(null, "public", tableName, new String[]{"TABLE"});
            assertThat(rs.next()).as("Table '%s' devrait exister", tableName).isTrue();
        }
    }

    private void assertTableNotExists(String tableName) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            ResultSet rs = conn.getMetaData().getTables(null, "public", tableName, new String[]{"TABLE"});
            assertThat(rs.next()).as("Table '%s' ne devrait plus exister après V6", tableName).isFalse();
        }
    }

    private void assertColumnExists(String tableName, String columnName) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            ResultSet rs = conn.getMetaData().getColumns(null, "public", tableName, columnName);
            assertThat(rs.next()).as("Colonne '%s.%s' devrait exister", tableName, columnName).isTrue();
        }
    }

    private void assertIndexExists(String indexName) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT 1 FROM pg_indexes WHERE schemaname='public' AND indexname=?")) {
            ps.setString(1, indexName);
            ResultSet rs = ps.executeQuery();
            assertThat(rs.next()).as("Index '%s' devrait exister", indexName).isTrue();
        }
    }
}
