import time
import math
import json

import gradio as gr
import numpy as np

from ftfy import fix_encoding
from confluent_kafka import KafkaException, TopicPartition



    
def T_on_select(evt: gr.SelectData):
    return evt.value 


def toggle_inputs(value):
    if value == "crexdata-unauth":
        return gr.update(visible=True)
    else:
        return gr.update(visible=False)
    

def print_assignment(consumer, partitions):
    print('Assignment:', partitions)
    

def delivery_callback(err, event): 
    if err is not None:
        print(f'Delivery failed on reading for {event.value().decode("utf8")}: {err}')
    else:
        print(f'Temp reading for {event.value().decode("utf8")} produced to {event.topic()}')


def select_items_from_bins(bin_sizes, M):
    """
    Selects items from uneven bins (represented as a dictionary) such that each bin is equally represented in the selection.

    Args:
        bin_sizes (dict): A dictionary where keys are bin identifiers (e.g., strings) and values are the number of items in each bin.
        M (int): The total number of items to select.

    Returns:
        dict: A dictionary with the same keys as bin_sizes, representing the number of items to select from each bin.
              If M > total_items, returns the original bin_sizes dictionary.
    """

    total_items = sum(bin_sizes.values())
    if M > total_items:
        return bin_sizes.copy()  

    bin_sizes_array = np.array(list(bin_sizes.values()))
    items_per_bin = np.zeros(len(bin_sizes), dtype=int)  

    difference = M
    while difference > 0:
        available_items = bin_sizes_array - items_per_bin
        total_available = np.sum(available_items)

        if total_available == 0:
            break  

        remaining_proportions = available_items / total_available
        to_add = np.round(remaining_proportions * difference).astype(int)
        to_add = np.minimum(to_add, available_items)  

        items_per_bin += to_add
        difference -= np.sum(to_add)
        if difference < 0:
            difference = 0  
    selected_items = dict(zip(bin_sizes.keys(), items_per_bin.tolist()))

    return selected_items
        
        
def consume_latest_messages(consumer, topic, num_messages, timeout=10.0):
    """
    Consume up to `num_messages` latest messages from the given `topic`,
    distributed across partitions. If messages are more than in partitions, consume all.
    """

    consumer.unsubscribe()

    metadata = consumer.list_topics(topic, timeout=5.0)
    partitions = metadata.topics[topic].partitions.keys()
    topic_partitions = [TopicPartition(topic, p) for p in partitions]

    partition_sizes = {}
    high_offsets = {}
    for tp in topic_partitions:
        low, high = consumer.get_watermark_offsets(tp, timeout=5.0)
        partition_sizes[tp] = max(0, high - low) 
        high_offsets[tp] = high                   

    alloc = select_items_from_bins(partition_sizes, num_messages)

    seek_positions = []
    for tp in topic_partitions:
        count = alloc[tp]
        high = high_offsets[tp]
        low = high - partition_sizes[tp]
        start_offset = max(low, high - count)
        seek_positions.append(TopicPartition(tp.topic, tp.partition, start_offset))

    consumer.assign(seek_positions)

    messages = []
    start_time = time.time()
    while len(messages) < num_messages and (time.time() - start_time) < timeout:
        msg = consumer.poll(timeout=1.0)
        if msg is None:
            continue
        if msg.error():
            if msg.error().code() == KafkaException._PARTITION_EOF:
                continue
            raise KafkaException(msg.error())
        messages.append(msg)

    return messages


def ftfy_fix_dict(data):
    if isinstance(data, dict):
        return {k: ftfy_fix_dict(v) for k, v in data.items()}
    elif isinstance(data, list):
        return [ftfy_fix_dict(v) for v in data]
    elif isinstance(data, str):
        return fix_encoding(data)
    else:
        return data


def msg_processing(msg, case='loads'):
    match case:
        case "loads":
            loaded_msg = json.loads(msg.value().decode('utf-8'))
            fixed_msg = ftfy_fix_dict(loaded_msg)
            return fixed_msg
        case "dumps":
            fixed_msg = ftfy_fix_dict(msg)
            return json.dumps(fixed_msg)
        case _:
            raise Exception("Error in utils.py: msg_processing(): case parameter should be loads or dumps")
