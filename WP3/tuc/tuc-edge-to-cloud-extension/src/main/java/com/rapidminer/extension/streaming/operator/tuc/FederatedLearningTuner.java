/**
 * Authors: Ourania Ntouni
 * <p>
 * Copyright (C) 2025-2026 Technical University of Crete
 */
package com.rapidminer.extension.streaming.operator.tuc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.utils.ExampleSetBuilder;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.extension.streaming.ioobject.StreamDataContainer;
import com.rapidminer.extension.streaming.operator.AbstractStreamOperator;
import com.rapidminer.extension.streaming.operator.StreamingNest;
import com.rapidminer.extension.streaming.operator.StreamingOptimizationOperator;
import com.rapidminer.extension.streaming.utility.graph.StreamGraph;
import com.rapidminer.extension.streaming.utility.graph.StreamGraphNodeVisitor;
import com.rapidminer.extension.streaming.utility.graph.StreamProducer;
import com.rapidminer.extension.streaming.utility.graph.StreamSource;
import com.rapidminer.extension.streaming.utility.graph.JsonDataProducer;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.parameter.*;
import com.rapidminer.parameter.conditions.AndParameterCondition;
import com.rapidminer.parameter.conditions.EqualTypeCondition;
import com.rapidminer.parameter.conditions.ParameterCondition;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.container.Pair;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * RapidMiner operator that produces JSON according to the new schema:
 *
 * {
 *   "mode": "flower" | "ns3",
 *   "flower": { ... } OR empty,
 *   "ns3": { ... } OR empty
 * }
 */
public class FederatedLearningTuner extends AbstractStreamOperator implements StreamSource {

    private final OutputPort jsonOutput = getOutputPorts().createPort("output stream 1");

    // ------------------------------------------------------------------------
    // Parameter keys
    // ------------------------------------------------------------------------

    // 1. MODE
    private static final String PARAMETER_OPERATION_MODE = "operation_mode"; // category: flower / ns3
    private static final String PARAMETER_UNIQUE_NAME = "Unique_model_name"; // category: flower / ns3
    private static final String PARAMETER_MODE = "mode";  // category: flower / ns3

    // ------------------------------------------------------------------------
    // FLOWER-RELATED PARAMETERS
    // ------------------------------------------------------------------------

    // 2a. Flower: Dataset Section
    private static final String PARAMETER_FLOWER_DATASET_NAME = "Flower_dataset_name";  // required
    private static final String PARAMETER_FLOWER_DIRICHLET_ALPHA = "Flower_dirichlet_alpha"; // optional
    private static final String PARAMETER_FLOWER_CLIENT_DATA_LOCATIONS = "Flower_client_data_locations"; // optional
    private static final String PARAMETER_FLOWER_DATASET_PATH = "Flower_dataset_path";
    // 2b. Flower: Model Section
    private static final String PARAMETER_FLOWER_NEURAL_NETWORK_NAME = "Flower_neural_network_name"; // required
    private static final String PARAMETER_FLOWER_NUM_LABELS = "Flower_num_labels";                   // required
    private static final String PARAMETER_FLOWER_OUTPUT_TOPIC = "Flower_output_topic";               // optional
    private static final String PARAMETER_FLOWER_BROKER_IP = "Flower_broker_ip";                     // optional


    // 2c. Flower: Training Section
    private static final String PARAMETER_FLOWER_NUM_CLIENTS = "Flower_num_clients";            // required
    private static final String PARAMETER_FLOWER_CLIENTS_PER_ROUND = "Flower_clients_per_round";// required
    private static final String PARAMETER_FLOWER_BATCH_SIZE = "Flower_batch_size";              // required
    private static final String PARAMETER_FLOWER_TOTAL_ROUNDS = "Flower_total_rounds";          // required
    private static final String PARAMETER_FLOWER_FDA_APPROACH = "Flower_fda_approach";          // required
    private static final String PARAMETER_FLOWER_THRESHOLD = "Flower_threshold";                // required

    // 2d. Flower: Server Section
    private static final String PARAMETER_FLOWER_SERVER_IP = "Flower_server_ip";                    // required
    private static final String PARAMETER_FLOWER_SERVER_FL_PORT = "Flower_server_fl_port";          // required
    private static final String PARAMETER_FLOWER_SERVER_METRIC_PORT = "Flower_server_metric_port";  // required

    // 2e. Flower: Clients Section
    private static final String PARAMETER_FLOWER_CLIENTS_NETWORK = "Flower_clients_network"; // required

    // ------------------------------------------------------------------------
    // NS3-RELATED PARAMETERS
    // ------------------------------------------------------------------------

    // 3a. NS3: Federated Learning
    private static final String PARAMETER_NS3_NUM_CLIENTS = "NS3_num_clients";               // required
    private static final String PARAMETER_NS3_CLIENTS_PER_ROUND = "NS3_clients_per_round";   // required
    private static final String PARAMETER_NS3_TOTAL_ROUNDS = "NS3_total_rounds";             // required
    private static final String PARAMETER_NS3_FDA_APPROACH = "NS3_Sync_method";             // required
    private static final String PARAMETER_NS3_DATASET_NAME = "NS3_dataset_name";             // required
    private static final String PARAMETER_NS3_NEURAL_NETWORK_NAME = "NS3_neural_network_name";// required
    private static final String PARAMETER_NS3_NUM_LABELS = "NS3_num_labels";                 // required

    // 3b. NS3: Network
    private static final String PARAMETER_NS3_TX_GAIN = "NS3_tx_gain";                                      // required
    private static final String PARAMETER_NS3_SERVER_DATARATE = "NS3_server_datarate";                      // required
    private static final String PARAMETER_NS3_MAX_PACKET_SIZE = "NS3_max_packet_size";                      // required
    private static final String PARAMETER_NS3_WIFI_PERFORMANCE = "NS3_wifi_performance_standard";           // required
    private static final String PARAMETER_NS3_TOPOLOGY = "NS3_topology";                                    // optional


