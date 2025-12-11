# Event Type Prediction tools
============

This repository contains scripts demonstrating the functionality of the event type prediction tools in CREXDATA Task 4.5.
The aim of this task is to monitor a social media platform in order to detect in real time posts that
contain relevant information about the ongoing weather incident of interest.

The first step then is to assess whether a post contains relevant information. To do this, we fine-tune a BERT-based
model using datasets tagged for this purpose, that is, each document is tagged whether they contain information about
an event or not. Right now, the classes are Fire (ie. containing information regarding a fire), Flood and None (no
relevant information or something completely unrelated).

Scripts
------------
The scripts in this repository, `training/main_train.py` (with `training/train.py` as auxiliary), `inference/model_inference.py`, `preprocessing/data_process.py`, and `training/distill_model.py`.

### Preprocessing data for training

Processes a TSV file to detect language in social media post and anonymize them by replacing usernames `@`with `<USERNAME>`, and emails with `<EMAIL>`. Detected languages are saved in column "tweet_language" and column with social media posts is rename to "tweet_text". The dataset is then exported as TSV at output path as `FILENAME_processed.tsv`.

#### Usage:

Parameters:

    -f/--file-path: path of tsv file to preprocess

    -tc/--text-colname: name of tweet or text column in tsv file

    -o'/--output-path: path to save tsv

Example:

    python preprocessing/data_preprocess.py -f /INPUT_PATH/FILENAME.tsv -o /OUTPUT_PATH/ --text-colname POST_COLUMN_NAME


### Training an event prediction model

To train an event prediction model, we mainly need a dataset to train from, a .tsv file with at least the columns
"event" (fire, flood, none, etc.) and "tweet_text". It should also include a column "tweet_language" if the multilanguage
parameter wants to be used.

#### Usage 

Parameters:

    -ei/--experiment_id: name of the experiment (default: current date-time)

    -r/--results_folder: folder in which to save the output files (default: "results")

    -es/--experiment_suffix: any other information to add to the experiment id (optional)

    -ml/--max_length: maximum number of tokens per input sentence (default: 512)

    -m/--model_path: path to the base model or complete name to be downloaded from huggingface hub (models we have tested:
    bert-base-multilingual-cased, distilbert-base-multilingual-cased, cardiffnlp/twitter-xlm-roberta-base,
    Twitter/twhin-bert-base)

    --train_file_path: path to training file

    --few_shot_train_file_path: path to few-shot training file (optional)

    --validation_file_path: path to validation file

    --test_file_path: path to test file

    --fine_tuning: which technique to use to fine-tune the model, choices=('normal', 'lora'), (default: no fine-tuning,
    NOT recommended)

    --multilanguage: mainly for testing, the final results will be displayed by language

Example:

    python training/main_train.py -m Twitter/twhin-bert-base 
    --train_file_path data/train_file.tsv 
    --validation_file_path data/val_file.tsv 
    --test_file_path data/test_file.tsv --fine_tuning normal

#### Resulting files 

By default, all the output files will be placed in the "results" folder. Inside, each experiment will have a folder
named after the experiment_id+experiment_suffix, which will in turn contain the checkpoints, the configuration, the tokenizer
and the test results (report.tsv and test_results.json contain roughly the same information but in different format).

### Testing an event prediction model 

Once the user has obtained a fine-tuned model in some way (either training it or acquiring it from somewhere else), it can be
tested with any other datasets available using `inference/model_inference.py`.

#### Usage

Parameters:

    --test_file_path: path to test file

    --out_file_path: path to output file

    --model_path: path to fine-tuned model. The folder should contain a "tokenizer" folder, an "id2label.json" file, 
    and the checkpoints, which can be organized in folders (checkpoint-5, checkpoint-10, this is the way the training 
    script saves the checkpoints) or we can just place "model.safetensors" and "config.json" along with the rest of the 
    mentioned files.

    --lora: if we have used LoRA to fine-tune a model we need to specify it because the loading is a bit different

#### Resulting files 

out_file_path will contain three columns: "tweet_text" (the original text), "prediction" (the predicted class), 
"prediction_score" (the score for the predicted class).

### Distilling an event prediction model

This process distills a fine-tuned event prediction model sacrificing a bit of performance for faster inference.

#### Usage

Parameters:

    -p/--pretrained_model_path: folder path to fine-tuned model to be distilled 

    -tr/--train_file_path: path to training file

    -te/--test_file_path: path to test file

    -ml/--max_length: maximum number of tokens per input sentence (default: 50)

    -hl/--hidden_layers: number of hidden layers to reduce the distilled model to.

    -ei/--experiment_id: name of the experiment (default: current date-time)

    -es/--experiment_suffix: any other information to add to the experiment id (optional)

    -r/--results_folder: folder in which to save the output files (default: "results")

#### Resulting files

Is a model in the results_folder. The folder should contain a "tokenizer" folder, an "id2label.json" file, and the checkpoints, which can be organized in folders (checkpoint-5, checkpoint-10, this is the way the training script saves the checkpoints) or we can just place "model.safetensors" and "config.json" along with the rest of the mentioned files.

## License

This work is distributed under a [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0).
