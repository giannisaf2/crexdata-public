import os
import sys
import time
import json
import logging

from confluent_kafka import Producer

current = os.path.dirname(os.path.realpath(__file__))
parent = os.path.dirname(current)
sys.path.append(parent)
from config import KAFKA_CONFIG, KAFKA_CRAWL_LOGS_TOPIC


class KafkaHandler(logging.Handler):
    """Class to instantiate the kafka logging facility."""

    def __init__(self, tls=None):
        """Initialize an instance of the kafka handler."""
        logging.Handler.__init__(self)
        self.producer = Producer(**KAFKA_CONFIG)
        self.topic = KAFKA_CRAWL_LOGS_TOPIC

    def emit(self, record):
        """Emit the provided record to the kafka_client producer."""
        # drop kafka logging to avoid infinite recursion
        if 'kafka.' in record.name:
            return

        try:
            # apply the logger formatter
            msg = self.format(record)
            self.producer.produce(self.topic, msg) #json.dumps({'message': msg})
            self.flush(timeout=1.0)
        except Exception:
            logging.Handler.handleError(self, record)

    def flush(self, timeout=None):
        """Flush the objects."""
        self.producer.flush(timeout=timeout)

    def close(self):
        """Close the producer and clean up."""
        self.acquire()
        try:
            self.producer.flush(timeout=1.0)
            logging.Handler.close(self)
        finally:
            self.release()
            

def setup_logging(name="logger_none", level=logging.DEBUG):
    logger = logging.getLogger(name)
    if not logger.handlers:
        logger.setLevel(level)
        kafka_handler = KafkaHandler()
        kafka_handler.setLevel(level)
        formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
        kafka_handler.setFormatter(formatter)
        logger.addHandler(kafka_handler)
        logger.propagate = False 
    return logger
            
    