    private static final String PARAMETER_NS3_SIMULATION_PARALLELISM = "Simulation_parallelism";
    private static final String PARAMETER_NS3_SIMULATION_FORCE_RUN = "Simulation_force_run";

    public FederatedLearningTuner(OperatorDescription description) {
        super(description);
        getTransformer().addGenerationRule(jsonOutput, StreamDataContainer.class);
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> parameterTypes = super.getParameterTypes();

        // --------------------------------------------------------------------
        // 0. Mode selector
        // --------------------------------------------------------------------


        ParameterTypeCategory modeParam = new ParameterTypeCategory(
                PARAMETER_MODE,
                "Select whether to produce JSON for Flower or NS3 mode.",
                new String[] {"flower", "ns3"},
                0,  // default index => "flower"
                false
        );
        parameterTypes.add(modeParam);

        // Conditions for Flower and NS3 modes
        ParameterCondition modeCondition = new EqualTypeCondition(
                this, PARAMETER_MODE, new String[]{"flower"}, false, 0);

        ParameterTypeCategory operationModeParam = new ParameterTypeCategory(
                PARAMETER_OPERATION_MODE,
                "Select the operation mode: Training or Inferencing.",
                new String[] {"Training", "Inference"},
                0,  // default index => "Training"
                false
        );
        operationModeParam.registerDependencyCondition(modeCondition);
        parameterTypes.add(operationModeParam);

        ParameterTypeCategory ns3Operationmode = new ParameterTypeCategory(
                "NS3_operation_mode",
                "Select the operation mode: Training or Inferencing.",
                new String[] {"Simulation", "Query"},
                0,  // default index => "Training"
                false
        );
        // Conditions for Flower and NS3 modes
        ParameterCondition ns3ModeCondition = new EqualTypeCondition(
                this, PARAMETER_MODE, new String[]{"ns3"}, false, 1);
        ns3Operationmode.registerDependencyCondition(ns3ModeCondition);
        parameterTypes.add(ns3Operationmode);

        ParameterCondition ns3SimulationCondition = new EqualTypeCondition(
                this, "NS3_operation_mode", new String[]{"Simulation"}, false, 0);

        ParameterTypeString uniqueName = new ParameterTypeString(
                PARAMETER_UNIQUE_NAME,
                "Unique name for the inferencing model.",
                false
        );
        uniqueName.registerDependencyCondition(modeCondition);
        parameterTypes.add(uniqueName);

        ParameterCondition opModeCondition = new EqualTypeCondition(
                this, PARAMETER_OPERATION_MODE, new String[]{"Training"}, false, 0);

        AndParameterCondition combinedCondition = new AndParameterCondition(
                this, false, modeCondition, opModeCondition
        );

        ParameterCondition infrenceCondition = new EqualTypeCondition(
                this, PARAMETER_OPERATION_MODE, new String[]{"Inference"}, false, 1);

        AndParameterCondition flowerInferenceCondition = new AndParameterCondition(
                this, false, modeCondition, infrenceCondition);
        // --------------------------------------------------------------------
        // 1. Label heading for Flower parameters (shown conditionally)
        // --------------------------------------------------------------------

        // --------------------------------------------------------------------
        // 2. FLOWER PARAMETERS
        // --------------------------------------------------------------------

        // 2a. Dataset
        ParameterTypeString flowerDatasetName = new ParameterTypeString(
                PARAMETER_FLOWER_DATASET_NAME,
                "[FLOWER] The dataset name (e.g., 'MNIST', 'CIFAR10'). (Required if mode=flower)",
                true
        );
        flowerDatasetName.registerDependencyCondition(combinedCondition);
        parameterTypes.add(flowerDatasetName);

        ParameterTypeString flowerDatasetPath = new ParameterTypeString(
                PARAMETER_FLOWER_DATASET_PATH,
                "[FLOWER] The dataset path for inference.",
                true
        );
        flowerDatasetPath.registerDependencyCondition(flowerInferenceCondition);
        parameterTypes.add(flowerDatasetPath);

        ParameterTypeDouble flowerDirichletAlpha = new ParameterTypeDouble(
                PARAMETER_FLOWER_DIRICHLET_ALPHA,
                "[FLOWER] Dirichlet data distribution parameter for non-IID partitioning. (Optional)",
                0.0,
                Double.MAX_VALUE,
                true // optional
        );

        flowerDirichletAlpha.registerDependencyCondition(combinedCondition);
        parameterTypes.add(flowerDirichletAlpha);

        ParameterTypeList flowerClientDataLocations = new ParameterTypeList(
                PARAMETER_FLOWER_CLIENT_DATA_LOCATIONS,
                "[FLOWER] List of client-specific dataset paths (Optional).",
                new ParameterTypeInt("client_id", "Unique identifier for the client.", 0, Integer.MAX_VALUE),
                new ParameterTypeString("path", "Dataset path for the client.", true)
        );
        flowerClientDataLocations.registerDependencyCondition(combinedCondition);
        parameterTypes.add(flowerClientDataLocations);

        // 2b. Model
        ParameterTypeCategory flowerNeuralNetworkName = new ParameterTypeCategory(
                PARAMETER_FLOWER_NEURAL_NETWORK_NAME,
                "[FLOWER] The name of the neural network to be used. (Required if mode=flower)",
                new String[]{"lenet5", "VGG16", "ResNet", "AlexNet"},
                0,
                false
        );
        flowerNeuralNetworkName.registerDependencyCondition(combinedCondition);
        parameterTypes.add(flowerNeuralNetworkName);

        ParameterTypeInt flowerNumLabels = new ParameterTypeInt(
                PARAMETER_FLOWER_NUM_LABELS,
                "[FLOWER] Number of output labels for the task. (Required if mode=flower)",
                1,
                Integer.MAX_VALUE,
                true
        );
        flowerNumLabels.registerDependencyCondition(combinedCondition);
        parameterTypes.add(flowerNumLabels);

//        ParameterTypeString flowerOutputTopic = new ParameterTypeString(
//                PARAMETER_FLOWER_OUTPUT_TOPIC,
//                "[FLOWER] Optional output topic to publish the model after training completes.",
//                true // optional
//        );
//        flowerOutputTopic.registerDependencyCondition(
//                new EqualTypeCondition(this, PARAMETER_MODE, new String[]{"flower"}, false,0)
//        );
//        parameterTypes.add(flowerOutputTopic);

//        ParameterTypeString flowerBrokerIp = new ParameterTypeString(
//                PARAMETER_FLOWER_BROKER_IP,
//                "[FLOWER] Optional IP of the Kafka broker.",
//                false // optional
//        );
//        flowerBrokerIp.registerDependencyCondition(
//                new EqualTypeCondition(this, PARAMETER_MODE, new String[]{"flower"}, false,0)
//        );
//        parameterTypes.add(flowerBrokerIp);

        // 2c. Training
        ParameterTypeInt flowerNumClients = new ParameterTypeInt(
                PARAMETER_FLOWER_NUM_CLIENTS,
                "[FLOWER] Total number of clients in the federation. (Required if mode=flower)",
                1,
                Integer.MAX_VALUE,
                true
        );



        flowerNumClients.registerDependencyCondition(combinedCondition);
//        flowerNumClients.registerDependencyCondition(
//                new EqualTypeCondition(this, PARAMETER_MODE, new String[]{"flower"}, false,0)
//        );

        parameterTypes.add(flowerNumClients);

        ParameterTypeInt flowerClientsPerRound = new ParameterTypeInt(
                PARAMETER_FLOWER_CLIENTS_PER_ROUND,
                "[FLOWER] Number of clients participating in each training round. (Required if mode=flower)",
                1,
                Integer.MAX_VALUE,
                true
        );
        flowerClientsPerRound.registerDependencyCondition(combinedCondition);
        parameterTypes.add(flowerClientsPerRound);

        ParameterTypeInt flowerBatchSize = new ParameterTypeInt(
                PARAMETER_FLOWER_BATCH_SIZE,
                "[FLOWER] Batch size for data processing. (Required if mode=flower)",
                1,
                Integer.MAX_VALUE,
                true
        );
        flowerBatchSize.registerDependencyCondition(combinedCondition);
        parameterTypes.add(flowerBatchSize);

        ParameterTypeInt flowerTotalRounds = new ParameterTypeInt(
                PARAMETER_FLOWER_TOTAL_ROUNDS,
                "[FLOWER] Total number of federated training rounds. (Required if mode=flower)",
                1,
                Integer.MAX_VALUE,
                true
        );
        flowerTotalRounds.registerDependencyCondition(combinedCondition);
        parameterTypes.add(flowerTotalRounds);

        ParameterTypeCategory flowerFdaApproach = new ParameterTypeCategory(
                PARAMETER_FLOWER_FDA_APPROACH,
                "[FLOWER] FDA approach to use. (Required if mode=flower)",
                new String[]{"Naive", "Linear", "Sketch"},
                0,
                false
        );
        flowerFdaApproach.registerDependencyCondition(combinedCondition);
        parameterTypes.add(flowerFdaApproach);

        ParameterTypeDouble flowerThreshold = new ParameterTypeDouble(
                PARAMETER_FLOWER_THRESHOLD,
                "[FLOWER] Threshold (Theta) for Round Termination Condition. (Required if mode=flower)",
                0.0,
                Double.MAX_VALUE,
                true
        );
        flowerThreshold.registerDependencyCondition(combinedCondition);
        parameterTypes.add(flowerThreshold);

        // 2d. Server
        ParameterTypeString flowerServerIp = new ParameterTypeString(
                PARAMETER_FLOWER_SERVER_IP,
                "[FLOWER] Server IP address. (Required if mode=flower)",
                true
        );
        flowerServerIp.registerDependencyCondition(
                new EqualTypeCondition(this, PARAMETER_MODE, new String[]{"flower"}, false,0)
        );
        parameterTypes.add(flowerServerIp);

        ParameterTypeInt flowerServerFlPort = new ParameterTypeInt(
                PARAMETER_FLOWER_SERVER_FL_PORT,
                "[FLOWER] Federated Learning Server port number. (Required if mode=flower)",
                1,
                Integer.MAX_VALUE,
                true
        );
        flowerServerFlPort.registerDependencyCondition(
                new EqualTypeCondition(this, PARAMETER_MODE, new String[]{"flower"}, false,0)
        );
        parameterTypes.add(flowerServerFlPort);

        ParameterTypeInt flowerServerMetricPort = new ParameterTypeInt(
                PARAMETER_FLOWER_SERVER_METRIC_PORT,
                "[FLOWER] Metric Server port number. (Required if mode=flower)",
                1,
                Integer.MAX_VALUE,
                true
        );
        flowerServerMetricPort.registerDependencyCondition(combinedCondition);
        parameterTypes.add(flowerServerMetricPort);

        // 2e. Clients (network)
        ParameterTypeList flowerClientsNetwork = new ParameterTypeList(
                PARAMETER_FLOWER_CLIENTS_NETWORK,
                "[FLOWER] List of Client network details (Required if mode=flower). " +
                        "Each entry: ip, port, (optional id).",
                new ParameterTypeString("ip", "IP address for the client.", false),
                new ParameterTypeString("port_or_id",
                        "Enter 'port:id' or just 'port'. We'll parse accordingly.", false)
        );
        flowerClientsNetwork.registerDependencyCondition(combinedCondition);
        parameterTypes.add(flowerClientsNetwork);

        // --------------------------------------------------------------------
        // 3. Label heading for NS3 parameters (shown conditionally)
        // --------------------------------------------------------------------
        AndParameterCondition ns3CombinedCondition = new AndParameterCondition(
                this, false, ns3ModeCondition, ns3SimulationCondition
        );

        // --------------------------------------------------------------------
        // 4. NS3 PARAMETERS
        // --------------------------------------------------------------------

        ParameterTypeInt ns3NumClients = new ParameterTypeInt(
                PARAMETER_NS3_NUM_CLIENTS,
                "[NS3] Total number of clients in the federation. (Required if mode=ns3)",
                1,
                Integer.MAX_VALUE,
                true
        );
        ns3NumClients.registerDependencyCondition(ns3CombinedCondition);
        parameterTypes.add(ns3NumClients);

        ParameterTypeInt ns3ClientsPerRound = new ParameterTypeInt(
                PARAMETER_NS3_CLIENTS_PER_ROUND,
                "[NS3] Number of clients participating in each training round. (Required if mode=ns3)",
                1,
                Integer.MAX_VALUE,
                true
        );
        ns3ClientsPerRound.registerDependencyCondition(ns3CombinedCondition);
        parameterTypes.add(ns3ClientsPerRound);

        ParameterTypeDouble ns3ModelSize = new ParameterTypeDouble(
                "NS3_model_size",
                "[NS3] Model Size of federated training rounds. (Required if mode=ns3)",
                1,
                Double.MAX_VALUE,
                true
        );
        ns3ModelSize.registerDependencyCondition(ns3CombinedCondition);
        parameterTypes.add(ns3ModelSize);

        ParameterTypeInt ns3TotalRounds = new ParameterTypeInt(
                PARAMETER_NS3_TOTAL_ROUNDS,
                "[NS3] Total number of federated training rounds. (Required if mode=ns3)",
                1,
                Integer.MAX_VALUE,
                true
        );
        ns3TotalRounds.registerDependencyCondition(ns3CombinedCondition);
        parameterTypes.add(ns3TotalRounds);

        ParameterTypeCategory ns3FdaApproach = new ParameterTypeCategory(
                PARAMETER_NS3_FDA_APPROACH,
                "[NS3] FDA approach to use. (Required if mode=ns3)",
                new String[]{"Naive", "Linear", "Sketch", "Sync"},
                0,
                false
        );
        ns3FdaApproach.registerDependencyCondition(ns3CombinedCondition);
        parameterTypes.add(ns3FdaApproach);

//        ParameterTypeString ns3DatasetName = new ParameterTypeString(
//                PARAMETER_NS3_DATASET_NAME,
//                "[NS3] The dataset to be used. (Required if mode=ns3)",
//                true
//        );
//        ns3DatasetName.registerDependencyCondition(ns3CombinedCondition);
//        parameterTypes.add(ns3DatasetName);

//        ParameterTypeCategory ns3NeuralNetworkName = new ParameterTypeCategory(
//                PARAMETER_NS3_NEURAL_NETWORK_NAME,
//                "[NS3] The name of the neural network to be used. (Required if mode=ns3)",
//                new String[]{"Lenet", "VGG16", "ResNet", "AlexNet"},
//                0,
//                false
//        );
//        ns3NeuralNetworkName.registerDependencyCondition(ns3CombinedCondition);
//        parameterTypes.add(ns3NeuralNetworkName);

//        ParameterTypeInt ns3NumLabels = new ParameterTypeInt(
//                PARAMETER_NS3_NUM_LABELS,
//                "[NS3] Number of output labels for the task. (Required if mode=ns3)",
//                1,
//                Integer.MAX_VALUE,
//                true
//        );
//        ns3NumLabels.registerDependencyCondition(ns3CombinedCondition);
//        parameterTypes.add(ns3NumLabels);

        // 3b. Network
        ParameterTypeDouble ns3TxGain = new ParameterTypeDouble(
                PARAMETER_NS3_TX_GAIN,
                "[NS3] TX Gain for wifi transmissions. (Required if mode=ns3)",
                0.0,
                Double.MAX_VALUE,
                true
        );
        ns3TxGain.registerDependencyCondition(ns3CombinedCondition);
        parameterTypes.add(ns3TxGain);

        ParameterTypeString ns3ServerDatarate = new ParameterTypeString(
                PARAMETER_NS3_SERVER_DATARATE,
                "[NS3] Server datarate (e.g. '10Mbps'). (Required if mode=ns3)",
                true
        );
        ns3ServerDatarate.registerDependencyCondition(ns3CombinedCondition);
        parameterTypes.add(ns3ServerDatarate);

        ParameterTypeInt ns3MaxPacketSize = new ParameterTypeInt(
                PARAMETER_NS3_MAX_PACKET_SIZE,
                "[NS3] Maximum packet size (bytes). (Required if mode=ns3)",
                1,
                Integer.MAX_VALUE,
                true
        );
        ns3MaxPacketSize.registerDependencyCondition(ns3CombinedCondition);
        parameterTypes.add(ns3MaxPacketSize);

        ParameterTypeCategory ns3WifiPerformance = new ParameterTypeCategory(
                PARAMETER_NS3_WIFI_PERFORMANCE,
                "[NS3] Wifi performance standard: low, basic, high. (Required if mode=ns3)",
                new String[]{"low", "basic", "high"},
                1,
                false
        );
        ns3WifiPerformance.registerDependencyCondition(ns3CombinedCondition);
        parameterTypes.add(ns3WifiPerformance);

        ParameterTypeFile ns3Topology = new ParameterTypeFile(
                PARAMETER_NS3_TOPOLOGY,
                "[NS3] (Optional) Path to JSON file describing the topology.",
                "json",
                true // optional
        );
        ns3Topology.registerDependencyCondition(ns3CombinedCondition);
        parameterTypes.add(ns3Topology);

        ParameterTypeInt ns3SimulationParallelism = new ParameterTypeInt(
                PARAMETER_NS3_SIMULATION_PARALLELISM,
                "[NS3] Assign 1 CPU per simulation round. Should not exceed # of CPUs in machine.",
                0,
                1000,
                true
        );
        ns3SimulationParallelism.registerDependencyCondition(ns3CombinedCondition);
        parameterTypes.add(ns3SimulationParallelism);

        ParameterTypeBoolean ns3SimulationForceRun = new ParameterTypeBoolean(
                PARAMETER_NS3_SIMULATION_FORCE_RUN,
                "[NS3] Whether to re-run simulation even if identical results already exist.",
                true,
                false
        );
        ns3SimulationForceRun.registerDependencyCondition(ns3CombinedCondition);
        parameterTypes.add(ns3SimulationForceRun);


        // Output fields
        ParameterTypeString outputType = new ParameterTypeString(
                "output_type", "Type of the output (e.g. artifact)", false);
//        outputType.registerDependencyCondition(opModeCondition);
        parameterTypes.add(outputType);

        ParameterTypeString outputTopicOut = new ParameterTypeString(
                "Kafka_output_topic_name", "Kafka topic name for output", false);
        parameterTypes.add(outputTopicOut);

        ParameterTypeString kafkaIp = new ParameterTypeString(
                "kafka_broker_ip", "IP address of the Kafka broker", false);

        parameterTypes.add(kafkaIp);

        ParameterTypeInt kafkaPort = new ParameterTypeInt(
                "kafka_broker_port", "Port number of the Kafka broker", 0, 65535);


        parameterTypes.add(kafkaPort);

        return parameterTypes;
    }

