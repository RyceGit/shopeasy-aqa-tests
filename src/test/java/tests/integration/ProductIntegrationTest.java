package tests.integration;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProductIntegrationTest {

    private static RequestSpecBuilder requestSpec;

    // Читаем URL из окружения (GitLab CI) или падаем на локалхост
    private final String dbUrl = getDbUrl();
    private final String dbUser = "root";
    private final String dbPassword = "1234";

    // Читаем API URL из параметров Maven или используем локалхост
    private final String apiUrl = System.getProperty("api.url", "http://localhost:8080");

    private String getDbUrl() {
        String envUrl = System.getenv("SPRING_DATASOURCE_URL");
        return (envUrl != null && !envUrl.isEmpty()) ? envUrl : "jdbc:mysql://localhost:3307/shopeasy";
    }

    @BeforeEach
    void setUp() {
        String uniqueUser = "ryce_aqa_" + System.currentTimeMillis();
        String loginBody = "{\"username\": \"" + uniqueUser + "\", \"password\": \"password123\"}";

        // ШАГ 0: Создаем юзера (игнорируем результат, так как он может уже существовать при локальном перезапуске)
        RestAssured.given()
                .baseUri(apiUrl)
                .contentType("application/json")
                .body(loginBody)
                .post("/api/auth/register");

        // ШАГ 1: Логинимся
        Response response = RestAssured.given()
                .baseUri(apiUrl)
                .contentType("application/json")
                .body(loginBody)
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(200) // Теперь сервер ответит 200, потому что юзер существует
                .extract().response();
        // ... (остальной код)

        String token = response.path("accessToken");

        requestSpec = new RequestSpecBuilder()
                .setBaseUri(apiUrl)
                .setContentType("application/json") // <--- Исправлено здесь
                .addHeader("Authorization", "Bearer " + token);
    }

    @Test
    void testCreateProductViaApiAndVerifyInDb() throws Exception {
        String uniqueProductName = "Ryce Shirt " + System.currentTimeMillis();

        String productJson = """
                {
                  "name": "%s",
                  "description": "Тестовая футболка от автотеста",
                  "price": 1500.0,
                  "stock": 50
                }
                """.formatted(uniqueProductName);

        RestAssured.given()
                .spec(requestSpec.build())
                .body(productJson)
                .when()
                .post("/api/products")
                .then()
                .statusCode(200);

        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            Statement statement = connection.createStatement();

            String sqlQuery = "SELECT * FROM products WHERE name = '" + uniqueProductName + "'";
            ResultSet resultSet = statement.executeQuery(sqlQuery);

            assertTrue(resultSet.next(), "Товар не был найден в базе данных!");

            String actualDescription = resultSet.getString("description");
            double actualPrice = resultSet.getDouble("price");
            int actualStock = resultSet.getInt("stock");

            assertEquals("Тестовая футболка от автотеста", actualDescription);
            assertEquals(1500.0, actualPrice);
            assertEquals(50, actualStock);

            System.out.println("========================================");
            System.out.println("ИНТЕГРАЦИОННЫЙ ТЕСТ ПРОШЕЛ УСПЕШНО!");
            System.out.println("Товар найден в БД с корректными данными.");
            System.out.println("========================================");
        }
    }
}