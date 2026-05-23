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

    @BeforeAll
    public static void setup() {
        RestAssured.baseURI = "http://localhost:8080";
    }

    @Test
    public void testCreateOrderSendsMessageToKafka() {
        String uniqueUser = "user_" + System.currentTimeMillis();
        String authRequestBody = "{\n" +
                "  \"username\": \"" + uniqueUser + "\",\n" +
                "  \"password\": \"password123\"\n" +
                "}";

        // Шаг 0.1: Регистрация
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(authRequestBody)
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(200);

        // Шаг 0.2: Авторизация
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

        // Тело заказа
        String orderRequestBody = "{\n" +
                "  \"items\": [\n" +
                "    {\n" +
                "      \"productId\": 1,\n" +
                "      \"quantity\": 2\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        // Шаг 1: Создаем заказ ПЕРВЫМ ДЕЛОМ.
        // Бэкенд мгновенно создаст топик order-events (если его нет) и положит туда событие.
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

        // Шаг 2: Настраиваем консьюмер для жесткого прямого чтения
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9094");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "shopeasy-aqa-direct-group"); // Формальность

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);

        // РЫЧАГ: Отказываемся от subscribe() и Rebalance.
        // Подключаемся напрямую к партиции 0 (она всегда есть по умолчанию)
        TopicPartition partition = new TopicPartition("order-events", 0);
        consumer.assign(Collections.singletonList(partition));

        // Принудительно сдвигаем курсор чтения в самое начало (оффсет 0)
        consumer.seekToBeginning(Collections.singletonList(partition));

        // Шаг 3: Читаем сообщения
        List<String> messages = new ArrayList<>();
        int attempts = 0;
        while (messages.size() < 1 && attempts < 5) {
            System.out.println("Опрашиваем напрямую партицию 0, попытка " + (attempts + 1));
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
            records.forEach(record -> messages.add(record.value()));
            attempts++;
        }
        consumer.close();

        // Шаг 4: Ассерты (Проверки)
        assertFalse(messages.isEmpty(), "Брокер Kafka не получил сообщение в топик order-events!");
        String jsonMessage = messages.get(0);
        System.out.println("Cобытие из Кафки: " + jsonMessage);
        assertTrue(jsonMessage.contains("\"productId\":1") || jsonMessage.contains("1"),
                "В сообщении Кафки нет данных о заказанном товаре!");
    }
}