    /**
     * Generates the JSON according to the new schema:
     *
     * {
     *   "mode": "...",
     *   "flower": {
     *       "dataset": { ... },
     *       "model": { ... },
     *       "training": { ... },
     *       "server": { ... },
     *       "clients": { ... }
     *   },
     *   "ns3": {
     *       "federated_learning": { ... },
     *       "network": { ... }
     *   }
     * }
     */
    private String generateJSON() throws OperatorException, JsonProcessingException {

//        validateParameters();
        // Grab the mode:
        String mode = getParameterAsString(PARAMETER_MODE);

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();

        // Always put "mode"
        rootNode.put("mode", mode);

        // We will fill either "flower" or "ns3" (the other will be empty).
        ObjectNode flowerNode = mapper.createObjectNode();
        ObjectNode ns3Node = mapper.createObjectNode();

        // --------------------------------------------------------------------
        // Build FLOWER object if mode == "flower"
        // --------------------------------------------------------------------
        if ("flower".equals(mode)) {

            if ("Inference".equals(getParameterAsString(PARAMETER_OPERATION_MODE))) {// =============== DATASET SECTION ===============
                ObjectNode datasetNode = mapper.createObjectNode();

                // required
                String dsName = getParameterAsString(PARAMETER_FLOWER_DATASET_NAME);
                String dataPath = getParameterAsString(PARAMETER_FLOWER_DATASET_PATH);

                if (Objects.equals(getParameterAsString(PARAMETER_OPERATION_MODE), "Inference"))
                    datasetNode.put("dataset_name", dataPath);
                else
                    datasetNode.put("dataset_name", dsName);
                // optional
                if (isParameterSet(PARAMETER_FLOWER_DIRICHLET_ALPHA)) {
                    double alpha = getParameterAsDouble(PARAMETER_FLOWER_DIRICHLET_ALPHA);
                    datasetNode.put("dirichlet_alpha", alpha);
                }

                // optional client_data_locations
                if (isParameterSet(PARAMETER_FLOWER_CLIENT_DATA_LOCATIONS)) {
                    List<String[]> clientLocations = getParameterList(PARAMETER_FLOWER_CLIENT_DATA_LOCATIONS);
                    if (!clientLocations.isEmpty()) {
                        ArrayNode clientDataArray = mapper.createArrayNode();
                        for (String[] item : clientLocations) {
                            // item[0] = client_id, item[1] = path
                            ObjectNode locNode = mapper.createObjectNode();
                            locNode.put("client_id", Integer.parseInt(item[0]));
                            locNode.put("path", item[1]);
                            clientDataArray.add(locNode);
                        }
                        datasetNode.set("client_data_locations", clientDataArray);
                    }
                }

                // =============== MODEL SECTION ===============
                ObjectNode modelNode = mapper.createObjectNode();

                // required
                String nnName = getParameterAsString(PARAMETER_FLOWER_NEURAL_NETWORK_NAME);
                modelNode.put("neural_network_name", nnName);
//
//            int numLabels = getParameterAsInt(PARAMETER_FLOWER_NUM_LABELS);
//            modelNode.put("num_labels", numLabels);

//            // optional
//            if (isParameterSet(PARAMETER_FLOWER_OUTPUT_TOPIC)) {
//                modelNode.put("output_topic", getParameterAsString(PARAMETER_FLOWER_OUTPUT_TOPIC));
//            }
//            if (isParameterSet(PARAMETER_FLOWER_BROKER_IP)) {
//                modelNode.put("broker_ip", getParameterAsString(PARAMETER_FLOWER_BROKER_IP));
//            }

                // =============== TRAINING SECTION ===============
                ObjectNode trainingNode = mapper.createObjectNode();

                // required
                trainingNode.put("num_clients", getParameterAsInt(PARAMETER_FLOWER_NUM_CLIENTS));
                trainingNode.put("clients_per_round", getParameterAsInt(PARAMETER_FLOWER_CLIENTS_PER_ROUND));
                trainingNode.put("batch_size", getParameterAsInt(PARAMETER_FLOWER_BATCH_SIZE));
                trainingNode.put("total_rounds", getParameterAsInt(PARAMETER_FLOWER_TOTAL_ROUNDS));

                trainingNode.put("fda_approach", getParameterAsString(PARAMETER_FLOWER_FDA_APPROACH));

                // threshold
                trainingNode.put("threshold", getParameterAsDouble(PARAMETER_FLOWER_THRESHOLD));

                // =============== SERVER SECTION ===============
                ObjectNode serverNode = mapper.createObjectNode();
                ObjectNode serverNetworkNode = mapper.createObjectNode();

                // required
                serverNetworkNode.put("ip", getParameterAsString(PARAMETER_FLOWER_SERVER_IP));
                serverNetworkNode.put("fl_server_port", getParameterAsInt(PARAMETER_FLOWER_SERVER_FL_PORT));
                serverNetworkNode.put("metric_server_port", getParameterAsInt(PARAMETER_FLOWER_SERVER_METRIC_PORT));

                serverNode.set("server_network", serverNetworkNode);

                // =============== CLIENTS SECTION ===============
                ObjectNode clientsNode = mapper.createObjectNode();
                ArrayNode clientsArray = mapper.createArrayNode();

                if (isParameterSet(PARAMETER_FLOWER_CLIENTS_NETWORK)) {
                    List<String[]> clientNetworks = getParameterList(PARAMETER_FLOWER_CLIENTS_NETWORK);
                    for (String[] net : clientNetworks) {
                        // net[0] => ip
                        // net[1] => "port:id" or just "port"
                        ObjectNode clientNetNode = mapper.createObjectNode();
                        clientNetNode.put("ip", net[0]);

                        // parse net[1]
                        String[] portAndMaybeId = net[1].split(":");
                        try {
                            // first is always port
                            clientNetNode.put("port", Integer.parseInt(portAndMaybeId[0]));
                        } catch (NumberFormatException e) {
                            clientNetNode.put("port", 0);
                        }
                        if (portAndMaybeId.length > 1) {
                            // second is the optional id
                            try {
                                int cId = Integer.parseInt(portAndMaybeId[1]);
                                clientNetNode.put("id", cId);
                            } catch (NumberFormatException ex) {
                                // skip if not a valid number
                            }
                        }
                        clientsArray.add(clientNetNode);
                    }
                }
                clientsNode.set("clients_network", clientsArray);
                // Build the "output" section
                ObjectNode outputNode = mapper.createObjectNode();
                outputNode.put("type", getParameterAsString("output_type"));
                outputNode.put("topic_out", getParameterAsString("Kafka_output_topic_name"));


                ObjectNode kafkaBrokerNode = mapper.createObjectNode();
                kafkaBrokerNode.put("ip", getParameterAsString("kafka_broker_ip"));
                kafkaBrokerNode.put("port", getParameterAsInt("kafka_broker_port"));

                outputNode.set("kafka_broker", kafkaBrokerNode);

                // Optional or required depending on your logic
                if (isParameterSet(PARAMETER_UNIQUE_NAME)) {
                    outputNode.put("inference_model_id", getParameterAsString(PARAMETER_UNIQUE_NAME));
                }
                String opMode = getParameterAsString(PARAMETER_OPERATION_MODE).toLowerCase();
                // =============== Assemble FLOWER node ===============
                flowerNode.set("dataset", datasetNode);
                flowerNode.set("model", modelNode);
                flowerNode.set("training", trainingNode);
                flowerNode.set("server", serverNode);
                flowerNode.set("clients", clientsNode);
                flowerNode.set("output", outputNode);
                flowerNode.put("mode", opMode);

            }


        } // end if (mode=flower)

        // --------------------------------------------------------------------
        // Build NS3 object if mode == "ns3"
        // --------------------------------------------------------------------
        if ("ns3".equals(mode)) {
            // =============== FEDERATED LEARNING ===============
            ObjectNode fedLearningNode = mapper.createObjectNode();
            String ns3OpMode = getParameterAsString("NS3_operation_mode");

            if (ns3OpMode.equals("Simulation")) {
                LOGGER.info("simulatiooooon");
                fedLearningNode.put("num_clients", getParameterAsInt(PARAMETER_NS3_NUM_CLIENTS));
                fedLearningNode.put("clients_per_round", getParameterAsInt(PARAMETER_NS3_CLIENTS_PER_ROUND));
                fedLearningNode.put("total_rounds", getParameterAsInt(PARAMETER_NS3_TOTAL_ROUNDS));
                fedLearningNode.put("fda_approach", getParameterAsString(PARAMETER_NS3_FDA_APPROACH));
//                fedLearningNode.put("dataset_name", getParameterAsString(PARAMETER_NS3_DATASET_NAME));
//                fedLearningNode.put("neural_network_name", getParameterAsString(PARAMETER_NS3_NEURAL_NETWORK_NAME));
//                fedLearningNode.put("num_labels", getParameterAsInt(PARAMETER_NS3_NUM_LABELS));

                // =============== NETWORK ===============
                ObjectNode networkNode = mapper.createObjectNode();
                networkNode.put("tx_gain", getParameterAsDouble(PARAMETER_NS3_TX_GAIN));
                networkNode.put("server_datarate", getParameterAsString(PARAMETER_NS3_SERVER_DATARATE));
                networkNode.put("max_packet_size", getParameterAsInt(PARAMETER_NS3_MAX_PACKET_SIZE));
                networkNode.put("wifi_performance_standard", getParameterAsString(PARAMETER_NS3_WIFI_PERFORMANCE));

                // optional "topology" TODO
//                if (isParameterSet(PARAMETER_NS3_TOPOLOGY)) {
//                    // getParameterAsFile(...) returns a File object referencing the user's chosen .json
//                    File topologyFile = getParameterAsFile(PARAMETER_NS3_TOPOLOGY);
//
//                    try {
//                        // Read the file contents
//                        String topologyContent = FileUtils.readFileToString(topologyFile, StandardCharsets.UTF_8);
//
//                        // Parse the file’s JSON into a JsonNode
//                        JsonNode topologyNode = mapper.readTree(topologyContent);
//
//                        // Insert it as an actual JSON object, e.g.:
//                        networkNode.set("topology", topologyNode);
//
//                    } catch (IOException e) {
//                        // If the file cannot be read or is invalid JSON
//                        throw new OperatorException("Failed to read/parse NS3 topology JSON file: "
//                                + topologyFile.getAbsolutePath(), e);
//                    }
//                }

                ObjectNode simulation = mapper.createObjectNode();
                if (isParameterSet(PARAMETER_NS3_SIMULATION_PARALLELISM))
                    simulation.put("parallelism", getParameterAsInt(PARAMETER_NS3_SIMULATION_PARALLELISM));


                if (isParameterSet(PARAMETER_NS3_SIMULATION_FORCE_RUN))
                    simulation.put("forceRun", getParameterAsBoolean(PARAMETER_NS3_SIMULATION_FORCE_RUN));

                // =============== Assemble NS3 node ===============
                ns3Node.set("federated_learning", fedLearningNode);
                ns3Node.set("network", networkNode);
                ns3Node.set("simulation", simulation);
            }
            // Build the "output" section
            ObjectNode ns3outputNode = mapper.createObjectNode();
//            ns3outputNode.put("type", getParameterAsString("output_type"));
            ns3outputNode.put("topic_out", getParameterAsString("Kafka_output_topic_name"));

            ObjectNode kafkaBrokerNode = mapper.createObjectNode();
            kafkaBrokerNode.put("ip", getParameterAsString("kafka_broker_ip"));
            kafkaBrokerNode.put("port", getParameterAsInt("kafka_broker_port"));

            ns3outputNode.set("kafka_broker", kafkaBrokerNode);

            // Optional or required depending on your logic
            if (isParameterSet(PARAMETER_UNIQUE_NAME)) {
                ns3outputNode.put("inference_model_id", getParameterAsString(PARAMETER_UNIQUE_NAME));
            }
            ns3Node.set("output", ns3outputNode);
            ns3Node.put("mode", getParameterAsString(PARAMETER_OPERATION_MODE).toLowerCase());

        }

        // Add the sub-objects to root. The one not selected remains empty.
        rootNode.set("flower", flowerNode);
        rootNode.set("ns3", ns3Node);

        // Convert to String
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
    }

