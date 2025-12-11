import re
import os
import json
import random
import argparse
import llama_cpp

from config import CRISIS_KEYWORDS, MODEL_CONFIG, OUT_DIR

def extract_response(response):
    match = re.search(r'\{.*\}', response, re.DOTALL)
    if match:
        json_content = match.group(0)  
    else:
        return None
    try:
        response_dict = json.loads(json_content)
        return response_dict
    except (json.JSONDecodeError, KeyError) as _:
        return None
   

def main(args):
    LLM = llama_cpp.Llama(model_path=os.path.join(MODEL_CONFIG["path"], MODEL_CONFIG["name"]),
                        n_ctx=MODEL_CONFIG["context_length"],
                        use_mmap=False)


    crisis_type = args.crisis_type
    language = args.language
    no_tweets = f"{args.no_tweets}"
    batch = f"{args.batch_no}"
    

    # Formatting batch number to add to prompt, introduces variety in the SDG process
    if batch[-1] == "1":
        if batch == "11":
            batch += "th"
        else:
            batch += "st"
    elif batch[-1] == "2":
        batch += "nd"
    elif batch[-1] == "3":
        batch += "rd"
    else:
        batch += "th"
        

    ## Prompt for tweets from affected persons perspective with keywords
    if args.prompt_type == "with-keywords":
        prompt = (
                "This is the {BATCH} time sending this prompt."
                "You are generating synthetic social media posts (tweets) written by people affected by an ongoing {CRISIS_TYPE} crisis. "
                "Each post should sound natural, diverse, and realistic.\n"
                "Instructions:\n"
                "- Use first-person perspective.\n"
                "- Each post should describe a different situation, location, or perspective related to the crisis.\n"
                "- Include details such as:\n"
                "  - Specific locations\n"
                "  - Visible damage\n"
                "  - Requests for help, information on people trapped, missing, or injured\n"
                "  - Updates about weather, rescue operations, evacuations, shelters, supply shortages, etc.\n"
                "- Vary emotional tone: panic, fear, urgency, gratitude, anger, confusion, hope.\n"
                "- Vary writing style:\n"
                "  - Formal, informal, slang, abbreviations\n"
                "  - Typos, hashtags, emojis\n"
                "- Avoid duplicate content.\n"
                "- Don't include post asking for donations and expressing sympathy.\n"
                "- The event should feel current or ongoing.\n"
                "- Write the posts in {LANGUAGE} language.\n"
                "- Try to include or relate to these keywords or their synonyms in the posts: {KEYWORDS}\n"
                "Output format should be in JSON:"
                '{"1":"[Tweet 1]",...}\n'
                "No Duplicates. No explanation. No added text, just JSON.\n"
                "Generate {N} tweets"
            )
    
    ## Prompt for tweets from government and meteorological agencies with warning alerts
    if args.prompt_type == "gov-alerts":
        prompt = (
                "This is the {BATCH} time sending this prompt."
                "You are generating synthetic social media posts (tweets) written by government and meteorological agencies during an ongoing {CRISIS_TYPE} crisis. "
                "Each post should sound official, diverse, and realistic. "
                "Write the posts in {LANGUAGE} language.\n"
                "Include and vary posts with these:\n"
                "  - Warning alerts, stay-at-home warnings, warning/disaster level\n"
                "  - Extreme weather warning, weather statistics (like precipitation etc.), predicted affected areas\n"
                "  - Closed roads/streets/highways, Roads/streets/highways to avoid, safety instructions, damage progress reports, relief progress reports\n"
                "Output format should be in JSON:"
                '{"1":"[Tweet 1]",...}\n'
                "No Duplicates. No explanation. No added text, just JSON.\n"
                "Generate {N} tweets"
            )

    ## Prompt for tweets not talking about crisis with boostrapping random topics
    if args.prompt_type == "nones-random-topics":
        random_topics = ["politics","celebrity","cancer","music","sports","food","lifestyle","memes","breaking news","personal updates","tourism"]
        prompt = (
                "This is the {BATCH} time sending this prompt."
                "You are generating synthetic social media posts (tweets) written by anyone. "
                "On the following random topics and any other topic you can think of:\n"
                f"  -{', '.join(random.sample(random_topics, 5))}\n"
                "Don't include posts about wildfires and flood crisis."
                "Write the posts in {LANGUAGE} language.\n"
                "Output format should be in JSON:"
                '{"1":"[Tweet 1]",...}\n'
                "No Duplicates. No explanation. No added text, just JSON.\n"
                "Generate {N} tweets"
            )
    
    ## Prompt for tweets talking about crisis, but focused on asking for donations, seeking sympathy, blaming the government and conspiracy theories
    if args.prompt_type == "nones-donations-sympathy":
        prompt = (
                "This is the {BATCH} time sending this prompt."
                "You are generating synthetic social media posts (tweets) written by anyone during a flood or wildfire crisis. "
                "Generate only posts asking for donations, expressing sympathy, blaming the government/politicians, or making up conspiracy theories."
                "Write the posts in {LANGUAGE} language.\n"
                "Output format should be in JSON:"
                '{"1":"[Tweet 1]",...}\n'
                "No Duplicates. No explanation. No added text, just JSON.\n"
                "Generate {N} tweets"
            )    

    final_prompt = []
                            
 
    final_prompt.append({"role": "user",
                         "content": (prompt.replace("{BATCH}", batch)
                                     .replace("{CRISIS_TYPE}", crisis_type)
                                     .replace("{LANGUAGE}", language)
                                     .replace("{KEYWORDS}", CRISIS_KEYWORDS[crisis_type][language])
                                     .replace("{N}", no_tweets))},
                        )


    res = LLM.create_chat_completion(final_prompt, **MODEL_CONFIG["generation_params"]) 
    response = res["choices"][0]["message"]["content"]
    
    response_dict = extract_response(response)

    jsonl_list = []
    for v in response_dict.values(): 
        jsonl_list.append({"tweet_text": f"{v}"})
        

    if args.prompt_type in ['nones-random-topics','nones-donations-sympathy']:
        jsonl_path = os.path.join(OUT_DIR, f"{language}_{no_tweets}_{args.prompt_type}_{args.batch_no}.jsonl") 
    else:
        jsonl_path = os.path.join(OUT_DIR, f"{language}_{crisis_type}_{no_tweets}_{args.prompt_type}_{args.batch_no}.jsonl")
          
    with open(jsonl_path, mode='w', encoding='utf-8') as jsonl_file:
        for row in jsonl_list:
            jsonl_file.write(json.dumps(row) + '\n')
            

if __name__ == "__main__":
    
    langauge_choices = ["English","Spanish","Catalan","German"]
    event_choices = ["flood","wildfire"]
    prompts_choice = ["nones-random-topics","with-keywords","gov-alerts","nones-donations-sympathy"]
    
    parser = argparse.ArgumentParser(description="Generate synthetic crisis tweets.")
    parser.add_argument("--language", nargs='?', choices=langauge_choices, default="English", help=f"Language in tweets. supported options: {', '.join(langauge_choices)}")
    parser.add_argument("--crisis-type", nargs='?', choices=event_choices, default="flood", help=f"Crisis type. supported options: {', '.join(event_choices)}")
    parser.add_argument("--prompt-type", nargs='?', choices=prompts_choice, default="with-keywords", help=f"Crisis type. supported options: {', '.join(prompts_choice)}")
    parser.add_argument("--no-tweets", type=int, default=100, help="Number of tweets to generate")
    parser.add_argument("--batch-no", type=int, default=1, help="Batch number. Use if generating multiple batches")

    args = parser.parse_args()
    
    main(args=args)