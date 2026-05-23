package tests.ui;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.WebDriverConditions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pages.LoginPage;

import static com.codeborne.selenide.Selenide.open;
import static com.codeborne.selenide.Selenide.webdriver;

public class LoginUiTest {

    private final LoginPage loginPage = new LoginPage();

    @BeforeAll
    static void setUp() {
        Configuration.baseUrl = "http://localhost";
        Configuration.headless = false;
        Configuration.holdBrowserOpen = false;
    }

    @Test
    void testSuccessfulLoginWithPageObject() {
        open("/");

        loginPage.openPage();
        loginPage.login("ryce_test_automation", "password123");

        webdriver().shouldHave(WebDriverConditions.url("http://localhost/products"));
    }
}