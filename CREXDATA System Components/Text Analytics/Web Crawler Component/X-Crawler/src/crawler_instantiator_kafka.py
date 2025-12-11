import os
import sys
import json
import signal

from confluent_kafka import Consumer

current = os.path.dirname(os.path.realpath(__file__))
parent = os.path.dirname(current)
sys.path.append(parent)
from sub.common_logger import setup_logging
from crawler_manager import CrawlerManager
from config import KAFKA_CONFIG, KAFKA_COMMANDS_TOPIC 


logger = setup_logging(name=os.path.splitext(os.path.basename(__file__))[0])

conf = {
    'group.id': 'crawler-group',
    'auto.offset.reset': 'latest',
}

conf.update(KAFKA_CONFIG)

consumer = Consumer(conf)
consumer.subscribe([KAFKA_COMMANDS_TOPIC])
manager = CrawlerManager()

current_running_crawls = []


# Graceful shutdown
def graceful_shutdown(signum, frame):
    logger.info("Cleaning up in crawler_instantiator_kafka.py")
    print("Cleaning up in crawler_instantiator_kafka.py")
    try:
        for id in current_running_crawls:
            manager.kill_process(id)          
        consumer.close()
    except:
        pass
    finally:
        logger.info("Cleaning up done in crawler_instantiator_kafka.py, Exiting....")
        print("Cleaning up done in crawler_instantiator_kafka.py, Exiting....")
        sys.exit(0)

signal.signal(signal.SIGTERM, graceful_shutdown)



try:
    print("Crawler consumer started.")
    logger.info("Crawler consumer started.")
    while True:
        msg = consumer.poll(1.0)
        if msg is None:
            continue
        if msg.error():
            print(f"Kafka error: {msg.error()}")
            logger.exception(f"Kafka error: {msg.error()}")
            continue

        try:
            command_data = json.loads(msg.value().decode('utf-8'))
            crawl_id = command_data["crawl_id"]
            command = command_data["command"]

            if command == "start":
                manager.start_process(crawl_id, command_data)
                current_running_crawls.append(crawl_id)
            elif command == "kill":
                manager.kill_process(crawl_id)
                current_running_crawls.remove(crawl_id)
        except Exception as e:
            print(f"Error processing message: {e}")
            logger.exception(f"Error processing kafka message: {e}")
            
except KeyboardInterrupt: 
    # kills all running crawls if consumer program interrupted wiith CTRL-C
    for id in current_running_crawls:
        manager.kill_process(id)          
    consumer.close()
