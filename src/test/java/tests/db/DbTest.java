package tests.db;

import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DbTest {

    @Test
    void testDatabaseConnection() {
        // Динамическое чтение URL: если мы в GitLab CI — берем из переменных, если локально — берем localhost:3307
        String envUrl = System.getenv("SPRING_DATASOURCE_URL");
        String url = (envUrl != null && !envUrl.isEmpty()) ? envUrl : "jdbc:mysql://localhost:3307/shopeasy";

        String user = "root";
        String password = "1234";

        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            System.out.println("========================================");
            System.out.println("УСПЕШНО ПОДКЛЮЧИЛИСЬ К MYSQL!");
            System.out.println("========================================");

            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SHOW TABLES");

            System.out.println("Список таблиц в базе данных:");
            while (resultSet.next()) {
                System.out.println("- " + resultSet.getString(1));
            }
            System.out.println("========================================");

        } catch (Exception e) {
            System.out.println("НЕ УДАЛОСЬ ПОДКЛЮЧИТЬСЯ К БАЗЕ ПО АДРЕСУ: " + url);
            e.printStackTrace();
        }
    }
}