package pages;

import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class LoginPage {

    // Ссылка Login в шапке сайта
    private final SelenideElement loginMenuLink = $(Selectors.byText("Login"));

    // Локаторы формы
    private final SelenideElement usernameField = $("#username");
    private final SelenideElement passwordField = $("#password");

    // СТАБИЛЬНЫЙ ЛОКАТОР: Ищет кнопку по типу сабмита формы, игнорируя язык интерфейса
    private final SelenideElement submitButton = $("button[type='submit']");

    public void openPage() {
        loginMenuLink.click();
    }

    public void login(String username, String password) {
        usernameField.setValue(username);
        passwordField.setValue(password);
        submitButton.click();
    }

    public void checkErrorMessageNotVisible() {
        $(".error-message").shouldNotBe(com.codeborne.selenide.Condition.visible);
    }
}