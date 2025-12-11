# PYflink setup for model scoring

![plot](./imgs/flink.png)

## Table of Contents

- [Description](#description)
- [Requirements](#requirements)
- [Setup](#setup)
- [Usage](#usage)
- [License](#license)

## Description
This is a pyflink setup in a docker container. It automatically processes social media messages received in the configured Kafka Topic, classifying them as *fire*, *flood*, or *none* and saves them to new Kafka Topics. It is setup like this so it can be run on a remote server.

## Requirements
- Docker.
- Rapiminer AI Hub API endpoint url, setup in *./aihub*.
- Keystore and truststore files to access Kafka cluster (*kafka.keystore.jks*, *kafka.keystore.jks* saved in *./flink-2/shared/kafka_auth_files/*)

## Setup

### Install Docker:
- Windows: [Guide](https://docs.docker.com/desktop/setup/install/windows-install/)
- Ubuntu: [Guide](https://docs.docker.com/engine/install/ubuntu/)

### Configure and Setup
As a requirement you need a running kafka cluster setup using SASL_SSL. If you don't want to use authentication, when starting the job send an unautheticated server as parameter, and also set this in the config.

The configuration is found in the *CONFIGURATION* in *./shared/relevance_job_utils/relevance-job-testing-async.py*

```bash
# -----------------CONFIGURATION---------------------------------------
"""
Configure this section before running
"""
ENDPOINT_URL = "<AI_HUB_ENDPOINT_URL>"

AUTHENTICATED = True # or False for unautheticated

CREXDATA_AUTH = {
    "security.protocol": "SASL_SSL",
    "sasl.mechanism": "PLAIN",
    "ssl.truststore.location": "/mnt/kafka_auth_files/kafka.truststore.jks",
    "ssl.truststore.password": "<PASWORD>",
    "ssl.keystore.location": "/mnt/kafka_auth_files/kafka.keystore.jks",
    "ssl.keystore.password": "<PASWORD>",
    "sasl.jaas.config": "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"<USER>\" password=\"<PASSWORD>\";"  
}

#----------------------------------------------------------------------
```
Edit the texts enclosed in *<>*, also set *AUTHENTICATED* to *False* if using an unautheticated server.

Then, add permission files (.jks) in kafka_auth_files directory, the kafka.truststore.jks, and kafka.keystore.jks files are needed. 

Finally, rename extensions of *flink-sql-connector-kafka-4.0.0-2.0.jar.keep*, and *kafka-clients-4.0.0.jar.keep* to *.jar* in *./shared/relevance_job_utils/*.

## Usage
Build and Run the Container:

To Run the container after setup run this command. The ```--build``` parameter is only required on the first run or if you make changes to the Dockerfile
```bash
docker-compose up --build -d
```

To start a relevance prediction flink job using flink, run in the terminal of the server hosting the container:

```bash
docker exec <DOCKER_CONTAINER_NAME> bash -c 'flink run -d -py /mnt/relevance_job_utils/relevance-job-testing-async.py --jarfile /mnt/relevance_job_utils/flink-sql-connector-kafka-4.0.0-2.0.jar --kafka-server <BOOSTRAP_SERVER> --input-topic <KAFKA_TOPIC_TO_PROCESS> --output-topic <KAFKA_TOPIC_TO_SAVE_ALL_PROCESSED> --output-filter <KAFKA_TOPIC_TO_SAVE_ONLY_RELEVANCE> --job-name <NAME_OF_JOB> --offset-type <OFFSET_TYPE>'
```
Edit the command:
- *<DOCKER_CONTAINER_NAME>* - name of the docker container, find the name by running ```docker ps``` which will list all running containers.
- *<BOOSTRAP_SERVER>* - is the bootstrap server of the kafka cluster. ex: localhost:9092.
- *<KAFKA_TOPIC_TO_PROCESS>* - kafka topic containing social media posts to process, must be in JSON with atleast a tweet_text field, ```{"tweet_text":"<TEXT>"}```.
- *<KAFKA_TOPIC_TO_SAVE_ALL_PROCESSED>* - kafka topic to save all processed messages.
- *<KAFKA_TOPIC_TO_SAVE_ONLY_RELEVANCE>* - kafka topic to save only those messages classified as *fire*, or *flood* due to relevance to incident.
- *<NAME_OF_JOB>* - name of flink job for identification purposes.
- *<OFFSET_TYPE>* - offset to start processing message *latest* will start from newest messages while *earliest* will start from beginning, these are Kafka configurations.

You can also run jobs from RapidMiner AI Studio, see *./aihub/processes/4. BSC-Text-Mining-Start-Relevance-Prediction-Streaming-Job.rmp*.

Once the job is running, you can view it on the Apache Flink Dashboard at ```http://localhost:48091/#/job/running```, you may need to change localhost to your IP, if you server has a proxy.


## License

This work is distributed under a [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0).