    @Override
    public Pair<StreamGraph, List<StreamDataContainer>> getStreamDataInputs() throws UserError {
        StreamGraph graph = ((StreamingNest) getExecutionUnit().getEnclosingOperator()).getGraph();
        return new Pair<>(graph, Collections.emptyList());
    }

    @Override
    public List<StreamProducer> addToGraph(StreamGraph graph, List<StreamDataContainer> streamDataInputs) {
        String jsonContent;
        try {
            jsonContent = generateJSON();
        } catch (UndefinedParameterError | JsonProcessingException e) {
            throw new RuntimeException(e);
        } catch (OperatorException e) {
            throw new RuntimeException(e);
        }

        // Create a new stream producer for the JSON data
        JsonDataProducer producer = new JsonDataProducer.Builder(graph)
                .withJsonData(jsonContent)
                .build();
        graph.registerSource(producer);
        return Collections.singletonList(producer);
    }

    @Override
    public void deliverStreamDataOutputs(StreamGraph graph, List<StreamProducer> streamProducers) throws UserError {
        StreamDataContainer outData = new StreamDataContainer(graph, streamProducers.get(0));
        jsonOutput.deliver(outData);
    }

    @Override
    public long getId() {
        return 0;
    }

    @Override
    public void accept(StreamGraphNodeVisitor visitor) {
        // no-op
    }

