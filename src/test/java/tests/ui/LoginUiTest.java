package tests.ui;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.WebDriverConditions;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pages.LoginPage;

import static com.codeborne.selenide.Selenide.open;
import static com.codeborne.selenide.Selenide.webdriver;

public class LoginUiTest {

    private final LoginPage loginPage = new LoginPage();

    @BeforeAll
    static void setUp() {
        // Берем baseUrl фронтенда из параметров сборки (в CI это http://frontend, локально - http://localhost)
        Configuration.baseUrl = System.getProperty("selenide.baseUrl", "http://frontend");
        Configuration.remote = System.getProperty("selenide.remote");
        Configuration.browser = System.getProperty("selenide.browser", "chrome");

        // КЛЮЧЕВЫЕ НАСТРОЙКИ ДЛЯ CI-СРЕДЫ
        Configuration.headless = true;
        Configuration.holdBrowserOpen = false;
        Configuration.timeout = 6000; // Немного увеличим таймаут ожидания элементов для облака

        // ПРЕ-РЕГИСТРАЦИЯ: Создаем пользователя для UI-теста через API до старта браузера
        String apiUrl = System.getProperty("api.url", "http://backend:8080");
        String loginBody = "{\"username\": \"ryce_test_automation\", \"password\": \"password123\"}";

        try {
            RestAssured.given()
                    .baseUri(apiUrl)
                    .contentType("application/json")
                    .body(loginBody)
                    .post("/api/auth/register");
        } catch (Exception e) {
            System.out.println("Пре-регистрация UI-пользователя завершилась (возможно, он уже создан): " + e.getMessage());
        }
    }

    @Test
    void testSuccessfulLoginWithPageObject() {
        // Открываем главную страницу относительно Configuration.baseUrl
        open("/");

        loginPage.openPage();

        // Входим под гарантированно существующим аккаунтом
        loginPage.login("ryce_test_automation", "password123");
        loginPage.checkErrorMessageNotVisible();

        // Проверяем относительный путь, чтобы тест не падал из-за разницы хостов (frontend vs localhost)
        webdriver().shouldHave(WebDriverConditions.urlContaining("/products"), java.time.Duration.ofSeconds(10));
    }
}