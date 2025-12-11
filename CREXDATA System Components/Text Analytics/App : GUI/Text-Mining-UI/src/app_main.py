import os
import sys
import time
import json
import pprint
import datetime
import requests
import gradio as gr
import pandas as pd
import plotly.express as px

from confluent_kafka import Consumer, Producer, TopicPartition

import utils
from genra_utils import GenraPipeline

sys.path.append(os.path.dirname(os.path.dirname(os.path.realpath(__file__))))
from config import CREXDATA_MAIN_CONFIG, QA_CONFIG, MODELS_DIR, ROOT_DIR




if QA_CONFIG["use_aihub"]:
    LLM = None
else:
    if gr.NO_RELOAD:
        import llama_cpp
        model_path = os.path.join(MODELS_DIR, QA_CONFIG["llm_model"])
        LLM = llama_cpp.Llama(model_path=model_path,
                            n_ctx=8192, 
                            use_mmap=False)


CONSUMER = None
PRODUCER = None
QA_CONSUMER = None
LATENCY_CONSUMER = None
KAFKA_BASE_CONFIG = {}
LATENCY_HISTORY = []


loadTwitterWidgets_js = """
    async () => {
        // Load Twitter Widgets script
        const script = document.createElement("script");
        script.onload = () => console.log("Twitter Widgets.js loaded");
        script.src = "https://platform.twitter.com/widgets.js";
        document.head.appendChild(script);
        // Define a global function to reload Twitter widgets
        globalThis.reloadTwitterWidgets = () => {
            // Reload Twitter widgets
            if (window.twttr && twttr.widgets) {
                twttr.widgets.load();
            }
                
        };
    }
"""


def cleanup():
    consumers = [CONSUMER, QA_CONSUMER, LATENCY_CONSUMER]    
    for c in consumers:
        if c is None:
            pass
        else:
            c.close()
        
   
def kafka_setup(kafka_server_dropdown, kafka_server, kafka_input_topic, kafka_output_topic, show_latency_plot, kafka_processed_topic):
    global CONSUMER, PRODUCER, QA_CONSUMER, KAFKA_BASE_CONFIG, LATENCY_CONSUMER
    
    if kafka_input_topic == "" or kafka_output_topic == "":
        raise gr.Error(message="Enter both the read and write topic! Refresh page and fill details correctly.", duration=5)
    
    if kafka_server_dropdown == "crexdata-auth":
        KAFKA_BASE_CONFIG = {}
        KAFKA_BASE_CONFIG.update(CREXDATA_MAIN_CONFIG)
        
    if kafka_server_dropdown == "crexdata-unauth": 
        KAFKA_BASE_CONFIG = {}
        if kafka_server == "":
             raise gr.Error(message="Enter local kafka server! E.g. 127.0.0.1:9092. Refresh page and fill details correctly.", duration=5)
        KAFKA_BASE_CONFIG.update({'bootstrap.servers': kafka_server})
    
    consumer_conf = KAFKA_BASE_CONFIG.copy()
    consumer_conf.update({'group.id': 'relevance_gui_client',
                          'auto.offset.reset': 'earliest',
                          'enable.auto.commit': False})        
    CONSUMER = Consumer(consumer_conf)
    CONSUMER.subscribe([kafka_input_topic], on_assign=utils.print_assignment)
    PRODUCER = Producer(**KAFKA_BASE_CONFIG)
    
    qa_consumer_conf = KAFKA_BASE_CONFIG.copy()
    qa_consumer_conf.update({'group.id': 'qa_gui_client',
                          'auto.offset.reset': 'earliest',
                          'enable.auto.commit': False})
    QA_CONSUMER = Consumer(qa_consumer_conf)
    
    if show_latency_plot:
        la_consumer_conf = KAFKA_BASE_CONFIG.copy()
        la_consumer_conf.update({'group.id': 'latency_consumer_client',
                            'auto.offset.reset': 'latest',
                            'enable.auto.commit': True})        
        LATENCY_CONSUMER = Consumer(la_consumer_conf)
        LATENCY_CONSUMER.subscribe([kafka_processed_topic], on_assign=utils.print_assignment)
        plot_update_dict = {"visible": True} 
    else:
        plot_update_dict = {"visible": False}

    return gr.update(interactive=False), gr.update(interactive=False), gr.update(interactive=False), \
        gr.update(interactive=False), gr.update(interactive=False), gr.update(interactive=True), \
            gr.update(interactive=True), gr.update(interactive=False), gr.update(interactive=True), \
                gr.update(interactive=True), gr.update(interactive=False), gr.update(interactive=False), \
                    gr.update(interactive=True),gr.update(interactive=True),gr.update(interactive=True), \
                        gr.update(value="""<div style="text-align: center; color: green; font-weight: bold; font-size: 1.2em;">✅ Connected to Kafka</div>"""), \
                            gr.update(interactive=False), gr.update(interactive=False), gr.update(**plot_update_dict), gr.update(**plot_update_dict)
        
        
def kafka_reader(pollnum, polltimeout, number_consumed): 
    msgs_dicts = [] 
    messages = CONSUMER.consume(num_messages=pollnum, timeout=polltimeout)
    for msg in messages:
        if msg is None:
            print("No msg")
            continue
        if msg.error():
            print(f"Consumer error: {msg.error()}")
            continue
        else:
            print(f"Received message: {msg.value().decode('utf-8')},\n Timestamp: {msg.timestamp()}")
            msgs_dicts.append(utils.msg_processing(msg, case='loads'))
        
    if not msgs_dicts:
        df = pd.DataFrame()
    else:
        df = pd.DataFrame.from_records(msgs_dicts)    
        
    if not df.empty:
        df = df.rename_axis('index').reset_index()
        df = df.drop(columns=["msg_created_timestamp", "flink_consumer_lag_ms", "latency_ms", "processing_time_ms"], errors="ignore")
    
    msg_indices = gr.CheckboxGroup(label="Select tweet index to send to ARGOS",show_label=True,choices=[x for x in df.index.to_list()],value=None) 
    
    number_consumed += len(df.index)
    return df, msg_indices, gr.update(choices=[utils.msg_processing(m, case='dumps') for m in msgs_dicts], value=[utils.msg_processing(m, case='dumps') for m in msgs_dicts]), \
        gr.update(interactive=True), gr.update(value=number_consumed), gr.update(value=f"""<h1 style="text-align:center">{number_consumed}</h1>""")
                
        
