import json
import os
import signal
import sys
from confluent_kafka import Consumer, KafkaException

BOOTSTRAP = os.environ.get("KAFKA_BOOTSTRAP_SERVERS", "kafka:9092")
TOPIC = "prompt-sessions"
LOG_FILE = os.environ.get("LOG_FILE", "/var/log/prompt-sessions.log")

conf = {
    "bootstrap.servers": BOOTSTRAP,
    "group.id": "log-service",
    "auto.offset.reset": "earliest",
    "enable.auto.commit": True,
}

consumer = Consumer(conf)
consumer.subscribe([TOPIC])

running = True


def shutdown(sig, frame):
    global running
    print("Shutting down log-service...", flush=True)
    running = False


signal.signal(signal.SIGINT, shutdown)
signal.signal(signal.SIGTERM, shutdown)

print(f"log-service arrancado. Escuchando topic '{TOPIC}' en {BOOTSTRAP}", flush=True)

with open(LOG_FILE, "a") as log:
    while running:
        msg = consumer.poll(timeout=1.0)
        if msg is None:
            continue
        if msg.error():
            print(f"Error Kafka: {msg.error()}", flush=True)
            continue
        try:
            event = json.loads(msg.value().decode("utf-8"))
            line = json.dumps(event, ensure_ascii=False)
            log.write(line + "\n")
            log.flush()
            print(f"[LOG] {line}", flush=True)
        except Exception as e:
            print(f"Error procesando mensaje: {e}", flush=True)

consumer.close()
print("log-service finalizado.", flush=True)
