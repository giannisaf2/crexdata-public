import os

labels = ["fire", "flood", "none"]


# prompt = """Given the following text: "<TEXT>".
# Is it talking about a weather disaster corresponding to a "fire", or "flood", or "none"?
# Classify as "fire" or "flood" if clearly talking about a fire or flood; otherwise, default to "none".
# Your response should be formatted as follows:
# {"classification": "[flood/fire/none]"}
# Example
# Input: "Evacuation orders have been issued for Riverside due to rapidly spreading wildfire near the canyon."
# Output: {"classification": "fire"}<EXAMPLES>
# """.strip()

# prompt = """
# Analyze the given body of text and determine whether it contains information relevant to an ongoing fire disaster, flood disaster, or neither. 
# The classification should be based on the presence of specific, actionable details or key indicators of relevance to such disasters.
# Examine the text for mentions of fire-related or flood-related events, such as locations, impacts, safety advisories, or emergency responses.
# Classify as "fire" if the text contains information about wildfires, house fires, smoke hazards, evacuation orders due to fire, or related topics.
# Classify as "flood" if the text discusses water inundation, flash floods, storm surges, flood warnings, or evacuation due to flooding.
# Classify as "none" if the text does not provide information relevant to either disaster or lacks actionable disaster-related content.
# Only assign "flood" or "fire" if the relevance is clear; otherwise, default to "none".
# The response should be formatted as follows:
# {"classification": "[flood/fire/none]"}
# Example
# Input: "Evacuation orders have been issued for Riverside due to rapidly spreading wildfire near the canyon."
# Output: {"classification": "fire"}<EXAMPLES>
# Classify:
# Input: "<TEXT>"
# """.strip()

prompt = """
Analyze the given body of text and determine whether it contains information relevant to an ongoing fire disaster, flood disaster, or neither.
The classification should be based on the presence of specific, actionable details or key indicators of relevance to such disasters.
Examine the text for mentions of fire-related or flood-related events, such as locations, impacts, safety advisories, or emergency responses.
Classify as "fire" if the text contains information about wildfires, house fires, smoke hazards, evacuation orders due to fire, or related topics.
Classify as "flood" if the text discusses water inundation, flash floods, storm surges, flood warnings, or evacuation due to flooding.
Classify as "none" if the text does not provide information relevant to either disaster or lacks actionable disaster-related content.
Texts that express sympathy, offer prayers, or request donations should also be classified as "none".
Only assign "flood" or "fire" if the relevance is clear; otherwise, default to "none".
The response should be formatted as follows:
{"classification": "[flood/fire/none]"}
Example
Input: "Evacuation orders have been issued for Riverside due to rapidly spreading wildfire near the canyon."
Output: {"classification": "fire"}<EXAMPLES>
Classify:
Input: "<TEXT>"
""".strip() # Includes sympathy

# prompt = """
# Analyze the given body of text and determine whether it contains information relevant to an ongoing fire disaster, flood disaster, or neither.
# Examine the text for mentions of fire-related or flood-related events, such as locations, impacts, safety advisories, or emergency responses.
# Classify as "fire" if the text contains information about wildfires, house fires, smoke hazards, evacuation orders due to fire, or related topics.
# Classify as "flood" if the text discusses water inundation, flash floods, storm surges, flood warnings, or evacuation due to flooding.
# Classify as "none" if the text does not provide information relevant to either disaster.
# Texts that express sympathy, offer prayers, or request donations should also be classified as "none".
# The response should be formatted as follows:
# {"classification": "[flood/fire/none]"}
# Example
# Input: "Evacuation orders have been issued for Riverside due to rapidly spreading wildfire near the canyon."
# Output: {"classification": "fire"}<EXAMPLES>
# Classify:
# Input: "<TEXT>"
# """.strip() # Reduces phrases containing "'actionable' information or details"

# prompt = """
# Analyze the text and classify it as "fire", "flood", or "none" based on whether it provides specific, actionable information relevant to an ongoing fire or flood disaster.
# Classify as "fire" if the text mentions wildfires, house fires, smoke hazards, or evacuation orders due to fire.
# Classify as "flood" if the text mentions water inundation, flash floods, storm surges, flood warnings, or evacuation due to flooding.
# Classify as "none" if the text does not provide information relevant to either disaster or lacks actionable disaster-related content.
# Only assign "fire" or "flood" if the relevance is clear; otherwise, default to "none." 
# The response should be formatted as follows:
# {"classification": "[flood/fire/none]"}
# Example
# Input: "Evacuation orders have been issued for Riverside due to rapidly spreading wildfire near the canyon."
# Output: {"classification": "fire"}<EXAMPLES>
# Classify:
# Input: "<TEXT>"
# """.strip()


reprompt = """BAD RESPONSE. Format response as follows: {"classification": "[flood/fire/none]"}"""


MODELS_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'models')


ANNOTATION_CONFIG = {
    "prompt":  prompt, 
    "labels": labels,
    "reprompt": reprompt,
    "generation_params": {
        "use_cache": True,
        "max_new_tokens": 25,
        "do_sample": False, 
        "temperature": 0.7,
        "top_p": 0.1,
        "top_k": 1,
        "repetition_penalty": 1.1,
        }
    }