def kafka_produce_to_argos(indices, msgs, topic):
    if indices:
        msgs_to_produce = [msgs[i] for i in indices]
        
        for msg in msgs_to_produce:
            try:
                PRODUCER.produce(topic, msg, callback=utils.delivery_callback) 
            except BufferError:
                print(f'Local producer queue is full ({len(PRODUCER)} messages awaiting delivery): try again\n')
            PRODUCER.poll(0)

        print(f'%% Waiting for {len(PRODUCER)} deliveries\n')
        gr.Info(f"Waiting for {len(PRODUCER)} deliveries", duration=5)
        p_chk = PRODUCER.flush()
        
        if p_chk == 0:
            gr.Info(f"All messages saved", duration=5)
            print(f"Messages left: {p_chk}")
        
        ## NOTE ADD CONSUMER COMMIT
        CONSUMER.commit(asynchronous=False)
        return gr.update(interactive=False)
    else:
        gr.Warning(f"No Indices selected to save. If you don't wish to save any messages, click 'Get from topic'", duration=10)
        return gr.update(interactive=True)



    

## QA Functions -----------------------------------------------------------------------------------------------------

def qa_get_posts(qa_num_msgs,kafka_qa_input_topic,qa_event_id):
    msgs_dicts = []    
    messages = utils.consume_latest_messages(QA_CONSUMER,kafka_qa_input_topic,qa_num_msgs)
    for msg in messages:
        if qa_event_id.strip() == "":
            msgs_dicts.append(utils.msg_processing(msg, case='loads'))
        else: 
            if utils.msg_processing(msg, case='loads')["event_identifier"] == qa_event_id:
                msgs_dicts.append(utils.msg_processing(msg, case='loads'))
        
    if not msgs_dicts:
        df = pd.DataFrame()
    else:
        df = pd.DataFrame.from_records(msgs_dicts)
        
    if not df.empty:
        df = df.rename_axis('index').reset_index()
        df['index'] = df['index'] + 1
        df = df.drop(columns=["msg_created_timestamp", "flink_consumer_lag_ms", "latency_ms", "processing_time_ms"], errors="ignore")
        
    return df, gr.update(interactive=True)



def genra_rag(tweets_lst, queries_lst, qa_query_type, qa_response_lang):
    topk = 5
    batch_size = 30 # size of batch in genra process, if tweets for QA greater than this, genra runs in 2 or more batches
    aggregator = 'linear'

    contexts = []

    queries_df = pd.DataFrame({'id':[j for j in range(len(queries_lst))],'query': queries_lst})
    
    tweets_df = pd.DataFrame({'text': tweets_lst})
    tweets_df.reset_index(inplace=True)
    tweets_df.rename(columns={"index": "order"},inplace=True)

    gr.Info("Loading GENRA pipeline....")
    genra = GenraPipeline(LLM, 
                          os.path.join(MODELS_DIR, QA_CONFIG["embedder_model"]), 
                          aggregator, contexts, qa_response_lang)
    gr.Info("Waiting for data...")
    batches = [tweets_df[i:i+batch_size] for i in range(0,len(tweets_df),batch_size)]
    
    if qa_query_type == "query":
        summarize_batch = True
        for batch_number, tweets in enumerate(batches):
            gr.Info(f"Populating index for batch {batch_number}")
            genra.qa_indexer.index_dataframe(tweets)
            gr.Info(f"Performing retrieval for batch {batch_number}")
            genra.retrieval(batch_number, queries_df, topk, summarize_batch) 
        
        gr.Info("Processed all batches!")
        gr.Info("Preparing results...")
        results = genra.answers_store
        final_answers = []
        for q, g_answers in results.items():
            for answer in g_answers:
                final_answers.append({'question':q, "tweets":answer['tweets'], "batch":answer['batch_number'], "summary":answer['summary'] }) 
        return final_answers
    
    elif qa_query_type == "summarise":
        gr.Info("Getting summary...")
        summary = genra.summarize_no_query(tweets_lst)
        return summary
        

  
