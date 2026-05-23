package tests.api;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AuthTest {

    private static RequestSpecBuilder requestSpec;

    // Читаем API URL из параметров Maven или используем локалхост
    private final String apiUrl = System.getProperty("api.url", "http://localhost:8080");

    @BeforeEach
    void setUp() {
        String loginBody = """
                {
                  "username": "ryce_test_automation",
                  "password": "password123"
                }
                """;

        Response response = RestAssured.given()
                .baseUri(apiUrl)
                .contentType("application/json")
                .body(loginBody)
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .extract().response();

        String token = response.path("accessToken");

        requestSpec = new RequestSpecBuilder()
                .setBaseUri(apiUrl)
                .setContentType("application/json") // <--- Исправлено здесь
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