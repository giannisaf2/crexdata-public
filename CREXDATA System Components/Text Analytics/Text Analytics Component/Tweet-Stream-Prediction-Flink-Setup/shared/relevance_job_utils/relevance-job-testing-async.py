import sys
import json
import time
import requests
import argparse
from concurrent.futures import ThreadPoolExecutor
from ftfy import fix_encoding

from pyflink.common.typeinfo import Types
from pyflink.datastream.state import ValueStateDescriptor
from pyflink.datastream import StreamExecutionEnvironment
from pyflink.common.serialization import SimpleStringSchema
from pyflink.datastream.connectors import KafkaSource, KafkaSink, KafkaRecordSerializationSchema, DeliveryGuarantee
from pyflink.datastream.functions import RuntimeContext, MapFunction, KeyedProcessFunction
from pyflink.common.watermark_strategy import WatermarkStrategy
from pyflink.datastream.connectors import KafkaOffsetsInitializer
from pyflink.datastream.execution_mode import RuntimeExecutionMode


# -----------------CONFIGURATION---------------------------------------
"""
Configure this section before running
"""
ENDPOINT_URL = "<AI_HUB_ENDPOINT_URL>"

AUTHENTICATED = True # or False for unautheticated

CREXDATA_AUTH = {
    "security.protocol": "SASL_SSL",
    "sasl.mechanism": "PLAIN",
    "ssl.truststore.location": "/mnt/kafka_auth_files/kafka.truststore.jks",
    "ssl.truststore.password": "<PASWORD>",
    "ssl.keystore.location": "/mnt/kafka_auth_files/kafka.keystore.jks",
    "ssl.keystore.password": "<PASWORD>",
    "sasl.jaas.config": "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"<USER>\" password=\"<PASSWORD>\";"  
}

#----------------------------------------------------------------------


def relevance_predictor_endpoint_call(record):
    start_ts = int(time.time() * 1000)
    endpoint_url = (
        ENDPOINT_URL
    )
    headers = {"Content-Type": "application/json"}
    tweet_text = fix_encoding(record['tweet_text'])
    data = {"data": [{"tweet_text": tweet_text}]}
    response = requests.post(endpoint_url, headers=headers, json=data)
    try:
        response.raise_for_status()        
        prediction = response.json()
        record["event_class"] = prediction["data"][0]["event_class"]
        record["event_score"] = prediction["data"][0]["event_score"]
        end_ts = int(time.time() * 1000)
        record["processing_time_ms"] = end_ts - start_ts
    except requests.exceptions.HTTPError as e:
        raise Exception (f"HTTPError: {e}")
    return record 


class RoundRobinKeySelector:
    """
    Assigns a key to each element in a round-robin fashion.
    This is used to distribute a stream evenly across all available parallel tasks
    when there is no natural key in the data.
    """
    def __init__(self, parallelism: int):
        self.parallelism = max(1, parallelism)  # Ensure parallelism is at least 1
        self.counter = 0

    def get_key(self, element):
        key = self.counter % self.parallelism
        self.counter += 1
        return key
    

class JsonDump(MapFunction):
    
    def map(self, value):
        return json.dumps(value)
    
            