def qa_run(qa_query, qa_df, qa_query_type, qa_response_lang, qa_type):
    
    if qa_query.strip() == "" and qa_query_type != "summarise":
        qa_response = "Please, Ask your question about the incident."
    else:
        if qa_df.empty:
            qa_response = "No context messages provided, Check that the QA input kafka topic has social media messages."
            gr.Info("No context messages provided, Check that the QA input kafka topic has social media messages.", duration=5)
        else:
            start_time = time.time()   
            df = qa_df.copy()  
            context_list = df["tweet_text"].to_list()

            if qa_type == "no-RAG":
                template_texts =""
                for i, text in enumerate(context_list):
                    template_texts += f'{i+1}. {text} \n'
                    
                if qa_query_type == "summarise":
                    conversation = [{"role": "user", "content": 
                                    "The documents below describe a developing disaster event. "
                                    "Based on these documents, write a brief summary in the form of a paragraph, " 
                                    "highlighting the most crucial information. "
                                    f"Reply ONLY in {qa_response_lang} regardless of the language in the documents.\n"
                                    f"Documents: {template_texts.strip()}"}]
                else:
                    conversation = [{"role": "user", "content": 
                                    "For the following query and documents, "
                                    "try to answer the given query using only the documents. "
                                    "If the answer is not in the documents, state that. "
                                    "Refer to documents in your response using their numbers. "
                                    f"Reply ONLY in {qa_response_lang} regardless of the language in the documents. "
                                    f"\nQuery:\n{qa_query.strip()}\nDocuments:\n{template_texts.strip()}"}]
                    
                LLM.reset()
                res = LLM.create_chat_completion(conversation, temperature=0.7, top_p=0.95, top_k=10, min_p=0.05, repeat_penalty=1.1, max_tokens=4096) 
                qa_response = res["choices"][0]["message"]["content"]

            
            elif qa_type == "Genra-RAG":   
                response = genra_rag(context_list, [qa_query.strip()], qa_query_type, qa_response_lang)
                if isinstance(response, str):
                    qa_response = response
                else:
                    for res in response:
                        tweets_dict = {}
                        for i, tweet in enumerate(res['tweets']):
                            tweets_dict[f'Document {i+1}'] = {"index": f"{context_list.index(tweet[0])+1}", "text":f"{tweet[0]}", "score":f"{tweet[1]}"}
                        res['tweets'] = tweets_dict
                        res['summary'] = " ".join(res['summary'].replace('"',"'").split())
                    qa_response = json.dumps(response, indent=4, ensure_ascii=False, sort_keys=True)
                
            end_time = time.time()
            runtime = end_time - start_time
            print(f"QA ran for: {runtime}")
        
    return qa_response


def qa_run_aihub(qa_query, qa_df, qa_query_type, qa_response_lang, qa_type):
    
    if qa_query.strip() == "" and qa_query_type != "summarise":
        qa_response = "Please, Ask your question about the incident."
    else:
        if qa_df.empty:
            qa_response = "No context messages provided, Check that the QA input kafka topic has social media messages."
            gr.Info("No context messages provided, Check that the QA input kafka topic has social media messages.", duration=5)
        else:
            start_time = time.time()    
            df = qa_df.copy()  
            context_list = df["tweet_text"].to_list()
      
            if qa_type == "no-RAG":
                template_texts =""
                for i, text in enumerate(context_list):
                    template_texts += f'{i+1}. {text} \n'
                    
                if qa_query_type == "summarise":
                    conversation = [{"role": "user", "content": 
                                    "The documents below describe a developing disaster event. "
                                    "Based on these documents, write a brief summary in the form of a paragraph, " 
                                    "highlighting the most crucial information. "
                                    f"Reply ONLY in {qa_response_lang} regardless of the language in the documents.\n"
                                    f"Documents: {template_texts.strip()}"}]
                else:
                    conversation = [{"role": "user", "content": 
                                    "For the following query and documents, "
                                    "try to answer the given query using only the documents. "
                                    "If the answer is not in the documents, state that. "
                                    "Refer to documents in your response using their numbers. "
                                    f"Reply ONLY in {qa_response_lang} regardless of the language in the documents. "
                                    f"\nQuery:\n{qa_query.strip()}\nDocuments:\n{template_texts.strip()}"}]
                    
                                
                url = (f"{QA_CONFIG['aihub_params']['endpoint_url']}"
                    f"min_p={QA_CONFIG['aihub_params']['min_p']}&"
                    f"model={QA_CONFIG['aihub_params']['model']}&"
                    f"top_k={QA_CONFIG['aihub_params']['top_k']}&"
                    f"top_p={QA_CONFIG['aihub_params']['top_p']}&"
                    f"max_tokens={QA_CONFIG['aihub_params']['max_tokens']}&"
                    f"temperature={QA_CONFIG['aihub_params']['temperature']}&"
                    f"repeat_penalty={QA_CONFIG['aihub_params']['repeat_penalty']}")
                
                headers = {
                    "Content-Type": "application/json",
                    "Authorization": f"apitoken {QA_CONFIG['aihub_apitoken']}" 
                    }
                data = {"data":[{"prompt": conversation[0]["content"]}]}
                
                response = requests.post(url, headers=headers, json=data)
                
                print(f"AIHUB response code: {response.status_code}")
                if f"{response.status_code}" == "200":
                    response_json = response.json()
                    qa_response = response_json["data"][0]["qa_response"]
                else:
                    gr.Warning("Something wrong with AIHUB endpoint. Retry again.", duration=5)
                    response_json = response.json()
                    qa_response = f"AIHUB response code: {response.status_code} \n\n {response_json}"
            
            elif qa_type == "Genra-RAG":   
                response = genra_rag(context_list, [qa_query.strip()], qa_query_type, qa_response_lang)
                if isinstance(response, str):
                    qa_response = response
                else:
                    for res in response:
                        tweets_dict = {}
                        for i, tweet in enumerate(res['tweets']):
                            tweets_dict[f'Document {i+1}'] = {"index": f"{context_list.index(tweet[0])+1}", "text":f"{tweet[0]}", "score":f"{tweet[1]}"}
                        res['tweets'] = tweets_dict
                        res['summary'] = " ".join(res['summary'].replace('"',"'").split()) 
                    qa_response = json.dumps(response, indent=4, ensure_ascii=False, sort_keys=True)
    
            end_time = time.time()
            runtime = end_time - start_time
            print(f"QA ran for: {runtime}")
        
    return qa_response

## -------------------------------------------------------------------------------------------------------------------
## CUI Functions -----------------------------------------------------------------------------------------------------

