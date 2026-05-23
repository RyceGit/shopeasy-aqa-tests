package consumer;

import config.KafkaTestConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.time.Duration;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

public class KafkaTestConsumer {

    public static List<String> consumeLatestMessages(String topicName, int expectedCount) {
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(KafkaTestConfig.getConsumerProperties());
        consumer.subscribe(Collections.singletonList(topicName));

        List<String> messages = new ArrayList<>();
        int attempts = 0;
        int maxAttempts = 5; // Увеличили количество попыток до 5

        System.out.println("=== Начало опроса топика: " + topicName + " ===");

        while (messages.size() < expectedCount && attempts < maxAttempts) {
            System.out.println("Попытка " + (attempts + 1) + ": опрашиваем Kafka...");
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2)); // Ожидаем пакет до 2 секунд

            for (ConsumerRecord<String, String> record : records) {
                messages.add(record.value());
            }
            attempts++;
        }

        System.out.println("=== Опрос завершен. Найдено сообщений: " + messages.size() + " ===");
        consumer.close();

        return messages;
    }

}