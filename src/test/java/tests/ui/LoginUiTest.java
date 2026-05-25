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
        Configuration.baseUrl = System.getProperty("selenide.baseUrl", "http://localhost");
        Configuration.remote = System.getProperty("selenide.remote");
        Configuration.browser = System.getProperty("selenide.browser", "chrome");
        Configuration.headless = true; // Для CI-среды
        Configuration.holdBrowserOpen = false;

        // Регистрируем стабильного UI-пользователя перед тестом, если его ещё нет
        String apiUrl = System.getProperty("api.url", "http://localhost:8080");
        String loginBody = "{\"username\": \"ryce_ui_user\", \"password\": \"password123\"}";

        try {
            RestAssured.given()
                    .baseUri(apiUrl)
                    .contentType("application/json")
                    .body(loginBody)
                    .post("/api/auth/register");
        } catch (Exception e) {
            System.out.println("Не удалось отправить запрос на предустановку UI-пользователя: " + e.getMessage());
        }
    }

    @Test
    void testSuccessfulLoginWithPageObject() {
        open("/");

        loginPage.openPage();
        // Входим под свежесозданным пользователем
        loginPage.login("ryce_ui_user", "password123");

        webdriver().shouldHave(WebDriverConditions.urlContaining("/products"));
    }
}