def start_crawl(kafka_commands_topic, cui_crawl_id, cui_xusername, cui_xemail, cui_xpassword,
                cui_emailpass, cui_crawl_lang, cui_loc_lat, cui_loc_lon, cui_loc_rad):
    if cui_crawl_id.strip() == "":
        raise gr.Error("Enter a Crawl Identifier in the text box. Refresh page and fill details correctly.", duration=5)
    else:
        msg_dict = {"crawl_id":cui_crawl_id,  
                    "command":"start",  
                    "crawl_params.username":cui_xusername,
                    "crawl_params.password":cui_xpassword,
                    "crawl_params.email":cui_xemail,
                    "crawl_params.emailpass":cui_emailpass,
                    "crawl_params.lat":cui_loc_lat,
                    "crawl_params.lon":cui_loc_lon,
                    "crawl_params.radius":cui_loc_rad,
                    "crawl_params.lang":cui_crawl_lang}
        try:
            PRODUCER.produce(kafka_commands_topic, utils.msg_processing(msg_dict, case='dumps'), callback=utils.delivery_callback) 
        except BufferError:
            print(f'Local producer queue is full ({len(PRODUCER)} messages awaiting delivery): try again\n')
        PRODUCER.poll(0)
        p_chk = PRODUCER.flush()
    
        if p_chk == 0:
            gr.Info(f"Crawl start command sent", duration=5)
        
        
def kill_crawl(kafka_commands_topic, cui_killcrawl_id):
    if cui_killcrawl_id.strip() == "":
        raise gr.Error("Enter a valid Crawl Identifier to kill in the text box. Refresh page and fill details correctly.", duration=5)
    else:
        msg_dict = {"crawl_id":cui_killcrawl_id,  
                    "command":"kill",  
                    "crawl_params.username":None,
                    "crawl_params.password":None,
                    "crawl_params.email":None,
                    "crawl_params.emailpass":None,
                    "crawl_params.lat":None,
                    "crawl_params.lon":None,
                    "crawl_params.radius":None,
                    "crawl_params.lang":None}
        try:
            PRODUCER.produce(kafka_commands_topic, utils.msg_processing(msg_dict, case='dumps'), callback=utils.delivery_callback) 
        except BufferError:
            print(f'Local producer queue is full ({len(PRODUCER)} messages awaiting delivery): try again\n')
        PRODUCER.poll(0)
        p_chk = PRODUCER.flush()
        
        if p_chk == 0:
            gr.Info(f"Crawl kill command sent", duration=5)
        
## -------------------------------------------------------------------------------------------------------------------
## DEBUG Functions ---------------------------------------------------------------------------------------------------
        
def get_console1_value(kafka_crawl_logs_topic, cui_tail_console1):
    """
    Displays logs of crawler_manager
    """
    if kafka_crawl_logs_topic.strip() != "":
        consumer_conf = KAFKA_BASE_CONFIG.copy()
        consumer_conf.update({'group.id': 'log_consume_group',
                              'auto.offset.reset': 'earliest'}) 
        consumer = Consumer(consumer_conf)
        try:
            msgs = utils.consume_latest_messages(consumer,kafka_crawl_logs_topic,cui_tail_console1)
            decoded_msgs = [msg.value().decode('utf-8') for msg in msgs \
                if "- crawler_manager -" in msg.value().decode('utf-8')] 
                #if "- crawler_instantiator_kafka -" in msg.value().decode('utf-8') or "- crawler_manager -" in msg.value().decode('utf-8')]
            if not decoded_msgs:
                return "Logs topic is empty"
            return "\n".join(decoded_msgs) 
        except Exception as ex:
            return f"Something went wrong. Error: {ex}"
        finally:
            consumer.close()
    else:
        return "Set kafka_crawl_logs_topic value"
    

def get_console2_value(kafka_crawl_logs_topic,cui_tail_console2,cui_console2_id):
    """
    Displays all crawl logs with filters
    """    
    if kafka_crawl_logs_topic.strip() != "":
        consumer_conf = KAFKA_BASE_CONFIG.copy()
        consumer_conf.update({'group.id': 'log_consume_group',
                            'auto.offset.reset': 'earliest'}) 
        consumer = Consumer(consumer_conf)
        if cui_console2_id.strip() == "":
            try:
                msgs = utils.consume_latest_messages(consumer,kafka_crawl_logs_topic,cui_tail_console2)
                decoded_msgs = [msg.value().decode('utf-8') for msg in msgs]
                if not decoded_msgs:
                    return "Logs topic is empty"
                return "\n".join(decoded_msgs) 
            except Exception as ex:
                return f"Something went wrong. Error: {ex}"
            finally:
                consumer.close()
        else:
            try:
                msgs = utils.consume_latest_messages(consumer,kafka_crawl_logs_topic,cui_tail_console2)
                decoded_msgs = [msg.value().decode('utf-8') for msg in msgs if f"- {cui_console2_id} -" in msg.value().decode('utf-8')]
                if not decoded_msgs:
                    return "Logs topic is empty"
                return "\n".join(decoded_msgs) 
            except Exception as ex:
                return f"Something went wrong. Error: {ex}"
            finally:
                consumer.close()
    else:
        return "Set kafka_crawl_logs_topic value"

    
