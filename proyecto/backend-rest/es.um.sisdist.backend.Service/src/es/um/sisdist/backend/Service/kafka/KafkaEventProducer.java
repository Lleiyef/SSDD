package es.um.sisdist.backend.Service.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Optional;
import java.util.Properties;
import java.util.logging.Logger;

public class KafkaEventProducer {

    private static final Logger logger = Logger.getLogger(KafkaEventProducer.class.getName());
    private static final String TOPIC = "prompt-sessions";

    private static KafkaEventProducer instance = new KafkaEventProducer();

    private final KafkaProducer<String, String> producer;
    private final boolean enabled;

    private KafkaEventProducer() {
        String bootstrap = Optional.ofNullable(System.getenv("KAFKA_BOOTSTRAP_SERVERS"))
                                   .orElse("kafka:9092");
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "1");
        props.put(ProducerConfig.RETRIES_CONFIG, "2");
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, "3000");
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, "3000");

        KafkaProducer<String, String> p = null;
        boolean ok = false;
        try {
            p = new KafkaProducer<>(props);
            ok = true;
        } catch (Exception e) {
            logger.warning("Kafka no disponible, eventos desactivados: " + e.getMessage());
        }
        this.producer = p;
        this.enabled = ok;
    }

    public static KafkaEventProducer getInstance() {
        return instance;
    }

    public void send(String dialogueId, String type, String payload) {
        if (!enabled || producer == null) return;
        String value = String.format(
            "{\"dialogue_id\":\"%s\",\"type\":\"%s\",\"payload\":%s,\"ts\":%d}",
            dialogueId, type, payload, System.currentTimeMillis());
        try {
            producer.send(new ProducerRecord<>(TOPIC, dialogueId, value));
        } catch (Exception e) {
            logger.warning("Error enviando evento Kafka: " + e.getMessage());
        }
    }
}
