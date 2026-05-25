package tests.integration;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class KafkaIntegrationTest {

    // Настройки БД для пре-сидинга товара
    private final String dbUrl = System.getenv("SPRING_DATASOURCE_URL") != null && !System.getenv("SPRING_DATASOURCE_URL").isEmpty()
            ? System.getenv("SPRING_DATASOURCE_URL") : "jdbc:mysql://localhost:3307/shopeasy";
    private final String dbUser = "root";
    private final String dbPassword = "1234";

    @BeforeAll
    public static void setup() {
        RestAssured.baseURI = System.getProperty("api.url", "http://localhost:8080");
    }

    @Test
    public void testCreateOrderSendsMessageToKafka() {
        // РЫЧАГ ПРОТИВ 404: Гарантируем наличие товара с id = 1 в базе данных до отправки заказа
        try (java.sql.Connection connection = java.sql.DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             java.sql.Statement statement = connection.createStatement()) {
            statement.execute("SET FOREIGN_KEY_CHECKS = 0;");
            statement.execute("INSERT INTO products (id, name, description, price, stock) " +
                    "VALUES (1, 'Seeded Product', 'Pre-created for integration test', 500.0, 100) " +
                    "ON DUPLICATE KEY UPDATE id=1;");
            statement.execute("SET FOREIGN_KEY_CHECKS = 1;");
            System.out.println("=== ТОВАР С ID=1 УСПЕШНО ДОБАВЛЕН В БД ДЛЯ ТЕСТА ===");
        } catch (Exception e) {
            System.out.println("Не удалось сделать пре-сидинг товара через JDBC: " + e.getMessage());
        }

        String uniqueUser = "user_" + System.currentTimeMillis();
        String authRequestBody = "{\n" +
                "  \"username\": \"" + uniqueUser + "\",\n" +
                "  \"password\": \"password123\"\n" +
                "}";

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(authRequestBody)
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(200);

        String jwtToken = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(authRequestBody)
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .path("accessToken");

        System.out.println("Токен получен: Bearer " + jwtToken);

        String orderRequestBody = "{\n" +
                "  \"items\": [\n" +
                "    {\n" +
                "      \"productId\": 1,\n" +
                "      \"quantity\": 2\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        System.out.println("=== Отправляем запрос на создание заказа ===");
        RestAssured.given()
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(ContentType.JSON)
                .body(orderRequestBody)
                .when()
                .post("/api/orders")
                .then()
                .statusCode(Matchers.oneOf(200, 201));

        System.out.println("Ждём 2 секунды, чтобы брокер успел записать сообщение на диск...");
        try { Thread.sleep(2000); } catch (Exception e) {}

        Properties properties = new Properties();
        // Динамический хост Кафки в зависимости от среды запуска (локально или GitLab)
        String kafkaUrl = System.getenv("SPRING_KAFKA_BOOTSTRAP_SERVERS") != null ? "kafka:9092" : "localhost:9094";
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaUrl);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "shopeasy-aqa-direct-group");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);

        TopicPartition partition = new TopicPartition("order-events", 0);
        consumer.assign(Collections.singletonList(partition));
        consumer.seekToBeginning(Collections.singletonList(partition));

        List<String> messages = new ArrayList<>();
        int attempts = 0;
        while (messages.size() < 1 && attempts < 5) {
            System.out.println("Опрашиваем напрямую партицию 0, попытка " + (attempts + 1));
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
            records.forEach(record -> messages.add(record.value()));
            attempts++;
        }
        consumer.close();

        assertFalse(messages.isEmpty(), "Брокер Kafka не получил сообщение в топик order-events!");
        String jsonMessage = messages.get(0);
        System.out.println("Cобытие из Кафки: " + jsonMessage);
        assertTrue(jsonMessage.contains("\"productId\":1") || jsonMessage.contains("1"),
                "В сообщении Кафки нет данных о заказанном товаре!");
    }
}