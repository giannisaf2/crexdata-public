import os
import sys
import json
import argparse
from confluent_kafka import Producer

current = os.path.dirname(os.path.realpath(__file__))
parent = os.path.dirname(current)
sys.path.append(parent)
from config import KAFKA_CONFIG

TEST_KAFKA_COMMANDS_TOPIC = "test_command_topic"


parser = argparse.ArgumentParser(description="Script to scrape tweets")
parser.add_argument("--id", type=str, required=True, help="Test crawl id")
parser.add_argument("--case", type=str, required=True, help="Option kill or start")

args = parser.parse_args()

producer = Producer(**KAFKA_CONFIG)

msg_dict = {"crawl_id":args.id,  
            "command":args.case,  
            "crawl_params.username":"test-usrname",
            "crawl_params.password":"test-passwd",
            "crawl_params.email":"test-email",
            "crawl_params.emailpass":"test-emailpass",
            "crawl_params.lat":"41.3825",
            "crawl_params.lon":"2.176944",
            "crawl_params.radius":"100",
            "crawl_params.lang":"Spanish"}
try:
    producer.produce(TEST_KAFKA_COMMANDS_TOPIC, json.dumps(msg_dict)) 
except BufferError:
    print(f'Local producer queue is full ({len(producer)} messages awaiting delivery): try again\n')
    
producer.poll(0)
producer.flush() 