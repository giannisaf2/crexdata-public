# Text Mining UI: A gradio app tool to use CREXDATA social media toolkit

## Table of Contents

- [Description](#description)
- [Requirements](#requirements)
- [Setup](#setup)
- [Usage](#usage)
- [License](#license)

## Description
This is the text mining UI developed for the CREXDATA project. It reads data from Kafka topics, and uses RapidMiner AI Hub endpoints, so it is mandatory to have those setup before using this tool.

## Requirements
- Docker (To run the container).
- Pre-hosted Kafka Cluster with Topics setup (see usage document).
- Permission files (certificate.pem, key.pem, CARoot.pem) for Kafka Cluster safed in kafkaConnection directory.
- A HuggingFace LLM in GGUF format compatible with llama-cpp. Suggestion: [Llama-3.2 3B Q5_K_XL](https://huggingface.co/unsloth/Llama-3.2-3B-Instruct-GGUF)
- A HuggingFace sentence transformer model for text embedding. Suggestion: [Multilingual-e5-small](https://huggingface.co/intfloat/multilingual-e5-small)

## Setup

### Install Docker:
- Windows: [Guide](https://docs.docker.com/desktop/setup/install/windows-install/)
- Ubuntu: [Guide](https://docs.docker.com/engine/install/ubuntu/)

### Configure UI
As a requirement you need a running kafka cluster already setup. The server may require authetication (SASL_SSL) or no authentication.
If you use a server that requires authentication provide the details in config.py
```bash
CREXDATA_MAIN_CONFIG = {'bootstrap.servers': '<KAFKA_SERVER>',       # bootstrap server
                        'security.protocol': 'SASL_SSL',             # AUTH mechanism
                        'sasl.mechanism': 'PLAIN',                   # AUTH mechanism
                        'sasl.username': '<USERNAME>',               # username to access kafka server
                        'sasl.password': '<PASSWORD>',               # password to access kafka server
                        'ssl.certificate.location': os.path.join(ROOT_DIR, 'kafkaConnection', 'certificate.pem'),
                        'ssl.key.location': os.path.join(ROOT_DIR, 'kafkaConnection', 'key.pem'),
                        'ssl.ca.location': os.path.join(ROOT_DIR, 'kafkaConnection', 'CARoot.pem'),
                        }
```
For Question Answering, it possible to use a LLM setup as an endpoint in RapidMiner AI Hub, or a local LLM.
- To use a local model, change ```"use_aihub"``` to False, and provide the name of the model in ```"llm_model"```. Ensure the model is already download to the ```models``` directory. Also, ensure an embedder model used for RAG is in provided in ```"embedder_model"```, and downloaded to the ```models``` directory.
- To use the an AI Hub model, provide the enmdpoint url, the apitoken, and generation parameters.
```bash
QA_CONFIG = {
    "use_aihub": False,                                              # set to False to use local model for QA, but you must have a llama-cpp GGUF model downloaded into ./models
    "llm_model": "Llama-3.2-3B-Instruct-UD-Q5_K_XL.gguf",            # A llama-cpp GGUF model downloaded into ./models is required if "use_aihub" is False
    "embedder_model": "multilingual-e5-small",                       # Note QA requires an embedder model in local  ./models/
    "aihub_apitoken": "",                                            # Security token to access AIHUB QA endpoint - required if "use_aihub" is True
    "aihub_params": {                                                # AIHUB QA endpoint parameters - required if "use_aihub" is True
        "endpoint_url": "<QA_AIHUB_URL>",
        "min_p": 0.05,
        "top_k": 10,
        "top_p": 0.95,
        "max_tokens": 4096,
        "temperature": 0.7,
        "repeat_penalty": 1.1,
        "model": "Llama-3.2-3B-Instruct-UD-Q5_K_XL.gguf"
    }
}
```
Then, add permission files (.pem) in ```kafkaConnection``` directory, the ssl.certificate, ssl.key, and ssl.ca files are needed. Named as in the [Requirements](#requirements).

## Usage
Build and Run Container:

To Run the container after setup run this command. The ```--build``` parameter is only required on the first run or if you make changes to the Dockerfile
```bash
docker-compose up --build -d
```
The docker compose file will run the gradio app and make is available at ```localhost:7860/```

For usage of the Text mining UI refer to the software documentation in ```docs``` directory.

## License

This work is distributed under a [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0).

