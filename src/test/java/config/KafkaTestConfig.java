package config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.util.Properties;

public class KafkaTestConfig {

    public static Properties getConsumerProperties() {
        Properties properties = new Properties();

        // Направляем тест на выделенный внешний порт 9094
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9094");

        // Десериализаторы (перевод байтов в текст)
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        // РЫЧАГ: Каждому запуску теста — чистая группа, чтобы читать топик с нуля
        String uniqueGroupId = "shopeasy-aqa-group-" + System.currentTimeMillis();
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, uniqueGroupId);

        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "shopeasy-aqa-integration-group");

        return properties;
    }
}