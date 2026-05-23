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

    // Настройки БД строго по нашему docker-compose
    private final String dbUrl = "jdbc:mysql://localhost:3307/shopeasy";
    private final String dbUser = "root";
    private final String dbPassword = "1234";

    @BeforeEach
    void setUp() {
        // Автоматически получаем токен перед тестом
        String loginBody = "{\"username\": \"ryce_test_automation\", \"password\": \"password123\"}";

        Response response = RestAssured.given()
                .baseUri("http://localhost:8080")
                .contentType("application/json")
                .body(loginBody)
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .extract().response();

        String token = response.path("accessToken");

        requestSpec = new RequestSpecBuilder()
                .setBaseUri("http://localhost:8080")
                .setContentType("application/json")
                .addHeader("Authorization", "Bearer " + token);
    }

    @Test
    void testCreateProductViaApiAndVerifyInDb() throws Exception {
        String uniqueProductName = "Ryce Shirt " + System.currentTimeMillis();

        // 1. Формируем тело JSON для создания товара строго по Swagger
        String productJson = """
                {
                  "name": "%s",
                  "description": "Тестовая футболка от автотеста",
                  "price": 1500.0,
                  "stock": 50
                }
                """.formatted(uniqueProductName);

        // 2. Отправляем POST запрос на создание товара через API
        RestAssured.given()
                .spec(requestSpec.build())
                .body(productJson)
                .when()
                .post("/api/products")
                .then()
                .statusCode(200); // Обычно для создания используется 201 Created (если упадет, проверим статус)

        // 3. Идем напрямую в базу данных проверять, записался ли товар
        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            Statement statement = connection.createStatement();

            // Пишем SQL запрос на поиск товара по нашему уникальному имени
            String sqlQuery = "SELECT * FROM products WHERE name = '" + uniqueProductName + "'";
            ResultSet resultSet = statement.executeQuery(sqlQuery);

            // Проверяем, что база вернула хотя бы одну строчку
            assertTrue(resultSet.next(), "Товар не был найден в базе данных!");

            // Вытаскиваем значения из колонок таблицы и сверяем с тем, что отправляли
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