class AsyncRelevancePrediction(KeyedProcessFunction):

    def __init__(self, pool_size: int = 10, check_interval_ms: int = 200):
        self.pool_size = pool_size
        self.check_interval_ms = check_interval_ms
        self.executor = None
        self.timer_registered_state = None
        self.pending_futures = []

    def open(self, runtime_context: RuntimeContext):
        self.executor = ThreadPoolExecutor(max_workers=self.pool_size)
        timer_descriptor = ValueStateDescriptor("timer", Types.BOOLEAN())
        self.timer_registered_state = runtime_context.get_state(timer_descriptor)

    def process_element(self, value, ctx: 'KeyedProcessFunction.Context'):
        yield from self._check_completed_futures()
        
        record = json.loads(value)
        event_ts = ctx.timestamp()
        now_ts = int(time.time() * 1000)
        record["msg_created_timestamp"] = event_ts
        record["flink_consumer_lag_ms"] = now_ts - event_ts

        future = self.executor.submit(relevance_predictor_endpoint_call, record)
        self.pending_futures.append((value, future))

        if self.timer_registered_state.value() is None or not self.timer_registered_state.value():
            current_time = ctx.timer_service().current_processing_time()
            ctx.timer_service().register_processing_time_timer(current_time + self.check_interval_ms)
            self.timer_registered_state.update(True)
            
    def on_timer(self, timestamp: int, ctx: 'KeyedProcessFunction.OnTimerContext'):
        yield from self._check_completed_futures()
        current_time = ctx.timer_service().current_processing_time()
        ctx.timer_service().register_processing_time_timer(current_time + self.check_interval_ms)

    def _check_completed_futures(self):
        """Helper method to poll, yield results, and clean up the future list."""
        completed = [f for f in self.pending_futures if f[1].done()]
        for original_value, future in completed:
            try:
                result = future.result()
                end_ts = int(time.time() * 1000)
                result["latency_ms"] = end_ts - int(result["msg_created_timestamp"])
                yield result
            except Exception as e:
                original_value["processing_error"] = e
                yield original_value
        self.pending_futures = [f for f in self.pending_futures if not f[1].done()]
                     
    def close(self):
        """Shutdown the thread pool."""
        if self.executor:
            self.executor.shutdown(wait=True)
            

