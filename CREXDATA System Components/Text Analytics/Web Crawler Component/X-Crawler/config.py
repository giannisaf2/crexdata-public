import os

### KAFKA CONFIG - SASL_SSL
KAFKA_CONFIG = {
    'bootstrap.servers': os.getenv('KAFKA_BROKER'),
    'security.protocol': os.getenv('KAFKA_SECURITY_PROTOCOL'),
    'sasl.mechanism': os.getenv('KAFKA_SASL_MECHANISM'),
    'sasl.username': os.getenv('KAFKA_USERNAME'),
    'sasl.password': os.getenv('KAFKA_PASSWORD'),
    'ssl.certificate.location': './kafkaConnection/certificate.pem',
    'ssl.key.location': './kafkaConnection/key.pem',
    'ssl.ca.location': './kafkaConnection/CARoot.pem',
    }

### KAFKA CONFIG - NO AUTH
# KAFKA_CONFIG = {
#     'bootstrap.servers': os.getenv('KAFKA_BROKER'),
#     }

KAFKA_COMMANDS_TOPIC = "default_crawler_commands" 

KAFKA_TWEETS_TOPIC = "default_unprocessed_tweets" 

KAFKA_CRAWL_LOGS_TOPIC = "default_crawler_logs"
