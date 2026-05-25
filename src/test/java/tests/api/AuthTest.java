package tests.api;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// ДОБАВЛЕННЫЕ ИМПОРТЫ ДЛЯ СВЯЗИ С БД
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class AuthTest {

    private RequestSpecBuilder requestSpec;

    // ДОБАВЛЕННЫЕ ПЕРЕМЕННЫЕ ДЛЯ ПОДКЛЮЧЕНИЯ К БД
    private final String dbUrl = getDbUrl();
    private final String dbUser = "root";
    private final String dbPassword = "1234";

    private String getDbUrl() {
        String envUrl = System.getenv("SPRING_DATASOURCE_URL");
        return (envUrl != null && !envUrl.isEmpty()) ? envUrl : "jdbc:mysql://localhost:3307/shopeasy";
    }

    @BeforeEach
    void setUp() throws Exception {
        // Гарантируем переопределение URI перед КАЖДЫМ запросом
        RestAssured.baseURI = System.getProperty("api.url", "http://localhost:8080");

        String uniqueUser = "ryce_admin_" + System.currentTimeMillis();
        String loginBody = "{\"username\": \"" + uniqueUser + "\", \"password\": \"password123\"}";

        // 1. Регистрируем пользователя через обычный API
        RestAssured.given()
                .contentType("application/json")
                .body(loginBody)
                .post("/api/auth/register")
                .then()
                .statusCode(200);

        // 2. РЫЧАГ: Напрямую через JDBC выставляем пользователю роль ADMIN в БД
        try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             Statement statement = connection.createStatement()) {
            String updateSql = "UPDATE users SET role = 'ADMIN' WHERE username = '" + uniqueUser + "'";
            statement.executeUpdate(updateSql);
        }

        // 3. Логинимся уже под пользователем, который стал ADMIN
        Response response = RestAssured.given()
                .contentType("application/json")
                .body(loginBody)
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .extract().response();

        String token = response.path("accessToken");

        // Пересоздаем спецификацию с актуальным токеном администратора
        requestSpec = new RequestSpecBuilder()
                .setBaseUri(RestAssured.baseURI)
                .setContentType("application/json")
                .addHeader("Authorization", "Bearer " + token);
    }

    @Test
    void testAccessToProtectedEndpoint() {
        RestAssured.given()
                .spec(requestSpec.build())
                .when()
                .post("/api/users")
                .then()
                .statusCode(400);
    }
}