def relevance_prediction_job(args):
    env = StreamExecutionEnvironment.get_execution_environment()
    env.enable_checkpointing(30000)
    env.set_parallelism(2)
    env.set_runtime_mode(RuntimeExecutionMode.STREAMING)
    
    if args.offset_type == "earliest":
        offset_type = KafkaOffsetsInitializer.earliest()
    elif args.offset_type == "latest":
        offset_type = KafkaOffsetsInitializer.latest()
    else:
        parser.error("Invalid action, select from choices=[earliest, latest]")
    
    if AUTHENTICATED:
        kafka_source = KafkaSource.builder() \
            .set_bootstrap_servers(args.kafka_server) \
            .set_property("security.protocol", CREXDATA_AUTH["security.protocol"]) \
            .set_property("sasl.mechanism", CREXDATA_AUTH["sasl.mechanism"]) \
            .set_property("ssl.truststore.location", CREXDATA_AUTH["ssl.truststore.location"]) \
            .set_property("ssl.truststore.password", CREXDATA_AUTH["ssl.truststore.password"]) \
            .set_property("ssl.keystore.location", CREXDATA_AUTH["ssl.keystore.location"]) \
            .set_property("ssl.keystore.password", CREXDATA_AUTH["ssl.keystore.password"]) \
            .set_property("sasl.jaas.config", CREXDATA_AUTH["sasl.jaas.config"]) \
            .set_topics(args.input_topic) \
            .set_group_id("relevance-consumer-group") \
            .set_starting_offsets(offset_type) \
            .set_value_only_deserializer(SimpleStringSchema()) \
            .build()
            
        kafka_sink = KafkaSink.builder() \
            .set_bootstrap_servers(args.kafka_server) \
            .set_property("security.protocol", CREXDATA_AUTH["security.protocol"]) \
            .set_property("sasl.mechanism", CREXDATA_AUTH["sasl.mechanism"]) \
            .set_property("ssl.truststore.location", CREXDATA_AUTH["ssl.truststore.location"]) \
            .set_property("ssl.truststore.password", CREXDATA_AUTH["ssl.truststore.password"]) \
            .set_property("ssl.keystore.location", CREXDATA_AUTH["ssl.keystore.location"]) \
            .set_property("ssl.keystore.password", CREXDATA_AUTH["ssl.keystore.password"]) \
            .set_property("sasl.jaas.config", CREXDATA_AUTH["sasl.jaas.config"]) \
            .set_record_serializer(
                KafkaRecordSerializationSchema.builder()
                    .set_topic(args.output_topic)
                    .set_value_serialization_schema(SimpleStringSchema())
                    .build()
            ) \
            .set_delivery_guarantee(DeliveryGuarantee.AT_LEAST_ONCE) \
            .set_transactional_id_prefix("main-sink") \
            .build()
            
        kafka_sink_filtered = KafkaSink.builder() \
            .set_bootstrap_servers(args.kafka_server) \
            .set_property("security.protocol", CREXDATA_AUTH["security.protocol"]) \
            .set_property("sasl.mechanism", CREXDATA_AUTH["sasl.mechanism"]) \
            .set_property("ssl.truststore.location", CREXDATA_AUTH["ssl.truststore.location"]) \
            .set_property("ssl.truststore.password", CREXDATA_AUTH["ssl.truststore.password"]) \
            .set_property("ssl.keystore.location", CREXDATA_AUTH["ssl.keystore.location"]) \
            .set_property("ssl.keystore.password", CREXDATA_AUTH["ssl.keystore.password"]) \
            .set_property("sasl.jaas.config", CREXDATA_AUTH["sasl.jaas.config"]) \
            .set_record_serializer(
                KafkaRecordSerializationSchema.builder()
                    .set_topic(args.output_filtered)
                    .set_value_serialization_schema(SimpleStringSchema())
                    .build()
            ) \
            .set_delivery_guarantee(DeliveryGuarantee.AT_LEAST_ONCE) \
            .set_transactional_id_prefix("filtered-sink") \
            .build()
    else:
        kafka_source = KafkaSource.builder() \
            .set_bootstrap_servers(args.kafka_server) \
            .set_topics(args.input_topic) \
            .set_group_id("relevance-consumer-group") \
            .set_starting_offsets(offset_type) \
            .set_value_only_deserializer(SimpleStringSchema()) \
            .build()
            
        kafka_sink = KafkaSink.builder() \
            .set_bootstrap_servers(args.kafka_server) \
            .set_record_serializer(
                KafkaRecordSerializationSchema.builder()
                    .set_topic(args.output_topic)
                    .set_value_serialization_schema(SimpleStringSchema())
                    .build()
            ) \
            .set_delivery_guarantee(DeliveryGuarantee.AT_LEAST_ONCE) \
            .set_transactional_id_prefix("main-sink") \
            .build()
            
        kafka_sink_filtered = KafkaSink.builder() \
            .set_bootstrap_servers(args.kafka_server) \
            .set_record_serializer(
                KafkaRecordSerializationSchema.builder()
                    .set_topic(args.output_filtered)
                    .set_value_serialization_schema(SimpleStringSchema())
                    .build()
            ) \
            .set_delivery_guarantee(DeliveryGuarantee.AT_LEAST_ONCE) \
            .set_transactional_id_prefix("filtered-sink") \
            .build()
            
    ds = env.from_source(kafka_source, WatermarkStrategy.for_monotonous_timestamps(), "Kafka Source")

    key_selector = RoundRobinKeySelector(env.get_parallelism())
    stream = ds.key_by(key_selector.get_key) \
        .process(AsyncRelevancePrediction(pool_size=10)) \
        .map(JsonDump(), output_type=Types.STRING())

    stream.sink_to(kafka_sink)
    
    stream.filter(lambda x : json.loads(x)["event_class"] != "none").sink_to(kafka_sink_filtered)

    env.execute(args.job_name)


if __name__ == "__main__":
    print("Ready for Kafka messages!!")
    parser = argparse.ArgumentParser()
    parser.add_argument('--kafka-server',dest='kafka_server',required=True,help='Kafka server. Ex. localhost:9092')
    parser.add_argument('--input-topic',dest='input_topic',required=True,help='Input topic from crawler.')
    parser.add_argument('--output-topic',dest='output_topic',required=True,help='Output topic for processed tweets.')
    parser.add_argument('--output-filtered',dest='output_filtered',required=True,help='Output topic for filtered relevent tweets.')
    parser.add_argument('--job-name',dest='job_name',required=True,help='Job name.')
    parser.add_argument("--offset-type", nargs='?', choices=["earliest","latest"], default="latest", \
        help="Set the offset type, use 'earliest' to process a whole topic from the beginning and 'latest' for new incoming messages. Default 'latest'.")

    argv = sys.argv[1:]
    known_args, _ = parser.parse_known_args(argv)
    
    relevance_prediction_job(known_args)