    @Override
    public void doWork() throws OperatorException {
//        Pair<StreamGraph, List<StreamDataContainer>> inputs = getStreamDataInputs();
//        StreamGraph graph = inputs.getFirst();
//        logProcessing(graph.getName());
//
//        List<StreamProducer> streamProducers = addToGraph(graph, inputs.getSecond());
//        deliverStreamDataOutputs(graph, streamProducers);
        if (getExecutionUnit().getEnclosingOperator() instanceof StreamingNest || getExecutionUnit().getEnclosingOperator() instanceof StreamingOptimizationOperator) {
            // Streaming nest behavior: use the graph-based approach.
            Pair<StreamGraph, List<StreamDataContainer>> inputs = getStreamDataInputs();
            StreamGraph graph = inputs.getFirst();
            logProcessing(graph.getName());
            List<StreamProducer> streamProducers = addToGraph(graph, inputs.getSecond());
            deliverStreamDataOutputs(graph, streamProducers);
        } else {
            // Not inside a streaming nest: just output JSON directly.
            try {
                String jsonContent = generateJSON();
                ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonNode = mapper.readTree(jsonContent);
                // Create a single attribute called "json"
                Attribute jsonAttr = AttributeFactory.createAttribute("json", Ontology.STRING);
                ExampleSetBuilder builder = ExampleSets.from(jsonAttr).withExpectedSize(1);

                double[] row = new double[1];
                row[0] = jsonAttr.getMapping().mapString(jsonContent);

                builder.addRow(row);

                jsonOutput.deliver(builder.build());
//                logger.info("Standalone JSON output: " + jsonContent);
//                // Deliver the JSON string directly on the output port.
//                // (Depending on your output port configuration, you might need to adjust the port type.)
//                TableBuilder builder = Builders.newTableBuilder(1);
//
//                // 3) Add a single nominal column for the JSON data
//                builder.addNominal("json_column", i -> jsonContent);
//
//                // 4) Build the table (in this case, no parallelism is necessary)
//                Table table = builder.build(BeltTools.getContext(this));
//
//                // 5) Wrap the table in an IOTable
//                IOTable ioTable = new IOTable(table);
//                jsonOutput.deliver(ioTable);

            } catch (Exception e) {
                throw new OperatorException(e.getMessage(), e);
            }
        }
    }


