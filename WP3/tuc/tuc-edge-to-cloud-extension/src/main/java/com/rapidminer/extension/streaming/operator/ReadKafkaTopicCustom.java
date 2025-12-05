package com.rapidminer.extension.streaming.operator;

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.column.Column;
import com.rapidminer.connection.configuration.ConnectionConfiguration;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.utils.ExampleSetBuilder;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.extension.kafka_connector.PluginInitKafkaConnector;
import com.rapidminer.extension.kafka_connector.connections.KafkaConnectionHandler;
import com.rapidminer.extension.kafka_connector.operator.AbstractKafkaOperator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.GenerateNewExampleSetMDRule;
import com.rapidminer.operator.text.tools.transformation.JSONDocumentsToData;
import com.rapidminer.operator.tools.TableSubsetSelector;
import com.rapidminer.parameter.*;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.parameter.conditions.EqualTypeCondition;
import com.rapidminer.tools.ClassLoaderSwapper;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import groovy.lang.Tuple2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.serialization.StringDeserializer;
import com.rapidminer.extension.kafka_connector.operator.ReadKafkaTopic;
import org.json.JSONObject;
import org.w3c.dom.Attr;

public class ReadKafkaTopicCustom extends AbstractKafkaOperator {
    public static final String PARAMETER_RETRIEVE_TIMEOUT = "retrieval_time_out";
    public static final String PARAMETER_AUTO_OFFSET = "offset_strategy";
    public static final String PARAMETER_COLLECT_STRATEGY = "collection_strategy";
    public static final String PARAMETER_GET_ALL = "get_all";
    public static final String PARAMETER_NUMBER_OF_RECORDS = "number_of_records";
    public static final String PARAMETER_COLLECTION_COUNTER = "counter";
    public static final String PARAMETER_NUMBER_TIMEOUT = "time_out";
    public static final String PARAMETER_POLL_TIMEOUT = "polling_time_out";
    public static final String PARAMETER_RECORDS_FORMAT = "records_in_json_format";
    public static final String PARAMETER_GROUP_ID = "group_id";
    private static final String[] OFFSET_TYPES = new String[]{"earliest", "latest"};
    private static final int OFFSET_EARLIEST = 0;
    private static final int OFFSET_LATEST = 1;
    private static final String[] COLLECTION_TYPES = new String[]{"duration", "number"};
    private static final int COLLECTION_DURATION = 0;
    private static final int COLLECTION_NUMBER = 1;
    private final OutputPort output = (OutputPort) this.getOutputPorts().createPort("output data");

    public ReadKafkaTopicCustom(OperatorDescription description) {
        super(description);
        this.addTopicNameMetaDataCheck();
        this.getTransformer().addRule(new GenerateNewExampleSetMDRule(this.output) {
            public void transformMD() {
                ExampleSetMetaData emd = new ExampleSetMetaData();
                emd.addAttribute(new AttributeMetaData("key", 5));
                emd.addAttribute(new AttributeMetaData("value", 5));
                emd.addAttribute(new AttributeMetaData("partition", 3));
                emd.addAttribute(new AttributeMetaData("offset", 3));
                ReadKafkaTopicCustom.this.output.deliverMD(emd);
            }
        });
    }

