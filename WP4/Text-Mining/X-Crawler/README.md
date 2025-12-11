# X Crawler: A tool to crawl latest posts from X and save them in a Kafka Topic

## Table of Contents

- [Description](#description)
- [Requirements](#requirements)
- [Setup](#setup)
- [Usage](#usage)
- [License](#license)

## Description
This is a X crawling toolkit setup in a docker container. It receives commands from a Kafka Topic to start crawling posts from a particular {Lat, Lon} and then saves the posts to another Kafka Topic. It is setup like this so it can be run on a remote server.

## Requirements
- Docker (To run the container).
- Pre-hosted Kafka Cluster with Topics setup for crawl commands, crawl logs, and crawled posts.
- Permission files (certificate.pem, key.pem, CARoot.pem) for Kafka Cluster safed in kafkaConnection directory.

## Setup

### Install Docker:
- Windows: [Guide](https://docs.docker.com/desktop/setup/install/windows-install/)
- Ubuntu: [Guide](https://docs.docker.com/engine/install/ubuntu/)

### Configure Crawler
As a requirement you need a running kafka cluster setup using SASL_SSL. If you don't want to use authentication you can comment out (KAFKA CONFIG - SASL_SSL) in config.py
```bash
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
```
And uncomment:
```bash
### KAFKA CONFIG - SASL_SSL
# KAFKA_CONFIG = {
#     'bootstrap.servers': os.getenv('KAFKA_BROKER'),
#     }
```
If you have SASL_SSL setup, edit docker-compose.yml.example and add the correct config for broker, username and password.
```bash
KAFKA_BROKER: <KAFKA SERVER> # Ex. localhost:9092
KAFKA_USERNAME: <USERNAME> # username to access kafka server
KAFKA_PASSWORD: <PASSWORD> # password to access kafka server
```
Also, add your configured Kafka Topics in config.py
```bash
KAFKA_COMMANDS_TOPIC = "default_crawler_commands" 
KAFKA_TWEETS_TOPIC = "default_unprocessed_tweets" 
KAFKA_CRAWL_LOGS_TOPIC = "default_crawler_logs"
```
Then, add permission files (.pem) in kafkaConnection directory, the ssl.certificate, ssl.key, and ssl.ca files are needed. Named as in the [Requirements](#requirements).
Finally, rename docker-compose-example.yml to docker-compose.yml

## Usage
Build and Run Crawler Container:

To Run the container after setup run this command. The ```--build``` parameter is only required on the first run or if you make changes to the Dockerfile
```bash
docker-compose up --build -d
```

To use Crawler, send commands to command topic. Example commands below:

Start Command:
```bash
{
"crawl_id":"flood_barcelona_250724", # A unique ID to identify the crawl instance, used to start and kill crawl
"command": "start", # Command to Crawler  
"crawl_params.username":"test-username", # Username of X account to crawl with
"crawl_params.password":"test-passwd", # Password of X account to crawl with
"crawl_params.email":"test-email", # Email of X account to crawl with, should be Gmail to activate APP pass
"crawl_params.emailpass":"test-emailpass", # APP pass setup in Gmail 
"crawl_params.lat":"41.3825", # Latitude to Crawl posts from
"crawl_params.lon":"2.176944", # Longitude to Crawl posts from
"crawl_params.radius":"100", # Radius around Lat, Lon to Crawl post from in Kilometers
"crawl_params.lang":"Spanish" # Language of posts
}
```
Kill Command:
```bash
{
"crawl_id":"flood_barcelona_250724", # A unique ID to identify the crawl instance, used to start and kill crawl
"command": "kill", # Command to Crawler  
}
```
To setup APP pass for your email follow these [Instructions](https://support.google.com/mail/answer/185833?hl=en). 
You can post these crawl commands to you commands kafka topic using kakfa CLI or using test/test.produce.py script.
But, make sure to edit TEST_KAFKA_COMMANDS_TOPIC in the script to your kafka commands topic
```bash
python test/test.produce.py --id flood_barcelona_250724 --case start
python test/test.produce.py --id flood_barcelona_250724 --case kill
```

## License

This work is distributed under a [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0).

