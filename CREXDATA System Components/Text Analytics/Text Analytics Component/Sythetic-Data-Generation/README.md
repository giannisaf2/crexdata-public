# Sythetic Data Generation Scripts

## Table of Contents

- [Description](#description)
- [Requirements](#requirements)
- [Setup](#setup)
- [Usage](#usage)
- [License](#license)

## Description
Generates a fixed amount of sythetic tweets using an LLM. Tweets are based on four prompt types:
    - Posts from affected persons’ perspectives with keywords ("with-keywords"). 
    - Posts from government and meteorological agencies with warning alerts ("gov-alerts"). 
    - Posts unrelated to the crisis discussing random topics ("nones-random-topics"). 
    - Posts related to the crisis but lacking useful information ("nones-donations-sympathy").

## Requirements
- Python
- A HuggingFace LLM in GGUF format compatible with llama-cpp. Suggestions: [Mistral-Small-3.2 24B q8_0](https://huggingface.co/unsloth/Mistral-Small-3.2-24B-Instruct-2506-GGUF), [Gemma-3 27B q8_0](https://huggingface.co/unsloth/gemma-3-27b-it-GGUF)

## Setup

### Python Virtual Environment Setup:
Create a virtual env on you machine:
```bash
python -m venv venv
```
Then activate venv and install requirements:
```bash
source venv/bin/activate
pip install -r requirements.txt
```

### Configure Scripts
Modify config.py:
- Edit MODEL_CONFIG to provide generation paramters, model location, and model name.
- Edit CRISIS_KEYWORDS to change keywords if necessary.
- Edit OUT_DIR to provide location to save generated data.

## Usage

The script generates data in JSON format. 

The script ```generate_tweets.py``` is the main script and accepts the parameters:
```bash
--language, - Langauge in tweet. Supported options: ["English","Spanish","Catalan","German"].
--crisis-type, - Crisis type. Supported options: ["flood","wildfire"].
--prompt-type, - Type of prompt to generate with. Supported options: ["nones-random-topics","with-keywords","gov-alerts","nones-donations-sympathy"].
--no-tweets, - Number of tweets to generate.
--batch-no, Batch number. Use if generating multiple batches. You can run a shell script to generate muliple batches while incrementing this parameter.
```
Usage Example:

```bash
source venv/bin/activate
python generate_tweets.py --language English --crisis-type flood --prompt-type with-keywords --no-tweets 100 --batch-no 1
```

## License

This work is distributed under a [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0).