    private void validateParameters() throws OperatorException {
        // Get the mode early on so you know which parameters to check.
        String mode = getParameterAsString(PARAMETER_MODE);

//        if ("flower".equals(mode)) {
//            // Check Flower mode required parameters:
//            if (!isParameterSet(PARAMETER_FLOWER_DATASET_NAME) ||
//                    getParameterAsString(PARAMETER_FLOWER_DATASET_NAME).trim().isEmpty()) {
//                throw new OperatorException("Missing required parameter: " + PARAMETER_FLOWER_DATASET_NAME);
//            }
//            if (!isParameterSet(PARAMETER_FLOWER_NEURAL_NETWORK_NAME) ||
//                    getParameterAsString(PARAMETER_FLOWER_NEURAL_NETWORK_NAME).trim().isEmpty()) {
//                throw new OperatorException("Missing required parameter: " + PARAMETER_FLOWER_NEURAL_NETWORK_NAME);
//            }
//            // Add additional checks for each required parameter in Flower mode...
//            if (!isParameterSet(PARAMETER_FLOWER_NUM_LABELS)) {
//                throw new OperatorException("Missing required parameter: " + PARAMETER_FLOWER_NUM_LABELS);
//            }
//            if (!isParameterSet(PARAMETER_FLOWER_NUM_CLIENTS)) {
//                throw new OperatorException("Missing required parameter: " + PARAMETER_FLOWER_NUM_CLIENTS);
//            }
//            if (!isParameterSet(PARAMETER_FLOWER_CLIENTS_PER_ROUND)) {
//                throw new OperatorException("Missing required parameter: " + PARAMETER_FLOWER_CLIENTS_PER_ROUND);
//            }
//            if (!isParameterSet(PARAMETER_FLOWER_BATCH_SIZE)) {
//                throw new OperatorException("Missing required parameter: " + PARAMETER_FLOWER_BATCH_SIZE);
//            }
//            if (!isParameterSet(PARAMETER_FLOWER_TOTAL_ROUNDS)) {
//                throw new OperatorException("Missing required parameter: " + PARAMETER_FLOWER_TOTAL_ROUNDS);
//            }
//            if (!isParameterSet(PARAMETER_FLOWER_FDA_APPROACH) ||
//                    getParameterAsString(PARAMETER_FLOWER_FDA_APPROACH).trim().isEmpty()) {
//                throw new OperatorException("Missing required parameter: " + PARAMETER_FLOWER_FDA_APPROACH);
//            }
//            if (!isParameterSet(PARAMETER_FLOWER_THRESHOLD)) {
//                throw new OperatorException("Missing required parameter: " + PARAMETER_FLOWER_THRESHOLD);
//            }
//            if (!isParameterSet(PARAMETER_FLOWER_SERVER_IP) ||
//                    getParameterAsString(PARAMETER_FLOWER_SERVER_IP).trim().isEmpty()) {
//                throw new OperatorException("Missing required parameter: " + PARAMETER_FLOWER_SERVER_IP);
//            }
//            if (!isParameterSet(PARAMETER_FLOWER_SERVER_FL_PORT)) {
//                throw new OperatorException("Missing required parameter: " + PARAMETER_FLOWER_SERVER_FL_PORT);
//            }
//            if (!isParameterSet(PARAMETER_FLOWER_SERVER_METRIC_PORT)) {
//                throw new OperatorException("Missing required parameter: " + PARAMETER_FLOWER_SERVER_METRIC_PORT);
//            }
//            if (!isParameterSet(PARAMETER_FLOWER_CLIENTS_NETWORK)) {
//                throw new OperatorException("Missing required parameter: " + PARAMETER_FLOWER_CLIENTS_NETWORK);
//            }
//        } else if ("ns3".equals(mode)) {
//            if ("Simulation".equals(getParameterAsString("NS3_operation_mode"))) {
//            // Check NS3 mode required parameters:
//            if (!isParameterSet(PARAMETER_NS3_NUM_CLIENTS)) {
//                throw new OperatorException("Missing required parameter: " + PARAMETER_NS3_NUM_CLIENTS);
//            }
//            if (!isParameterSet(PARAMETER_NS3_CLIENTS_PER_ROUND)) {
//                throw new OperatorException("Missing required parameter: " + PARAMETER_NS3_CLIENTS_PER_ROUND);
//            }
//            if (!isParameterSet(PARAMETER_NS3_TOTAL_ROUNDS)) {
//                throw new OperatorException("Missing required parameter: " + PARAMETER_NS3_TOTAL_ROUNDS);
//            }
//            if (!isParameterSet(PARAMETER_NS3_FDA_APPROACH) ||
//                    getParameterAsString(PARAMETER_NS3_FDA_APPROACH).trim().isEmpty()) {
//                throw new OperatorException("Missing required parameter: " + PARAMETER_NS3_FDA_APPROACH);
//            }
//            if (!isParameterSet(PARAMETER_NS3_DATASET_NAME) ||
//                    getParameterAsString(PARAMETER_NS3_DATASET_NAME).trim().isEmpty()) {
//                throw new OperatorException("Missing required parameter: " + PARAMETER_NS3_DATASET_NAME);
//            }
//            if (!isParameterSet(PARAMETER_NS3_NEURAL_NETWORK_NAME) ||
//                    getParameterAsString(PARAMETER_NS3_NEURAL_NETWORK_NAME).trim().isEmpty()) {
//                throw new OperatorException("Missing required parameter: " + PARAMETER_NS3_NEURAL_NETWORK_NAME);
//            }
//            if (!isParameterSet(PARAMETER_NS3_NUM_LABELS)) {
//                throw new OperatorException("Missing required parameter: " + PARAMETER_NS3_NUM_LABELS);
//            }
//            if (!isParameterSet(PARAMETER_NS3_TX_GAIN)) {
//                throw new OperatorException("Missing required parameter: " + PARAMETER_NS3_TX_GAIN);
//            }
//            if (!isParameterSet(PARAMETER_NS3_SERVER_DATARATE) ||
//                    getParameterAsString(PARAMETER_NS3_SERVER_DATARATE).trim().isEmpty()) {
//                throw new OperatorException("Missing required parameter: " + PARAMETER_NS3_SERVER_DATARATE);
//            }
//            if (!isParameterSet(PARAMETER_NS3_MAX_PACKET_SIZE)) {
//                throw new OperatorException("Missing required parameter: " + PARAMETER_NS3_MAX_PACKET_SIZE);
//            }
//            if (!isParameterSet(PARAMETER_NS3_WIFI_PERFORMANCE) ||
//                    getParameterAsString(PARAMETER_NS3_WIFI_PERFORMANCE).trim().isEmpty()) {
//                throw new OperatorException("Missing required parameter: " + PARAMETER_NS3_WIFI_PERFORMANCE);
//            }
//        }
//    }
    }

}

