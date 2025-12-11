import os
import sys
import subprocess
from datetime import date
current = os.path.dirname(os.path.realpath(__file__))
parent = os.path.dirname(current)
sys.path.append(parent)
from sub.common_logger import setup_logging


logger = setup_logging(name=os.path.splitext(os.path.basename(__file__))[0])

# from - https://docs.x.com/x-api/enterprise-gnip-2.0/fundamentals/search-api#building-search-queries
X_LANG_MAP = {"Amharic": "am", "Arabic": "ar", "Armenian": "hy", "Basque": "eu", "Bengali": "bn",  
"Bosnian": "bs", "Bulgarian": "bg", "Burmese": "my", "Catalan": "ca", "Croatian": "hr", "Czech": "cs", 
"Danish": "da", "Dutch": "nl", "English": "en", "Estonian": "et", "Finnish": "fi", "French": "fr", 
"Georgian": "ka", "German": "de", "Greek": "el", "Gujarati": "gu", "Haitian Creole": "ht", "Hebrew": "iw", 
"Hindi": "hi", "Hungarian": "hu", "Icelandic": "is", "Indonesian": "in", "Italian": "it", "Japanese": "ja", 
"Kannada": "kn", "Khmer": "km", "Korean": "ko", "Lao": "lo", "Latinized Hindi": "hi-Latn", "Latvian": "lv", 
"Lithuanian": "lt", "Malayalam": "ml", "Maldivian": "dv", "Marathi": "mr", "Nepali": "ne", "Norwegian": "no", 
"Oriya": "or", "Panjabi": "pa", "Pashto": "ps", "Persian": "fa", "Polish": "pl", "Portuguese": "pt", "Romanian": "ro", 
"Russian": "ru", "Serbian": "sr", "Simplified Chinese": "zh-CN", "Sindhi": "sd", "Sinhala": "si", "Slovak": "sk",
"Slovenian": "sl", "Sorani Kurdish": "ckb", "Spanish": "es", "Swedish": "sv", "Tagalog": "tl", "Tamil": "ta",
"Telugu": "te", "Thai": "th", "Tibetan": "bo", "Traditional Chinese": "zh-TW", "Turkish": "tr", "Ukrainian": "uk",
"Urdu": "ur", "Uyghur": "ug", "Vietnamese": "vi", "Welsh": "cy"}


class CrawlerManager:
    def __init__(self):
        self.processes = {}  # {crawl_id: subprocess_instance}

    def start_process(self, crawl_id, command_data):
        if crawl_id in self.processes:
            print(f"Process {crawl_id} is already running.")
            logger.info(f"Process {crawl_id} is already running.")
            return

        if crawl_id.strip() == "":
            print(f"Blank crawl ID.")
            logger.info(f"Blank crawl ID.")
            return

        extra_args = []
        if command_data["crawl_params.lang"] == "None": 
            extra_args = []
        else:
            extra_args = ["--lang", X_LANG_MAP[command_data["crawl_params.lang"]]] 
            
            
        args = ["python3", "src/crawler_kafka.py",
            "--username", command_data["crawl_params.username"],
            "--password", command_data["crawl_params.password"],
            "--geocode", f'{ command_data["crawl_params.lat"] },{ command_data["crawl_params.lon"] },{ command_data["crawl_params.radius"] }km', 
            "--email", command_data["crawl_params.email"],
            "--emailpass", command_data["crawl_params.emailpass"],
            "--day", date.today().strftime("%Y-%m-%d"),
            "--identifier", command_data["crawl_id"]
        ] + extra_args
        
        with open(f"logs/stdouterr_{command_data['crawl_id']}.log", "a") as log_file:
            process = subprocess.Popen(args, stdout=log_file, stderr=log_file)
        self.processes[crawl_id] = process
        print(f"Started process {crawl_id} with PID {process.pid}")
        logger.info(f"Started process {crawl_id} with PID {process.pid}")
        

    def kill_process(self, crawl_id):
        if crawl_id in self.processes:
            self.processes[crawl_id].terminate()
            self.processes[crawl_id].wait()
            del self.processes[crawl_id]
            print(f"Killed process {crawl_id}")
            logger.info(f"Killed process {crawl_id}")
        else:
            print(f"Process {crawl_id} not found.")
            logger.info(f"Process {crawl_id} not found.")