    @Override
    public void doWork() throws OperatorException {
        String kafkaTopic = this.getParameterAsString("kafka_topic");
        this.performTopicNameCheck(kafkaTopic);

        ConnectionConfiguration connConfig = this.connectionSelector.getConnection().getConfiguration();
        Properties clusterConfig = KafkaConnectionHandler.getINSTANCE().buildClusterConfiguration(connConfig);
        boolean jsonFormat = this.getParameterAsBoolean(PARAMETER_RECORDS_FORMAT);
        List<Attribute> listOfAtts = new LinkedList<>();

        ExampleSetBuilder builder;
        ExampleSetBuilder jsonBuilder = null;
        Attribute keyAttribute = AttributeFactory.createAttribute("key", 5);
        listOfAtts.add(keyAttribute);
        Attribute valueAttribute = AttributeFactory.createAttribute("value", 5);
        listOfAtts.add(valueAttribute);
        Attribute partitionAttribute = AttributeFactory.createAttribute("partition", 3);
        listOfAtts.add(partitionAttribute);
        Attribute offsetAttribute = AttributeFactory.createAttribute("offset", 3);
        listOfAtts.add(offsetAttribute);
        builder = ExampleSets.from(listOfAtts);

        // Parameters
        final String kafkaOffsetStrategy = this.getParameterAsString(PARAMETER_AUTO_OFFSET); // "earliest" | "latest"
        final boolean getAll = this.getParameterAsBoolean(PARAMETER_GET_ALL);
        final int numberOfRecords = this.getParameterAsInt(PARAMETER_NUMBER_OF_RECORDS);
        final int retrieveTimeout = this.getParameterAsInt(PARAMETER_RETRIEVE_TIMEOUT);
        final String collectStrategy = this.getParameterAsString(PARAMETER_COLLECT_STRATEGY); // "duration" | "number"
        int counter = this.getParameterAsInt(PARAMETER_COLLECTION_COUNTER);
        final int numberTimeOut = this.getParameterAsInt(PARAMETER_NUMBER_TIMEOUT);
        final int pollTimeOut = this.getParameterAsInt(PARAMETER_POLL_TIMEOUT);
        final int apiTimeOut = this.getParameterAsInt("api_timeout") * 1000;
        boolean isFirst = true;
        List<Attribute> attributes = new ArrayList<>();


        // Basic consumer config
        clusterConfig.put("key.deserializer", StringDeserializer.class);
        clusterConfig.put("value.deserializer", StringDeserializer.class);
        clusterConfig.put("default.api.timeout.ms", apiTimeOut);

        boolean hasGroupId = isParameterSet(PARAMETER_GROUP_ID);
        if (hasGroupId) {
            LOGGER.info("Using group.id");
            clusterConfig.put("group.id", this.getParameterAsString(PARAMETER_GROUP_ID));
            clusterConfig.put("enable.auto.commit", "false"); // no need for reset
        } else {
            clusterConfig.put("group.id", UUID.randomUUID().toString());
            clusterConfig.put("enable.auto.commit", "false");
            clusterConfig.put("auto.offset.reset", kafkaOffsetStrategy);
        }

        try {
            ClassLoaderSwapper cls = ClassLoaderSwapper.withContextClassLoader(PluginInitKafkaConnector.getPluginLoader());
            Throwable clsErr = null;

            try {
                KafkaConsumer<Object, Object> kafkaConsumer = new KafkaConsumer<>(clusterConfig);
                Throwable consumerErr = null;

                try {
                    // Subscribe and wait for assignment
                    kafkaConsumer.subscribe(Collections.singletonList(kafkaTopic));
                    while (kafkaConsumer.assignment().isEmpty()) {
                        kafkaConsumer.poll(Duration.ofMillis(100));
                    }
                    List<TopicPartition> assigned = new LinkedList<>(kafkaConsumer.assignment());

                    // Start positions:
                    // 1) If committed exists for this group/partition => seek to committed
                    // 2) Otherwise fallback to parameter (earliest/latest) only for those partitions
                    for (TopicPartition tp : assigned) {
                        org.apache.kafka.clients.consumer.OffsetAndMetadata committed = null;
                        try {
                            committed = kafkaConsumer.committed(tp);
                        } catch (Exception ex) {
                            LOGGER.warning("Could not fetch committed offset for " + tp + ": " + ex.getMessage());
                        }

                        if (committed != null) {
                            kafkaConsumer.seek(tp, committed.offset());
                            LOGGER.info("Seeking " + tp + " to committed offset " + committed.offset());
                        } else {
                            if (OFFSET_TYPES[0].equals(kafkaOffsetStrategy)) { // earliest
                                kafkaConsumer.seekToBeginning(Collections.singletonList(tp));
                                LOGGER.info("No committed offset for " + tp + ". Seeking to beginning.");
                            } else { // latest
                                kafkaConsumer.seekToEnd(Collections.singletonList(tp));
                                LOGGER.info("No committed offset for " + tp + ". Seeking to end.");
                            }
                        }
                    }

                    // Polling / collection
                    final Duration duration = Duration.ofSeconds(pollTimeOut);
                    final Instant start = Instant.now();

                    if (OFFSET_TYPES[0].equals(kafkaOffsetStrategy)) {
                        // When starting from earliest, compute the max end offset as a guard
                        AtomicReference<Long> maxOffset = new AtomicReference<>(Long.MAX_VALUE);

                        KafkaConsumer<Object, Object> kafkaOffSetConsumer = new KafkaConsumer<>(clusterConfig);
                        Throwable offsetCnsErr = null;
                        try {
                            List<TopicPartition> partitions = kafkaOffSetConsumer
                                    .partitionsFor(kafkaTopic)
                                    .stream()
                                    .map(info -> new TopicPartition(info.topic(), info.partition()))
                                    .collect(Collectors.toList());
                            kafkaOffSetConsumer.assign(partitions);
                            kafkaOffSetConsumer.seekToEnd(partitions);
                            Map<TopicPartition, Long> offsets = kafkaOffSetConsumer.endOffsets(partitions, duration);
                            maxOffset.set(Collections.max(offsets.values()));
                        } catch (Throwable t) {
                            offsetCnsErr = t;
                            throw t;
                        } finally {
                            try {
                                kafkaOffSetConsumer.close();
                            } catch (Throwable closeErr) {
                                if (offsetCnsErr != null) {
                                    offsetCnsErr.addSuppressed(closeErr);
                                } else {
                                    throw closeErr;
                                }
                            }
                        }

                        long receivedMessages = 0L;
                        outerEarliest:
                        while (true) {
                            this.checkForStop();
                            ConsumerRecords<Object, Object> records = kafkaConsumer.poll(duration);

                            if (Duration.between(start, Instant.now()).getSeconds() > retrieveTimeout
                                    || receivedMessages >= maxOffset.get()) {
                                break;
                            }
                            for (ConsumerRecord<Object, Object> record : records) {
                                if (jsonFormat) {
                                    Tuple2<List<Attribute>, double[]> attributesAndRecord = deserializeRecord(record, attributes, isFirst);
                                    LOGGER.info(" 1 the record is "  + record.value().toString());
                                    if (isFirst) {
                                        jsonBuilder = ExampleSets.from(attributes);
                                        isFirst = false;
                                    }
                                    jsonBuilder.addRow(attributesAndRecord.getSecond());
                                }
                                else {
                                    double[] row = this.buildAttributesFromRecord(keyAttribute, valueAttribute, record);
                                    builder.addRow(row);
                                }
                                receivedMessages++;
                                if (!getAll && receivedMessages >= numberOfRecords) {
                                    break outerEarliest;
                                }
                            }


                        }
                    } else {
                        // latest strategy with two collection modes
                        if (COLLECTION_TYPES[1].equals(collectStrategy)) { // "number"
                            boolean numberNotReached = true;
                            while (numberNotReached) {
                                this.checkForStop();
                                ConsumerRecords<Object, Object> records = kafkaConsumer.poll(duration);

                                if (!records.isEmpty()) {
                                    for (ConsumerRecord<Object, Object> record : records) {
                                        if (jsonFormat) {
                                            Tuple2<List<Attribute>, double[]> attributesAndRecord = deserializeRecord(record, attributes, isFirst);
                                            if (isFirst) {
                                                jsonBuilder = ExampleSets.from(attributes);
                                                isFirst = false;
                                            }
                                            jsonBuilder.addRow(attributesAndRecord.getSecond());
                                        }
                                        else {
                                            double[] row = this.buildAttributesFromRecord(keyAttribute, valueAttribute, record);
                                            builder.addRow(row);
                                        }
                                        --counter;
                                        if (counter <= 0) {
                                            numberNotReached = false;
                                            break;
                                        }
                                    }
                                }

                                if (Duration.between(start, Instant.now()).getSeconds() > numberTimeOut) {
                                    break;
                                }
                            }
                        } else { // duration case
                            do {
                                this.checkForStop();
                                ConsumerRecords<Object, Object> records = kafkaConsumer.poll(duration);
                                if (!records.isEmpty()) {
                                    for (ConsumerRecord<Object, Object> record : records) {
                                        if (jsonFormat) {
                                            Tuple2<List<Attribute>, double[]> attributesAndRecord = deserializeRecord(record, attributes, isFirst);
                                            if (isFirst) {
                                                jsonBuilder = ExampleSets.from(attributes);
                                                isFirst = false;
                                            }
                                            jsonBuilder.addRow(attributesAndRecord.getSecond());
                                        }
                                        else builder.addRow(this.buildAttributesFromRecord(keyAttribute, valueAttribute, record));
                                    }
                                }
                            } while (Duration.between(start, Instant.now()).getSeconds() <= counter);
                        }
                    }

                    // Commit offsets explicitly when using a real group
                    if (hasGroupId) {
                        try {
                            kafkaConsumer.commitSync();
                            LOGGER.info("Committed offsets for group " + clusterConfig.getProperty("group.id"));
                        } catch (Exception ex) {
                            LOGGER.warning("Commit failed: " + ex.getMessage());
                        }
                    }
                } catch (Throwable t) {
                    consumerErr = t;
                    throw t;
                } finally {
                    try {
                        kafkaConsumer.close();
                    } catch (Throwable closeErr) {
                        if (consumerErr != null) {
                            consumerErr.addSuppressed(closeErr);
                        } else {
                            throw closeErr;
                        }
                    }
                }
            } catch (Throwable t) {
                clsErr = t;
                throw t;
            } finally {
                try {
                    cls.close();
                } catch (Throwable closeErr) {
                    if (clsErr != null) {
                        clsErr.addSuppressed(closeErr);
                    } else {
                        throw closeErr;
                    }
                }
            }
        } catch (TimeoutException te) {
            throw new UserError(this, "read_kafka_topic.kafka_timeout", new Object[]{te.getLocalizedMessage()});
        } catch (KafkaException ke) {
            throw new UserError(this, "read_kafka_topic.kafka_error", new Object[]{ke.getLocalizedMessage()});
        }

        ExampleSet kafkaExampleSet;

        if (jsonFormat) {
            kafkaExampleSet = jsonBuilder.build();
        }
        else
            kafkaExampleSet = builder.build();

        this.output.deliver(kafkaExampleSet);
    }

