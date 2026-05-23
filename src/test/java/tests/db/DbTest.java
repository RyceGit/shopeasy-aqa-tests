package tests.db;

import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DbTest {

    @Test
    void testDatabaseConnection() {
        // Меняем порт на 3307 и пароль на 1234 строго по докер-файлу
        String url = "jdbc:mysql://localhost:3307/shopeasy";
        String user = "root";
        String password = "1234";

        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            System.out.println("========================================");
            System.out.println("УСПЕШНО ПОДКЛЮЧИЛИСЬ К MYSQL В ДОКЕРЕ!");
            System.out.println("========================================");

            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SHOW TABLES");

            System.out.println("Список таблиц в базе данных:");
            while (resultSet.next()) {
                System.out.println("- " + resultSet.getString(1));
            }
            System.out.println("========================================");

        } catch (Exception e) {
            System.out.println("НЕ УДАЛОСЬ ПОДКЛЮЧИТЬСЯ!");
            e.printStackTrace();
        }
    }
}