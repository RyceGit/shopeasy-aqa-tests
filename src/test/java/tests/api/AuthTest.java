package tests.api;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AuthTest {

    // Сюда мы сохраним готовый конфиг с токеном внутри
    private static RequestSpecBuilder requestSpec;

    @BeforeEach
    void setUp() {
        String loginBody = """
                {
                  "username": "ryce_test_automation",
                  "password": "password123"
                }
                """;

        // 1. Стучимся за токеном
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

        // 2. Рычаг этапа: Настраиваем шаблон запроса, который АВТОМАТИЧЕСКИ
        // будет добавлять заголовок "Authorization: Bearer <токен>" ко всем тестам
        requestSpec = new RequestSpecBuilder()
                .setBaseUri("http://localhost:8080")
                .setContentType("application/json")
                .addHeader("Authorization", "Bearer " + token);
    }

    @Test
    void testAccessToProtectedEndpoint() {
        // Теперь нам не нужно прописывать baseUri, contentType и токен вручную!
        // Мы просто передаем нашу готовую спецификацию .spec(requestSpec.build())
        RestAssured.given()
                .spec(requestSpec.build())
                .when()
                .post("/api/users") // Тот самый эндпоинт создания юзеров, который раньше давал нам 403 Forbidden
                .then()
                // Если сервер ответит 400 (Bad Request) вместо 403 (Forbidden),
                // значит токен сработал, нас пустили внутрь "кабинета", но мы просто послали пустое тело!
                .statusCode(400);
    }
}