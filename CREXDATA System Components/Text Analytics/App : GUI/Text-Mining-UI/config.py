import os

ROOT_DIR = os.path.dirname(os.path.abspath(__file__))

MODELS_DIR = os.path.join(ROOT_DIR, 'models')

CREXDATA_MAIN_CONFIG = {'bootstrap.servers': '<KAFKA_SERVER>',
                        'security.protocol': 'SASL_SSL',
                        'sasl.mechanism': 'PLAIN',
                        'sasl.username': '<USERNAME>',               # username to access kafka server
                        'sasl.password': '<PASSWORD>',               # password to access kafka server
                        'ssl.certificate.location': os.path.join(ROOT_DIR, 'kafkaConnection', 'certificate.pem'),
                        'ssl.key.location': os.path.join(ROOT_DIR, 'kafkaConnection', 'key.pem'),
                        'ssl.ca.location': os.path.join(ROOT_DIR, 'kafkaConnection', 'CARoot.pem'),
                        }


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