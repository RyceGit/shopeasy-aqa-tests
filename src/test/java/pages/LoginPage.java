package pages;

import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class LoginPage {

    // 1. Добавляем локатор для ссылки Login в шапке сайта
    private final SelenideElement loginMenuLink = $(Selectors.byText("Login"));

    // Локаторы формы
    private final SelenideElement usernameField = $("#username");
    private final SelenideElement passwordField = $("#password");
    private final SelenideElement submitButton = $(Selectors.byText("Entrar"));

    // 2. Метод открытия страницы теперь кликает по меню, а не ломает Nginx прямым урлом
    public void openPage() {
        loginMenuLink.click();
    }

    public void login(String username, String password) {
        usernameField.setValue(username);
        passwordField.setValue(password);
        submitButton.click();
    }
    public void checkErrorMessageNotVisible() {
    }
}