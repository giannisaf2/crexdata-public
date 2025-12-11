import os
import re
import sys
import json
import torch

from transformers import AutoTokenizer, AutoModelForCausalLM

sys.path.append(os.path.dirname(os.path.dirname(os.path.realpath(__file__))))
from config import ANNOTATION_CONFIG, MODELS_DIR, MODELS_CONFIG

class AnnotateModel:
    def __init__(self, llm_name, gpu_check):
        self.modelname = llm_name
        self.gpu_check = gpu_check
        self.tokenizer, self.llm_model = self._init_model() 

    def _init_model(self):       
        if self.gpu_check:
            extra_params = {"device_map": "cuda", "torch_dtype": torch.float16}    
        else:
            extra_params = {"device_map": "auto", "torch_dtype": "auto"}

        model_path = os.path.join(MODELS_DIR, MODELS_CONFIG[self.modelname]["path_name"])
        tokenizer = AutoTokenizer.from_pretrained(model_path, **MODELS_CONFIG[self.modelname]["extra_tokenizer_params"])
        llm_model = AutoModelForCausalLM.from_pretrained(model_path, **extra_params, **MODELS_CONFIG[self.modelname]["extra_params"])
                    
        return tokenizer, llm_model

    def extract_response(self, text):  
        response_start = MODELS_CONFIG[self.modelname]["start_delimiter"]
        response_end = MODELS_CONFIG[self.modelname]["end_delimiter"]

        if self.modelname == "test": # Change modelname in config to test for a model you are testing to output whole text when the special delimiter tokens are not known
            assistant_respond = text
            print(assistant_respond)
        else:     
            assistant_respond = text.split(response_start)[-1].replace(response_end,'').strip().lower()
            
        return assistant_respond

    def reprompt(self, prev_conversation, prev_response):
        
        prev_conversation.append({ 'role': 'assistant', 'content': prev_response }) 
        prev_conversation.append({ 'role': 'user', 'content': ANNOTATION_CONFIG['reprompt'] })
        prompt = self.tokenizer.apply_chat_template(prev_conversation, tokenize=False, add_generation_prompt=True)  
        inputs = self.tokenizer(prompt, return_token_type_ids=False, return_tensors="pt").to(self.llm_model.device) 
            
        outputs = self.llm_model.generate(**inputs, **ANNOTATION_CONFIG["generation_params"])
        output_text = self.tokenizer.decode(outputs[0]) 
        response = self.extract_response(text=output_text) 
        
        return response, prev_conversation
    
def response_check(response, retries):
    
    if retries != 0:
        match = re.search(r'\{.*\}', response, re.DOTALL)
        if match:
            json_content = match.group(0)  
        else:
            return None
        try:
            response_dict = json.loads(json_content)
            _ = response_dict['classification']
        except (json.JSONDecodeError, KeyError) as _:
            return None
        if response_dict['classification'] not in ANNOTATION_CONFIG['labels']:
            return None
    else:
        response_dict = {'classification': 'none'}
    
    return response_dict
    
    
def annotate(text, model, fewshot, fewshot_type="in_chat", with_confidence=False):
    
    annotate_model = AnnotateModel(llm_name=model, gpu_check=torch.cuda.is_available())
    
    if fewshot.empty:
        conversation = [ { 'role': 'user', 'content': ANNOTATION_CONFIG['prompt'].replace('<TEXT>', text).replace('<EXAMPLES>', '') } ]
    else:
        fshot_list = fewshot.values.tolist()
        if fewshot_type == "in_prompt":
            examples = "\n"
            for example in fshot_list:
                examples += f"Input: {example[0]}"+'\n'
                examples += 'Output: {"classification": "'+ example[1] +'"}\n'
            conversation = [ { 'role': 'user', 'content': ANNOTATION_CONFIG['prompt'].replace('<TEXT>', text).replace('<EXAMPLES>', examples) } ]
        else:
            conversation = []
            for example in fshot_list:
                conversation.append({ 'role': 'user', 'content': ANNOTATION_CONFIG['prompt'].replace('<TEXT>', example[0]).replace('<EXAMPLES>', '') })
                conversation.append({ 'role': 'assistant', 'content': '{"classification": "'+ example[1] +'"}' })
            conversation.append({ 'role': 'user', 'content': ANNOTATION_CONFIG['prompt'].replace('<TEXT>', text).replace('<EXAMPLES>', '') })

    prompt = annotate_model.tokenizer.apply_chat_template(conversation, tokenize=False, add_generation_prompt=True)
    inputs = annotate_model.tokenizer(prompt, return_token_type_ids=False, return_tensors="pt").to(annotate_model.llm_model.device)
    
    if with_confidence:
        response_texts = []
        for _ in range(3):
            outputs = annotate_model.llm_model.generate(**inputs, **ANNOTATION_CONFIG["generation_params"])
            output_text = annotate_model.tokenizer.decode(outputs[0])  
            response = annotate_model.extract_response(output_text) 
            retry = 3
            response_dict = response_check(response, retry)
            while response_dict == None and retry != 0:
                print("Reprompt..")
                response, conversation = annotate_model.reprompt(conversation, response)
                retry -= 1 
                response_dict = response_check(response, retry)
            response_texts.append(response_dict['classification']) 
        primary_response = response_texts[0]  
        confidence_score = response_texts.count(primary_response) / 3  
        print(f"\n{text}: {primary_response}; score: {confidence_score}")
             
    else:
        outputs = annotate_model.llm_model.generate(**inputs, **ANNOTATION_CONFIG["generation_params"])
        output_text = annotate_model.tokenizer.decode(outputs[0])
        response = annotate_model.extract_response(output_text) 
        retry = 3
        response_dict = response_check(response, retry)
        while response_dict == None and retry != 0:
            print("Reprompt..")
            response, conversation = annotate_model.reprompt(conversation, response)
            retry -= 1
            response_dict = response_check(response, retry)  
            # print(conversation) # For debugging
        confidence_score = None
        primary_response = response_dict['classification']
        print(f"\n{text}: {primary_response}; score: {confidence_score}")
         
    return primary_response, confidence_score
