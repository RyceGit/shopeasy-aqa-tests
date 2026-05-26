package tests.ui;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.logevents.SelenideLogger; // Добавили импорт
import io.qameta.allure.selenide.AllureSelenide;     // Добавили импорт
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pages.LoginPage;

import static com.codeborne.selenide.Selenide.open;

public class LoginUiTest {

    private final LoginPage loginPage = new LoginPage();

    @BeforeAll
    static void setUp() {
        // 1. ПОДКЛЮЧАЕМ СКРИНШОТЫ ДЛЯ ALLURE
        SelenideLogger.addListener("AllureSelenide",
                new AllureSelenide()
                        .screenshots(true)
                        .savePageSource(true)
        );

        // 2. НАСТРОЙКИ СЕЛЕНИДА
        // Берем baseUrl фронтенда из параметров сборки (в CI это http://frontend, локально - http://localhost)
        Configuration.baseUrl = System.getProperty("selenide.baseUrl", "http://frontend");
        Configuration.remote = System.getProperty("selenide.remote");
        Configuration.browser = System.getProperty("selenide.browser", "chrome");

        // КЛЮЧЕВЫЕ НАСТРОЙКИ ДЛЯ CI-СРЕДЫ
        Configuration.headless = true;
        Configuration.holdBrowserOpen = false;
        Configuration.timeout = 6000; // Увеличим таймаут ожидания элементов для облака

        // 3. ПРЕ-РЕГИСТРАЦИЯ: Создаем пользователя для UI-теста через API до старта браузера
        String apiUrl = System.getProperty("api.url", "http://backend:8080");
        String loginBody = "{\"username\": \"ryce_test_automation\", \"password\": \"password123\"}";

        try {
            RestAssured.given()
                    .baseUri(apiUrl)
                    .contentType("application/json")
                    .body(loginBody)
                    .post("/api/auth/register");
            System.out.println("DEBUG: Пользователь зарегистрирован через API");
        } catch (Exception e) {
            System.out.println("Пре-регистрация UI-пользователя завершилась (возможно, он уже создан): " + e.getMessage());
        }
    } // Скобка закрывает метод setUp. Больше никаких методов внутри него нет!

    @Test
    void testSuccessfulLoginWithPageObject() {
        open("/");
        // Ждем загрузки элементов
        loginPage.openPage();

        // Добавь принудительную паузу, чтобы Angular «увидел» клик
        com.codeborne.selenide.Selenide.sleep(2000);

        loginPage.login("ryce_test_automation", "password123");

        // ... проверка
    }
}