LABEL_CLEANING_CONFIG = {
    "data_text_header": "tweet_text",
    "data_label_header": "predicted_labels",
    "labels": ["fire", "flood", "none"],
    "min_label_class_size": 10, # necessary to have a min amount of text per label that is maintained even after cleaning
    "cleanlab":{
        "clean_learning_params": {
            "cv_n_folds": 5, 
            "find_label_issues_kwargs": {  
                "filter_by": "prune_by_noise_rate",
                "min_examples_per_class": 10
                }
            },
        "find_label_issues_params": {
            "save_space": True
            }
        },
    "embedder_model": { 
        "name": "bge-m3", # Ensure you use a multilingual model when cleaning multilingual data (ex. bge-m3)
        "params": {
            "batch_size": 32, # Increase by multiples of 2 (32 works best in small RAM size)
            "show_progress_bar": True,
            "normalize_embeddings": False
            }
        }
    }  


MODELS_CONFIG = {
    "mistral": {"path_name": "Mistral-7B-Instruct-v0.3", "start_delimiter": "[/INST]", "end_delimiter": "</s>", "extra_params": {}, "extra_tokenizer_params": {"use_fast": True}},
    "qwen2_7b": {"path_name": "Qwen2-7B-Instruct", "start_delimiter": "<|im_start|>assistant", "end_delimiter": "<|im_end|>", "extra_params": {}, "extra_tokenizer_params": {"use_fast": True}},
    "qwen2_5_7b": {"path_name": "Qwen2.5-7B-Instruct", "start_delimiter": "<|im_start|>assistant", "end_delimiter": "<|im_end|>", "extra_params": {}, "extra_tokenizer_params": {"use_fast": True}},
    "qwen2_5_14b": {"path_name": "Qwen2.5-14B-Instruct", "start_delimiter": "<|im_start|>assistant", "end_delimiter": "<|im_end|>", "extra_params": {}, "extra_tokenizer_params": {"use_fast": True}},
    "catallamav02": {"path_name": "CataLlama-v0.2-Instruct-SFT", "start_delimiter": "<|start_header_id|>assistant<|end_header_id|>", "end_delimiter": "<|eot_id|>", "extra_params": {}, "extra_tokenizer_params": {"use_fast": True}},
    "llama3_1_8b": {"path_name": "Meta-Llama-3.1-8B-Instruct", "start_delimiter": "<|start_header_id|>assistant<|end_header_id|>", "end_delimiter": "<|eot_id|>", "extra_params": {}, "extra_tokenizer_params": {"use_fast": True}},
    "salamandra_7b": {"path_name": "salamandra-7b-instruct", "start_delimiter": "<|im_start|> assistant", "end_delimiter": "<|im_end|>", "extra_params": {}, "extra_tokenizer_params": {"use_fast": True}},
    "gemma3_4b": {"path_name": "gemma-3-4b-it", "start_delimiter": "<start_of_turn>model", "end_delimiter": "<end_of_turn>", "extra_params": {}, "extra_tokenizer_params": {"use_fast": True}},
    "gemma2_9b": {"path_name": "gemma-2-9b-it", "start_delimiter": "<start_of_turn>model", "end_delimiter": "<end_of_turn>", "extra_params": {}, "extra_tokenizer_params": {"use_fast": True}},
    "phi3mini": {"path_name": "Phi-3-mini-4k-instruct", "start_delimiter": "<|assistant|>", "end_delimiter": "<|end|>", "extra_params": {"trust_remote_code": True}, "extra_tokenizer_params": {"use_fast": True}},
    "phi3medium": {"path_name": "Phi-3-medium-4k-instruct", "start_delimiter": "<|assistant|>", "end_delimiter": "<|end|>", "extra_params": {"trust_remote_code": False}, "extra_tokenizer_params": {"use_fast": True}},
    "phi3_5mini": {"path_name": "Phi-3.5-mini-instruct", "start_delimiter": "<|assistant|>", "end_delimiter": "<|end|>", "extra_params": {"trust_remote_code": True}, "extra_tokenizer_params": {"use_fast": True}},
    "phi4mini": {"path_name": "Phi-4-mini-instruct", "start_delimiter": "<|assistant|>", "end_delimiter": "<|end|>", "extra_params": {"trust_remote_code": True}, "extra_tokenizer_params": {"use_fast": True}},
    "occiglot_7b_eu5": {"path_name": "occiglot-7b-eu5-instruct", "start_delimiter": "<|im_start|> assistant", "end_delimiter": "</s>", "extra_params": {}, "extra_tokenizer_params": {"use_fast": True}}, 
    "eurollm_9b": {"path_name": "EuroLLM-9B-Instruct", "start_delimiter": "<|im_start|>assistant", "end_delimiter": "<|im_end|>", "extra_params": {}, "extra_tokenizer_params": {"use_fast": True}},
    "teuken_7b": {"path_name": "Teuken-7B-instruct-research-v0.4", "start_delimiter": "Assistant:", "end_delimiter": "</s>", "extra_params": {"trust_remote_code": True}, "extra_tokenizer_params": {"use_fast": True, "trust_remote_code": True}},
    }