def get_latest_offset_and_timestamp(topic):

    if not topic.strip():
        return gr.update(value=f"""<h1 style="text-align:center">NaN</h1>"""), gr.update(value=f"""<h1 style="text-align:center">N/A</h1>"""), {} 

    try:
        consumer_conf = KAFKA_BASE_CONFIG.copy()
        consumer_conf.update({'group.id': 'topic_stats_group',
                            'auto.offset.reset': 'latest',
                            'enable.auto.commit': False}) 
        consumer = Consumer(consumer_conf)
        md = consumer.list_topics(topic=None, timeout=5)

        if topic not in md.topics:
            consumer.close()
            return gr.update(value=f"""<h1 style="text-align:center">Topic Not in Cluster</h1>"""), gr.update(value=f"""<h1 style="text-align:center">N/A</h1>"""), {} 

        partitions = md.topics[topic].partitions.keys()

        total_latest_offset = 0
        latest_timestamp = 0
        latest_msgs = []
        msg_dict = {}

        for p in partitions:
            tp = TopicPartition(topic, p)
            low, high = consumer.get_watermark_offsets(tp, timeout=5)
            true_high = high - low
            total_latest_offset += true_high

            # Fetch message at high-1 to get its timestamp
            if high > 0:
                fetch_tp = TopicPartition(topic, p, high - 1)
                consumer.assign([fetch_tp])
                msg = consumer.poll(timeout=2)
                if msg and msg.timestamp()[1] is not None:
                    ts = msg.timestamp()[1]
                    latest_timestamp = max(latest_timestamp, ts)
                    latest_msgs.append((ts,utils.msg_processing(msg, case='loads')))

        if len(latest_msgs) != 0:             
            latest_msgs_sorted = sorted(latest_msgs, key=lambda x: x[0], reverse=True)
            msg_dict = latest_msgs_sorted[0][1]
        
        consumer.close()

        if latest_timestamp > 0:
            dt = datetime.datetime.fromtimestamp(latest_timestamp / 1000.0)
            timestamp_str = dt.strftime("%Y-%m-%d %H:%M:%S")
        else:
            timestamp_str = "N/A"

        return gr.update(value=f"""<h1 style="text-align:center">{total_latest_offset}</h1>"""), gr.update(value=f"""<h1 style="text-align:center">{timestamp_str}</h1>"""), msg_dict

    except Exception as e:
        print(f"Error: {e}")
        return gr.update(value=f"""<h1 style="text-align:center">NaN</h1>"""), gr.update(value=f"""<h1 style="text-align:center">N/A</h1>"""), {} 
    
       
def get_latency_stats():
    global LATENCY_HISTORY
    
    def append_to_history(now_time, prs_time, lag_time, lcy_time):
        LATENCY_HISTORY.append({
            "wall_time": now_time,
            "processing_time_ms": prs_time,
            "lag_ms": lag_time,
            "latency_ms": lcy_time})
        
    now = int(time.time() * 1000)    
    if LATENCY_CONSUMER == None:
        return None
    else:
        msg = LATENCY_CONSUMER.poll(timeout=1.0)
        if msg is None:
            pass
        else:
            if msg.error():
                pass
            else:
                try:
                    msg_dict = utils.msg_processing(msg, case='loads')
                    append_to_history(now, msg_dict['processing_time_ms'], msg_dict['flink_consumer_lag_ms'], msg_dict['latency_ms'])
                except KeyError:
                    pass
                
        LATENCY_HISTORY = LATENCY_HISTORY[-50:]
        
        if len(LATENCY_HISTORY) == 0:
            return None
        
        df = pd.DataFrame(LATENCY_HISTORY)
        df["wall_time"] = pd.to_datetime(df["wall_time"], unit="ms").dt.strftime("%H:%M:%S")#("%b %d, %Y %H:%M:%S")
        df["Processing Time (s)"] = df['processing_time_ms'] / 1000
        df["Flink Lag (s)"] = df['lag_ms'] / 1000
        df["Latency (s)"] = df['latency_ms'] / 1000
        fig_line = px.line(
            df,
            x="wall_time",
            y=["Processing Time (s)", "Flink Lag (s)", "Latency (s)"],
            labels={"value": "Seconds", "wall_time": "Time (HH:MM:SS)"},
            title="Streaming Metrics",
            line_shape="vh"
        )
        return fig_line
    
## -------------------------------------------------------------------------------------------------------------------


STOCK_EXAMPLES = {
    "German": [["Welche Straßen und Orte werden besonders häufig erwähnt? Wie sieht die Situation dort aus?"], 
               ["Gibt es Hinweise auf gefährdete Infrastruktur, die betroffen sein könnte?"], 
               ["Wo häufen sich Berichte über nicht mehr passierbare Straßen oder Unterführungen?"],
               ["Welche Beiträge deuten auf akute Gefahr für Menschenleben hin?"],
               ["Wo berichten Menschen über Wasser im Wohnbereich, nicht nur im Keller?"],
               ["Findest du Hinweise auf eingeschlossene Personen (z. B. Tiefgarage, Aufzug, Auto)?"]],
    "English": [["Which streets and locations are mentioned most frequently? What is the situation like there?"],
                ["Is there evidence of vulnerable infrastructure that could be affected?"],
                ["Where are reports of impassable streets or underpasses occurring more frequently?"],
                ["Which posts indicate an acute danger to human life?"],
                ["Where do people report water in their living areas, not just in the basement?"],
                ["Do you find evidence of trapped people (e.g., underground parking garage, elevator, car)?"]],
    "Spanish": [["¿Qué calles y lugares se mencionan con más frecuencia? ¿Cuál es la situación allí?"],
                ["¿Hay evidencia de infraestructura vulnerable que pueda verse afectada?"],
                ["¿Dónde se reportan con mayor frecuencia calles o pasos subterráneos intransitables?"],
                ["¿Qué publicaciones indican un peligro grave para la vida humana?"],
                ["¿Dónde reportan las personas agua en sus viviendas, no solo en el sótano?"],
                ["¿Se encuentran evidencias de personas atrapadas (por ejemplo, en un estacionamiento subterráneo, un ascensor, un automóvil)?"]],
    "Catalan": [["Quins carrers i llocs es mencionen amb més freqüència? Quina és la situació allà?"],
                ["Hi ha evidència d'infraestructures vulnerables que es puguin veure afectades?"],
                ["On es produeixen més freqüentment informes de carrers o passos subterranis intransitables?"],
                ["Quins pals indiquen un perill greu per a la vida humana?"],
                ["On informa la gent que hi ha aigua a les seves zones d'habitatge, no només al soterrani?"],
                ["Trobeu evidència de persones atrapades (per exemple, aparcament subterrani, ascensor, cotxe)?"]],
    }