    private double[] buildAttributesFromRecord(Attribute keyAttribute, Attribute valueAttribute, ConsumerRecord<Object, Object> record) {
        String key = record.key() == null ? "" : record.key().toString();
        String value = record.value().toString();
        return new double[]{(double) keyAttribute.getMapping().mapString(key), (double) valueAttribute.getMapping().mapString(value), (double) record.partition(), (double) record.offset()};
    }

    private Tuple2<List<Attribute>, double[]> deserializeRecord(ConsumerRecord<Object, Object> record, List<Attribute> existingAttributes, boolean isFirst) throws OperatorException {
        Tuple2<List<Attribute>, double[]> tuple;
        if (record == null) {
            return new Tuple2<>(existingAttributes, new double[0]);
        }
        JSONObject newRecord = new JSONObject(record.value().toString());
        double[] values = new double[newRecord.keySet().size() + 4];
        int i = 0;
        Iterator<String> it = newRecord.keys();

        while (it.hasNext()) {
            String key = it.next();
            Object value = newRecord.get(key);
            LOGGER.info("This is the key " + key + " and this is the value " + value);
            if (value instanceof String) {
                LOGGER.info("String attribute detected");
                if (isFirst) {
                    Attribute attribute = AttributeFactory.createAttribute(key, 5);
                    existingAttributes.add(attribute);
                }
                values[i] = (double) existingAttributes.get(i).getMapping().mapString(value.toString());
            } else if (value instanceof Boolean) {
                LOGGER.info("Boolean attribute detected");
                if (isFirst) {
                    Attribute attribute = AttributeFactory.createAttribute(key, 6);
                    attribute.getMapping().setMapping(String.valueOf(Boolean.FALSE), 0);
                    attribute.getMapping().setMapping(String.valueOf(Boolean.TRUE), 1);
                    existingAttributes.add(attribute);
                }
                values[i] = (Boolean)value ? 1.0 : 0.0;
            } else if (value instanceof Integer) {
                LOGGER.info("Integer attribute detected");
                if (isFirst) {
                    Attribute attribute = AttributeFactory.createAttribute(key, 3);
                    existingAttributes.add(attribute);
                }
                values[i] = ((Integer)value).doubleValue();
            } else if (value instanceof Float) {
                LOGGER.info("Float attribute detected");
                if (isFirst) {
                    Attribute attribute = AttributeFactory.createAttribute(key, 4);
                    existingAttributes.add(attribute);
                }
                values[i] = ((Float)value).doubleValue();
            } else {
                LOGGER.info("Other attribute detected");
                if (isFirst)
                {
                    Attribute attribute = AttributeFactory.createAttribute(key, 5);
                    existingAttributes.add(attribute);
                }
                values[i] = Double.NaN;
            }
//            values[i] = (double) attribute.getMapping().mapString(value.toString());
            i++;
        }
        return new Tuple2<>(existingAttributes, values);
    }


