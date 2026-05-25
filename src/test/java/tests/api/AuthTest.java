package tests.api;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AuthTest {

    private RequestSpecBuilder requestSpec;

    // Читаем API URL из параметров Maven или используем локалхост
    private final String apiUrl = System.getProperty("api.url", "http://localhost:8080");

    @BeforeEach
    void setUp() {
        // Гарантируем переопределение URI перед КАЖДЫМ запросом
        RestAssured.baseURI = System.getProperty("api.url", "http://localhost:8080");

        String uniqueUser = "ryce_aqa_" + System.currentTimeMillis();
        String loginBody = "{\"username\": \"" + uniqueUser + "\", \"password\": \"password123\"}";

        // Регистрируем
        RestAssured.given()
                .contentType("application/json")
                .body(loginBody)
                .post("/api/auth/register");

        // Логинимся
        Response response = RestAssured.given()
                .contentType("application/json")
                .body(loginBody)
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .extract().response();

        String token = response.path("accessToken");

        // Пересоздаем спецификацию с актуальным базовым URI напрямую
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