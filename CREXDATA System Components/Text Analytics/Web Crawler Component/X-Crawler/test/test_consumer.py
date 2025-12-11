import os
import sys
from confluent_kafka import Consumer, KafkaException

current = os.path.dirname(os.path.realpath(__file__))
parent = os.path.dirname(current)
sys.path.append(parent)
from config import KAFKA_CONFIG


TEST_KAFKA_COMMANDS_TOPIC = "test_command_topic"
    
conf = {
    'group.id': 'crawler-group',
    'auto.offset.reset': 'earliest',
}

conf.update(KAFKA_CONFIG)

consumer = Consumer(conf)
consumer.subscribe([TEST_KAFKA_COMMANDS_TOPIC])


try:
    print("Crawler consumer started.")
    while True:
        msg = consumer.poll(1.0)
        if msg is None:
            continue
        if msg.error():
            raise KafkaException(msg.error())
        else:
            print(msg.value())
            break
            
except KeyboardInterrupt:   
    consumer.close()
    
finally:
    consumer.close()
    
    
    
    