    private void changeToJsonExampleSet(ExampleSet exampleSetBuilder) throws OperatorException {

        IOTable readRecords = (IOTable)exampleSetBuilder.copy();
        Column values  = readRecords.getTable().column("value");
        for (int i = 0; i < values.getDictionary().size(); i++) {
            LOGGER.info("Value " + i + ": " + values.getDictionary().get(i) );
        }
    }

public List<ParameterType> getParameterTypes() {
    List<ParameterType> types = super.getParameterTypes();
    ParameterType updateLink = new ParameterTypeLinkButton("update_topics", "update topic list from Kafka server", this.updateAction);
    types.add(updateLink);
    ParameterType offset = new ParameterTypeCategory("offset_strategy", "Kafka offset strategy", OFFSET_TYPES, 0);
    types.add(offset);
    ParameterType wait = new ParameterTypeInt("retrieval_time_out", "timer to finish the retrieval of old messages", 1, Integer.MAX_VALUE, 2, false);
    wait.registerDependencyCondition(new EqualTypeCondition(this, "offset_strategy", OFFSET_TYPES, true, new int[]{0}));
    types.add(wait);
    ParameterType getAll = new ParameterTypeBoolean("get_all", "retrieve all previous records", true, false);
    getAll.registerDependencyCondition(new EqualTypeCondition(this, "offset_strategy", OFFSET_TYPES, true, new int[]{0}));
    types.add(getAll);
    ParameterType records = new ParameterTypeInt("number_of_records", "number of records to retrieve", 1, Integer.MAX_VALUE, 100, false);
    records.registerDependencyCondition(new BooleanParameterCondition(this, "get_all", false, false));
    types.add(records);
    ParameterType strategy = new ParameterTypeCategory("collection_strategy", "Waiting by time or number of records", COLLECTION_TYPES, 0);
    strategy.registerDependencyCondition(new EqualTypeCondition(this, "offset_strategy", OFFSET_TYPES, true, new int[]{1}));
    types.add(strategy);
    ParameterType counter = new ParameterTypeInt("counter", "number of records or seconds to wait", 1, Integer.MAX_VALUE, 100, false);
    counter.registerDependencyCondition(new EqualTypeCondition(this, "offset_strategy", OFFSET_TYPES, true, new int[]{1}));
    types.add(counter);
    ParameterType timeout = new ParameterTypeInt("time_out", "max number seconds to wait for records to receive", 1, Integer.MAX_VALUE, 120, false);
    timeout.registerDependencyCondition(new EqualTypeCondition(this, "collection_strategy", COLLECTION_TYPES, true, new int[]{1}));
    types.add(timeout);
    ParameterType pollingTimeOut = new ParameterTypeInt("polling_time_out", "timeout in seconds for a single poll.", 1, Integer.MAX_VALUE, 5, true);
    types.add(pollingTimeOut);
    ParameterType apiTimeOut = new ParameterTypeInt("api_timeout", "generic operation time out in seconds", 0, Integer.MAX_VALUE, 10, true);
    types.add(apiTimeOut);

    ParameterType groupId = new ParameterTypeString(PARAMETER_GROUP_ID, "The group id for the consumer. If not set, a random group id will be generated.", true, false);
//        groupId.setHidden(true);
    types.add(groupId);

    ParameterType checkJSONFormat = new ParameterTypeBoolean(PARAMETER_RECORDS_FORMAT, "Check if the records are in JSON format, and all data have the same attributes. In case an attribute does not exist is set to null. If the checkbox is set to false, data will have the format of key-value. Value contains the read record.", false, false);
    types.add(checkJSONFormat);
    return types;
}
}
