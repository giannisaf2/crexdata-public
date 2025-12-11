# LLM Automatic Annotation Tool: A tool to suggest suitable labels for texts

## Table of Contents

- [Description](#description)
- [Requirements](#requirements)
- [Setup](#setup)
- [Usage](#usage)
- [License](#license)

## Description
This is a tool to suggest labels / annotate text for training models for text classification. It uses an LLM and a well defined prompt for a particular usecase (ex. in our case, it is used for Crisis Text Annotation) to annotate texts.
These labels are synthetic and should be reviewed before using them to train any models. We also include a data cleaning script which utilizes [Cleanlab AI's](https://cleanlab.ai/) open-source python library to find erroneous labels in the annotated texts.

## Requirements
- Python
- A HuggingFace instruction tuned model. Suggestion: [Gemma 3 4B](https://huggingface.co/google/gemma-3-4b-it)
- A HuggingFace sentence transformer model for text embedding. Suggestions: [BGE-M3](https://huggingface.co/BAAI/bge-m3), [LaBSE](https://huggingface.co/sentence-transformers/LaBSE)

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
First, ensure LLM and Embedder models are saved in models directory.

Modify config.py:
- Edit labels to reflect the labels you want to use in annotation.
- Edit prompt to match your usecase, but make sure they match the style of the sample prompt. Specifically, the format of the output ```{"classification": "[flood/fire/none]"}```and tags ```<EXAMPLES>, <TEXT>``` should be in the new prompt.
Also, ensure your labels match those in the prompt.
- Edit the reprompt with your new labels, This reprompt string is used to reprompt the model when it doesn't follow the labelling and output format.
- The generation parmaters in ANOTATION_CONFIG will work as is but can be tweaked depending on your Instruct LLM. Find more parameters in Transformers library documentation [HERE](https://huggingface.co/docs/transformers/en/main_classes/text_generation). 
- For LABEL_CLEANING_CONFIG, I suggest using the current config, just changing ``` data_text_header, and data_label_header``` to match the text field and label field in the (.tsv) file you want to clean. If you want to tweak the config read [cleanlab documentation](https://docs.cleanlab.ai/v2.5.0/).
- Finally in MODELS_CONFIG, you can configure some HuggingFace LLMs for annotation. It is a dictionary, with different models configured for use (PS. Gemma 3 4B is already configured). To configure add a new model following the samples:
```bash
"mistral": {"path_name": "Mistral-7B-Instruct-v0.3", "start_delimiter": "[/INST]", "end_delimiter": "</s>", "extra_params": {}, "extra_tokenizer_params": {"use_fast": True}},

"mistral" - model tag used as a parameter to the run_llm_annotation.py script when annotating.
"path_name" - Is the name of the llm as saved in the model directory (PS. the configured models are saved using their huggingface model tags)
"start_delimiter" - Is a string used to begin the llm's response in their chat template, necessary for extracting the annotation.
"end_delimiter" - Is a string used to end the llm's response in their chat template, necessary for extracting the annotation.
"extra_params" - To pass extra parameters to AutoModelForCausalLM.from_pretrained() - Like "trust_remote_code", necessary in Microsoft's Phi models.
"extra_tokenizer_params" - To pass extra parameters to AutoTokenizer.from_pretrained() - Like "use_fast"
```

## Usage

### LLM Annotation
The LLM Annotation tool has the following features:
- Zeroshot annotation: Annotation based solely on a provided prompt.
- Fewhot annotation: Annotation that combines the prompt and some examples to improve the annotation process. The samples can be provided in two ways:
    - in_prompt; This is the conventional way of providing fewshot samples, which is directly in the prompt with a tag ``` EXAMPLES: ```
    - in_chat; A way of providing the samples that leverages the conversational ability of instructed LLMs. It uses the [chat template](https://huggingface.co/docs/transformers/en/chat_templating) to provide the samples as alternating, user inputs and model responses. This method is more computationally expensive.  

The script ```src/run_llm_annotation.py``` is the main annotation script and accepts the parameters:
```bash
-f, --file-path - The full path of the (.tsv) to annotate. ex. data/sample.tsv
--text-header - The name of the field in the tsv file that has the text to annotated. ex. text
--modelname - The name of a pre-configured model in the models directory. ex. gemma3_4b
-o, --output-path - The path to save the annotation output. ex. data/
-e, --fewshot - The full path to fewshot file, to provide to the model to help annotations. ex. data/fewshot_sample.tsv. Fewshot file should be in the same structure as the provided sample.
--fewshot-type - Fewshot method used when providing samples. Allowed values are: "in_chat", which uses the chat template to provide the samples as in conversation; and  "in_prompt", which provides the samples in the prompt. (Default: in_chat)
--with-confidence - Prompts the LLM 3 times for an annotation and saves a confidence value which represents the LLM's confidence in the annotation. The value is count of times it repeates it first response divided by 3. Adding this tag will reduce the speed of annotation. (Default:)
```
Usage Examples:

LLM annotation zeroshot:
```bash
source venv/bin/activate
python src/run_llm_annotation.py -f data/sample.tsv -o data/ --text-header tweet_text --modelname gemma3_4b 
```

LLM annotation with fewshot:
```bash
source venv/bin/activate
python src/run_llm_annotation.py -f data/sample.tsv -o data/ --text-header tweet_text --modelname gemma3_4b --fewshot data/fewshot_sample.tsv --fewshot-type in_prompt
```

LLM annotation with confidence:
```bash
source venv/bin/activate
python src/run_llm_annotation.py -f data/sample.tsv -o data/ --text-header tweet_text --modelname gemma3_4b --with-confidence 
```

### Cleanlab Data Cleaning
The data cleaning tool uses [Cleanlab AI's](https://cleanlab.ai/) open-source python library to find erroneous labels in the annotated texts. It uses an embedder model to get text embeddings that uses that with a logistic regression classifier to create predicted probabilities which is needed for cleaning the data. More on the working of this library is found in the [cleanlab docs](https://docs.cleanlab.ai/v2.5.0/). The cleaning process uses the logistic regression classifier to suggest suitable labels for the error labels found in the data.

Features:
- Uses any HugginFace sentence transformer model (sbert) for feature extraction.
- Uses a threshold to filter texts label quality scores that don't meet your requirements. So, they can be re-labelled.

Before use:
- Ensure the LABEL_CLEANING_CONFIG in the config.py file has the correct ``` data_text_header, and data_label_header```
- Also ensure you have an embedder model download, in the models directory.

The ```src/run_cleanlab_data_cleaning.py``` script accepts the following parameters:
```bash
-f, --file-path - The full path of the (.tsv) to clean with cleanlab. ex. data/filename.tsv
-s, --score-threshold - A float value between [0.0 - 1] to use as label quality threshold. Data samples with label quality (provided by cleanlab) below this threshold are also considered error labels (recommendation: 0.3)
--relabel - Flag to enable re-labelling error labels found in dataset.
```

Usage Examples:

Label cleaning without re-labelling:
```bash
python src/run_cleanlab_data_cleaning.py -f data/filename.tsv -s 0.3
```
Note, dataset will be split into filename_clean.tsv and filename_dirty.tsv

Label cleaning with re-labelling:
```bash
python src/run_cleanlab_data_cleaning.py -f data/filename.tsv -s 0.3 --relabel
```
Note, output will be filename_cleaned.tsv


## License

This work is distributed under a [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0).