def set_qa_textbox(query_type, lang):
    if query_type == "query":
        return gr.update(value="",visible=True), gr.Dataset(samples=STOCK_EXAMPLES[lang]) 
    elif query_type == "summarise":
        return gr.update(value="",visible=False), gr.Dataset(samples=[]) 
    
    
## -------------------------------------------------------------------------------------------------------------------


# BLOCKS START ---------------------------------------------------------------------------------------------------
            
with gr.Blocks(title="Crexdata Text Mining",
               fill_width=True,
               theme=gr.themes.Default(spacing_size="sm", text_size="sm",primary_hue="teal")) as demo: 
    
    demo.load(None,None,None,js=loadTwitterWidgets_js)
    
    X_LANG_MAP = ["Amharic", "Arabic", "Armenian", "Basque", "Bengali", "Bosnian", "Bulgarian", "Burmese", "Catalan", 
                  "Croatian", "Czech", "Danish", "Dutch", "English", "Estonian", "Finnish", "French", "Georgian", 
                  "German", "Greek", "Gujarati", "Haitian Creole", "Hebrew", "Hindi", "Hungarian", "Icelandic", 
                  "Indonesian", "Italian", "Japanese", "Kannada", "Khmer", "Korean", "Lao", "Latinized Hindi", 
                  "Latvian", "Lithuanian", "Malayalam", "Maldivian", "Marathi", "Nepali", "Norwegian", "Oriya", 
                  "Panjabi", "Pashto", "Persian", "Polish", "Portuguese", "Romanian", "Russian", "Serbian", 
                  "Simplified Chinese", "Sindhi", "Sinhala", "Slovak", "Slovenian", "Sorani Kurdish", "Spanish", 
                  "Swedish", "Tagalog", "Tamil", "Telugu", "Thai", "Tibetan", "Traditional Chinese", "Turkish", 
                  "Ukrainian", "Urdu", "Uyghur", "Vietnamese", "Welsh"]
        
    with gr.Sidebar():
        gr.Image(value=os.path.join(ROOT_DIR, 'crexdata_icons', 'color.svg'), 
                       container=False,
                       show_download_button=False,
                       interactive=False,
                       show_fullscreen_button=False)
        gr.Markdown(
            """
            ## Mandatory Kafka Setup ⚠️
            Enter kafka server details and click connect kafka
            """
            )
        kafka_server_dropdown = gr.Dropdown(label="Choose server",
                                                choices=["crexdata-auth","crexdata-unauth"],
                                                value="crexdata-unauth",
                                                show_label=True)
        kafka_server = gr.Textbox(label="Bootstrap server",value="server.crexdata.eu:9192",visible=True)  
        kafka_input_topic = gr.Textbox(label="kafka topic with relevant tweets", value="text_mining_processed_relevant_tweets_stream")
        kafka_output_topic = gr.Textbox(label="kafka topic sending to ARGOS", value="text_mining_to_argos_stream")   
        kafka_qa_input_topic = gr.Textbox(label="kafka topic with relevant tweets for QA", value="text_mining_processed_relevant_tweets_stream")
        kafka_commands_topic = gr.Textbox(label="kafka topic crawler commands", value="text_mining_crawler_commands",interactive=True)
        kafka_crawl_logs_topic = gr.Textbox(label="kafka topic with crawler logs", value="text_mining_crawler_logs",interactive=True)
        kafka_processed_topic = gr.Textbox(label="kafka topic with processed tweets for latency", value="text_mining_processed_tweets_stream")
        show_latency_plot = gr.Checkbox(label="Show latency plot", value=False,interactive=True)
        connect_kafka_button = gr.Button("Connect to kafka",size='md',variant='primary')
        kafka_connected_status = gr.Markdown(value="")
        
        
    with gr.Tab("Crawler UI"): 
        gr.Markdown("""### Start Crawl Instance ▶️""")
        with gr.Group():
            with gr.Row(equal_height=True):
                with gr.Column():
                    cui_crawl_id = gr.Textbox(label="Crawl Identifier",
                        info="Unique identifier to manage the crawl. Ex: flood_dortmund_20250611",interactive=True)
                    cui_xusername = gr.Textbox(label="X username for crawling", value="X_username",interactive=True,info="account username")   
                    cui_xemail = gr.Textbox(label="X email for crawling", type='email',value="X_email",interactive=True,info="account email")              
                    
                with gr.Column():
                    cui_xpassword = gr.Textbox(label="X password for crawling", type='password',value="X_password",interactive=True,info="account password")
                    cui_emailpass = gr.Textbox(label="Gmail app access password", type='password',value="email_app_pass",
                        info="Should be the app pass for the x email, which should be gmail",interactive=True)
                    cui_crawl_lang = gr.Dropdown(choices=X_LANG_MAP, value="German", label="Crawl language",interactive=True,info="Only those supported by X")

                with gr.Column():  
                    cui_loc_lat = gr.Number(label='Lat of location to crawl from', value=51.513889,interactive=True,info="in Decimal")
                    cui_loc_lon = gr.Number(label='Lon of location to crawl from', value=7.465278,interactive=True,info="in Decimal")
                    cui_loc_rad = gr.Number(label='Radius to crawl from', value=100,interactive=True,info="in Km")  

                    
            cui_startcrawl_button = gr.Button("Start Crawl",variant='primary',size='md',interactive=False)
                
        gr.Markdown("### Kill Crawl Instance ⏹️")
        with gr.Group():
            with gr.Row(equal_height=True):
                with gr.Column(scale=7):
                    cui_killcrawl_id = gr.Textbox(label="Crawl Identifier to kill.",
                            info="Ex: flood_dortmund_20250611. Ensure the identifier is of a running crawl",interactive=True)
                with gr.Column(scale=3):
                    cui_killcrawl_button = gr.Button("Kill Crawl",variant='primary',size='md',interactive=False)
             
        
    with gr.Tab("Revelance UI"):
        with gr.Group():
            with gr.Accordion("Kafka polling parameters", open=False):
                with gr.Row(equal_height=True):
                    with gr.Column():
                        poll_number = gr.Number(label='Number of messages to return', value=10,info="Round value")
                    with gr.Column():
                        poll_timeout = gr.Slider(3, 50, value=10.0, label="Poll Timeout", info="lower values might not allow enough time to get kafka messages, and larger values means slower fetching")
            TN_get_button = gr.Button("Get relevant posts",variant='primary',size='md',interactive=False)
            
        TN_tweetID = gr.Textbox(visible=False)
        TN_msgs = gr.CheckboxGroup(visible=False)
        
        with gr.Row(equal_height=True):
            with gr.Column():
                with gr.Group():
                    gr.Markdown("""<p style="text-align:center">Number of Tweets Reviewed</p>""")
                    TN_consumed_markdown = gr.Markdown("""<h1 style="text-align:center">0</h1>""")
                    TN_number_consumed = gr.Number(label="Tweets reviewed",value=0,
                                                   container=False,interactive=False,visible=False)
            with gr.Column():
                TN_msg_selections = gr.CheckboxGroup(label="Select tweet index to send to ARGOS",interactive=True)
            with gr.Column():
                TN_produce_button = gr.Button("Save to ARGOS",interactive=False,variant='primary')

        gr.Markdown("""### Select a tweet_id in the table to view Embedded tweet""")
        with gr.Row():
            with gr.Column(scale=3):
                TN_data_filter = gr.Dropdown(visible=False)
                TN_tweet_embed = gr.HTML("""<div id="tweet-container"></div>""")    

            with gr.Column(scale=7):
                TN_data = gr.DataFrame(wrap=True,
                                    show_fullscreen_button=True, 
                                    show_copy_button=True,
                                    show_search="filter",
                                    max_height=700,
                                    interactive=False)
                
               
    with gr.Tab("Question Answering UI"):
        with gr.Group():
            with gr.Accordion("Kafka polling parameters", open=True):
                with gr.Row(equal_height=True):
                    with gr.Column():
                        qa_event_id = gr.Textbox(label="Event ID", info="unique event id used for crawling")
                    with gr.Column():
                        qa_num_msgs = gr.Number(label="Number of recent tweets to query",value=50,info="Round value")
            qa_get_button = gr.Button("Get recent posts",interactive=False,variant='primary')    
            
        with gr.Group():
            with gr.Row(equal_height=True):
                with gr.Column():
                    qa_type_dropdown = gr.Dropdown(label="Choose QA Type",choices=["no-RAG","Genra-RAG"],info="'no-RAG' is fast, while 'Genra-RAG' is slow but more accurate",
                                          value="no-RAG",show_label=True,interactive=True)
                    qa_query_type_dropdown = gr.Dropdown(label="Choose Query Type",choices=["query","summarise"],info="choose 'query' to ask a question, or 'summarise' to get a summary of the messages.",
                                                value="query",show_label=True,interactive=True)
                with gr.Column():
                    qa_response_lang_dropdown = gr.Dropdown(label="Choose Response Language",choices=["English","German","Spanish","Catalan"],info="the language to get your response",
                                                   value="English",show_label=True,interactive=True)                    
            
        qa_query = gr.Textbox(label="Ask a question about the incident.",interactive=True)
        
        qa_query_examples = gr.Examples(STOCK_EXAMPLES["English"], qa_query, visible=True)
        
        qa_run_button = gr.Button("Run QA",interactive=False,variant='primary')
        
        qa_response = gr.Textbox(label="Response.",lines=5)

        qa_tweetID = gr.Textbox(visible=False)
        gr.Markdown("""### Select a tweet_id in the table to view Embedded tweet""")
        with gr.Row():
            with gr.Column(scale=3):
                qa_tweet_embed = gr.HTML("""<div id="tweet-container-2"></div>""")
                
            with gr.Column(scale=7):                
                qa_df = gr.DataFrame(wrap=True,
                             show_fullscreen_button=True, 
                             show_copy_button=True,
                             show_search="filter",
                             max_height=700,
                             interactive=False)
                
                
    with gr.Tab("DEBUG/LOG Console"):
        latency_chart_header = gr.Markdown("### 📊 Real-Time Latency Chart", visible=False) 
        line_chart = gr.Plot(get_latency_stats, every=1.0, visible=False) 
        
        gr.Markdown("""### Kafka Topic Status
                    Enter topic:""")
        dg_topic_input = gr.Textbox(label="Enter topic:",container=False,show_label=True)
        with gr.Row(equal_height=True):
            with gr.Column():
                with gr.Group():
                    gr.Markdown("""<p style="text-align:center">Number of Messages in Topic</p>""")
                    dg_offset_display = gr.Markdown("""<h1 style="text-align:center">NaN</h1>""")
            with gr.Column():
                with gr.Group():
                    gr.Markdown("""<p style="text-align:center">Timestamp of Last Message</p>""")
                    dg_timestamp_display = gr.Markdown("""<h1 style="text-align:center">N/A</h1>""")
            with gr.Column():
                dg_refresh_btn = gr.Button("Refresh",variant='primary',size='md',interactive=False)
        dg_last_msg = gr.JSON(label="Last Message",show_label=True)
                
        gr.Markdown("""### Crawler Log console""")
        with gr.Row(equal_height=True):
            with gr.Column():
                with gr.Group():
                    with gr.Row(equal_height=True):
                        with gr.Column(scale=8):
                            with gr.Accordion(label="Settings",open=False):
                                cui_tail_console1 = gr.Number(label='Log tail', value=20,interactive=True)
                        with gr.Column(scale=2,min_width=160):
                            cui_log_refresh1_btn = gr.Button("Refresh",variant='primary',size='md',interactive=False)
                    cui_console1 = gr.Textbox(value="Click Refresh", label="Running crawls logs", lines=20,interactive=False)
            with gr.Column():
                with gr.Group():
                    with gr.Row(equal_height=True):
                        with gr.Column(scale=8):
                            with gr.Accordion(label="Settings",open=False):
                                cui_tail_console2 = gr.Number(label='Log tail', value=20,interactive=True)
                                cui_console2_id = gr.Textbox(value="", label="Filter crawl id",interactive=True)
                                gr.Examples(label="Click a log filter and then refresh",
                                            examples=[["ERROR"],["WARNING"],["INFO"]], 
                                            inputs=[cui_console2_id])
                        with gr.Column(scale=2,min_width=160):
                            cui_log_refresh2_btn = gr.Button("Refresh",variant='primary',size='md',interactive=False)
                    cui_console2 = gr.Textbox(value="Click Refresh", label="Crawl logs by identifier", lines=20,interactive=False) 


                   
    
    createEmbedding_js = """ (x) =>
            {
                reloadTwitterWidgets();
                const tweetContainer = document.getElementById("<=CONTAINER-NAME=>");
                tweetContainer.innerHTML = "";
                twttr.widgets.createTweet(x,tweetContainer,{theme: 'dark', dnt: true, align: 'center'});
            }
        
        """

    # Event listeners ----------------------------------------------------------------------------------------------------------------------
    TN_get_button.click(
        kafka_reader, 
        inputs=[poll_number, poll_timeout, TN_number_consumed],  
        outputs=[TN_data,TN_msg_selections,TN_msgs,TN_produce_button,TN_number_consumed,TN_consumed_markdown] 
        )
    
    TN_data.select(utils.T_on_select, None, TN_tweetID)
    TN_tweetID.change(fn=None, inputs=TN_tweetID,outputs=None,js=createEmbedding_js.replace("<=CONTAINER-NAME=>", "tweet-container"))
    
    TN_produce_button.click(
        kafka_produce_to_argos, 
        inputs=[TN_msg_selections, TN_msgs, kafka_output_topic],  
        outputs=[TN_produce_button]
        )
    
    connect_kafka_button.click(
        kafka_setup, 
        inputs=[kafka_server_dropdown,kafka_server,kafka_input_topic,kafka_output_topic,show_latency_plot,
                kafka_processed_topic],  
        outputs=[connect_kafka_button,kafka_server_dropdown,kafka_server,kafka_input_topic,kafka_output_topic,
                 TN_get_button,qa_get_button,kafka_qa_input_topic,cui_startcrawl_button,cui_killcrawl_button,
                 kafka_commands_topic,kafka_crawl_logs_topic,cui_log_refresh1_btn,cui_log_refresh2_btn,
                 dg_refresh_btn,kafka_connected_status,show_latency_plot,kafka_processed_topic,
                 latency_chart_header,line_chart
                 ]
        )
    
    kafka_server_dropdown.change(
        fn=utils.toggle_inputs,
        inputs=kafka_server_dropdown,
        outputs=[kafka_server]
    )
    
    qa_get_button.click(qa_get_posts, inputs=[qa_num_msgs,kafka_qa_input_topic,qa_event_id], outputs=[qa_df,qa_run_button])  
    
    if QA_CONFIG["use_aihub"]:               
        qa_run_button.click(qa_run_aihub, inputs=[qa_query,qa_df,qa_query_type_dropdown,qa_response_lang_dropdown,qa_type_dropdown], outputs=[qa_response])
    else:
        qa_run_button.click(qa_run, inputs=[qa_query,qa_df,qa_query_type_dropdown,qa_response_lang_dropdown,qa_type_dropdown], outputs=[qa_response])
    
    qa_df.select(utils.T_on_select, None, qa_tweetID)
    qa_tweetID.change(fn=None, inputs=qa_tweetID,outputs=None,js=createEmbedding_js.replace("<=CONTAINER-NAME=>", "tweet-container-2"))
    
    qa_query_type_dropdown.change(fn=set_qa_textbox,inputs=[qa_query_type_dropdown,qa_response_lang_dropdown],outputs=[qa_query, qa_query_examples.dataset])
    
    qa_response_lang_dropdown.change(fn=set_qa_textbox,inputs=[qa_query_type_dropdown,qa_response_lang_dropdown],outputs=[qa_query, qa_query_examples.dataset])
    
    cui_startcrawl_button.click(start_crawl, inputs=[kafka_commands_topic, cui_crawl_id, cui_xusername, cui_xemail, cui_xpassword,
                                                    cui_emailpass, cui_crawl_lang, cui_loc_lat, cui_loc_lon, cui_loc_rad])
    cui_killcrawl_button.click(kill_crawl, inputs=[kafka_commands_topic, cui_killcrawl_id])
    
    cui_log_refresh1_btn.click(get_console1_value,inputs=[kafka_crawl_logs_topic,cui_tail_console1],outputs=[cui_console1])
    cui_log_refresh2_btn.click(get_console2_value,inputs=[kafka_crawl_logs_topic,cui_tail_console2,cui_console2_id],outputs=[cui_console2])
    
    dg_refresh_btn.click(get_latest_offset_and_timestamp, inputs=dg_topic_input, outputs=[dg_offset_display, dg_timestamp_display, dg_last_msg])

    demo.unload(cleanup)
demo.launch(favicon_path=os.path.join(ROOT_DIR, 'crexdata_icons', 'crex_X_no_bgrd.png')) 