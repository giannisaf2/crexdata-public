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
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.container.Pair;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class FederatedLearning extends AbstractStreamOperator implements StreamSource {
    private static final Logger logger =  LogService.getRoot();
    private final OutputPort jsonOutput = getOutputPorts().createPort("output stream 1");
    // model
    private static final String PARAMETER_CHECKPOINT = "Checkpoint";
    private static final String PARAMETER_NUM_LABELS = "Number_of_labels";

    // dataset
    private static final String PARAMETER_PATH = "Dataset_path";
    private static final String PARAMETER_NAME = "Dataset_name";
    private static final String PARAMETER_BATCH_SIZE = "Batch_size";
    private static final String PARAMETER_DIRICHLET_ALPHA = "Dirichlet_alpha";

    // training
    private static final String PARAMETER_NUM_CLIENTS = "Number_of_clients";
    private static final String PARAMETER_CLIENTS_PER_ROUND = "Clients_per_round";
    private static final String PARAMETER_LOCAL_EPOCHS = "Local_epochs";
    private static final String PARAMETER_TOTAL_ROUNDS = "Total_rounds";

    // server
    private static final String PARAMETER_SERVER_IP = "Server_network_IP";
    private static final String PARAMETER_SERVER_PORT = "Server_network_Port";
    private static final String PARAMETER_SERVER_STRATEGY = "Server_Strategy";
    private static final String PARAMETER_STRATEGY_FDA = "FDA";
    private static final String PARAMETER_STRATEGY_ETA = "eta";

    // clients
    private static final String PARAMETER_CLIENTS_NETWORK = "Client_networks";
    private static final String PARAMETER_CLIENTS_LR = "lr";

    public FederatedLearning(OperatorDescription description) {
        super(description);
        getTransformer().addGenerationRule(jsonOutput, StreamDataContainer.class);
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> parameterTypes = super.getParameterTypes();


        // Model Section
        parameterTypes.add(new ParameterTypeString(PARAMETER_CHECKPOINT,
                "HuggingFace model checkpoint to use (e.g., 'roberta-base').", false));
        parameterTypes.add(new ParameterTypeInt(PARAMETER_NUM_LABELS,
                "Number of output labels for the task.", 1, Integer.MAX_VALUE, false));

        // Dataset Section
        parameterTypes.add(new ParameterTypeString(PARAMETER_PATH,
                "HuggingFace path or name of the dataset (e.g., 'glue').", false));
        parameterTypes.add(new ParameterTypeInt(PARAMETER_BATCH_SIZE,
                "Batch size for data processing.", 1, Integer.MAX_VALUE, false));
        parameterTypes.add(new ParameterTypeDouble(PARAMETER_DIRICHLET_ALPHA,
                "Dirichlet data distribution parameter for non-IID partitioning.", 0.0, Double.MAX_VALUE, false));
        parameterTypes.add(new ParameterTypeString(PARAMETER_NAME,
                "HuggingFace name of the dataset (e.g., 'mrpc').", true));

        // Training Section
        parameterTypes.add(new ParameterTypeInt(PARAMETER_NUM_CLIENTS,
                "Total number of clients in the federation.", 1, Integer.MAX_VALUE, false));
        parameterTypes.add(new ParameterTypeInt(PARAMETER_CLIENTS_PER_ROUND,
                "Number of clients participating in each training round.", 1, Integer.MAX_VALUE, false));
        parameterTypes.add(new ParameterTypeInt(PARAMETER_LOCAL_EPOCHS,
                "Number of epochs each client trains locally.", 1, Integer.MAX_VALUE, false));
        parameterTypes.add(new ParameterTypeInt(PARAMETER_TOTAL_ROUNDS,
                "Total number of federated training rounds.", 1, Integer.MAX_VALUE, false));

        // Server
        parameterTypes.add(new ParameterTypeString(PARAMETER_SERVER_IP,
                "Server IP address", false));
        parameterTypes.add(new ParameterTypeInt(PARAMETER_SERVER_PORT,
                "Port number for communication.", 0, 99999, false));

        parameterTypes.add(new ParameterTypeString("IP_pull_socket",
                "Server IP address", false));
        parameterTypes.add(new ParameterTypeInt("Port_pull_socket",
                "Port number for communication.", 0, 99999, false));

        parameterTypes.add(new ParameterTypeCategory(PARAMETER_SERVER_STRATEGY,
                "Name of the federated strategy.",
                new String[]{"FedAvg", "FedYogi", "FedAdagrad", "FedAvgM", "FedAdam", "FedAdamW"}, 0, true));

        parameterTypes.add(new ParameterTypeBoolean(PARAMETER_STRATEGY_FDA,
                "Whether or not to use FDA extension.", false, false));
        parameterTypes.add(new ParameterTypeDouble(PARAMETER_STRATEGY_ETA,
                "Server-side learning rate hyperparameter.", 0.0, Double.MAX_VALUE, true));


        // Clients
        ParameterTypeEnumeration clientAddresses = new ParameterTypeEnumeration(
                "Addresses and datapaths",
                "Address and data path for each client, formatted as 'ip:port|dataPath'. " +
                        "Multiple entries can be separated by commas.",
                new ParameterTypeTupel("Address|Datapath", "Describing the client address and data path",
                        new ParameterTypeString("Address", "IP address and port of the client, formatted as 'ip:port'"),
                        new ParameterTypeString("Data path", "Path to the client's data directory, e.g., '/home/data'")
                )
        );
        ParameterTypeEnumeration clientNetworkList = new ParameterTypeEnumeration(PARAMETER_CLIENTS_NETWORK,
                "List of client details",
                new ParameterTypeTupel("Clients","tupel describing the computing site",
                        new ParameterTypeInt("id", "Id of client",0, Integer.MAX_VALUE),
                        clientAddresses));

        parameterTypes.add(clientNetworkList);

        parameterTypes.add(new ParameterTypeDouble(PARAMETER_CLIENTS_LR,
                "Learning rate for the client-side optimizer (SGD).",
                0.0, Double.MAX_VALUE, false));

        return parameterTypes;
    }


    private String generateJSON() throws UndefinedParameterError, JsonProcessingException {
        // Create the ObjectMapper
        ObjectMapper mapper = new ObjectMapper();

        // 1. Model
        String checkpoint = getParameterAsString(PARAMETER_CHECKPOINT);
        int numLabels = getParameterAsInt(PARAMETER_NUM_LABELS);
        ObjectNode modelNode = mapper.createObjectNode();
        modelNode.put("num_labels", numLabels);
        modelNode.put("checkpoint", checkpoint);

        // 2. Dataset
        String datasetPath = getParameterAsString(PARAMETER_PATH);
        String datasetName = getParameterAsString(PARAMETER_NAME);
        int batchSize = getParameterAsInt(PARAMETER_BATCH_SIZE);
        double dirichletAlpha = getParameterAsDouble(PARAMETER_DIRICHLET_ALPHA);
        ObjectNode datasetNode = mapper.createObjectNode();
        datasetNode.put("dirichlet_alpha", dirichletAlpha);
        datasetNode.put("batch_size", batchSize);
        datasetNode.put("name", datasetName);
        datasetNode.put("path", datasetPath);


        // 3. Training
        int numClients = getParameterAsInt(PARAMETER_NUM_CLIENTS);
        int clientsPerRound = getParameterAsInt(PARAMETER_CLIENTS_PER_ROUND);
        int localEpochs = getParameterAsInt(PARAMETER_LOCAL_EPOCHS);
        int totalRounds = getParameterAsInt(PARAMETER_TOTAL_ROUNDS);
        ObjectNode trainingNode = mapper.createObjectNode();
        trainingNode.put("total_rounds", totalRounds);
        trainingNode.put("local_epochs", localEpochs);
        trainingNode.put("clients_per_round", clientsPerRound);
        trainingNode.put("num_clients", numClients);


        // 4. Server
        String serverIp = getParameterAsString(PARAMETER_SERVER_IP);
        int serverPort = getParameterAsInt(PARAMETER_SERVER_PORT);
        String strategyName = getParameterAsString(PARAMETER_SERVER_STRATEGY);
        boolean fda = getParameterAsBoolean(PARAMETER_STRATEGY_FDA);
        double eta = getParameterAsDouble(PARAMETER_STRATEGY_ETA);

        ObjectNode serverNode = mapper.createObjectNode();
        ObjectNode serverNetworkNode = mapper.createObjectNode();
        serverNetworkNode.put("port", serverPort);
        serverNetworkNode.put("ip", serverIp);

        serverNode.set("network", serverNetworkNode);

        ObjectNode strategyNode = mapper.createObjectNode();
        strategyNode.put("eta", eta);
        strategyNode.put("fda", fda);
        strategyNode.put("name", strategyName);

        serverNode.set("strategy", strategyNode);

        // 5. Clients
        double clientLr = getParameterAsDouble(PARAMETER_CLIENTS_LR);
        String[] clientNetworks = ParameterTypeEnumeration.transformString2Enumeration(getParameterAsString(PARAMETER_CLIENTS_NETWORK));

        ObjectNode clientsNode = mapper.createObjectNode();
        ArrayNode networkArray = mapper.createArrayNode();

        // Each element in clientNetworks is an array: [ clientID, <List of [Address, Data path]> ]
        for (String entry : clientNetworks) {

            String[] idAddressDataPath = ParameterTypeTupel.transformString2Tupel(entry);
            int clientId = Integer.parseInt(idAddressDataPath[0]);
            LOGGER.info("Client ID: " + clientId);

            List<String> addressDataPaths = ParameterTypeEnumeration.transformString2List(idAddressDataPath[1]);
            String[] addressDataPath = ParameterTypeTupel.transformString2Tupel(addressDataPaths.get(0));
            // The second column is "ipPort|dataPath", e.g. "192.168.48.11:8082|/home/data"
            String ipPort = addressDataPath[0];
            String dataPath = addressDataPath[1];
            LOGGER.info("Client Address: " + ipPort + ", Data Path: " + dataPath);
            // Split ipPort by ":" to separate IP and port
            String[] addressParts = ipPort.split(":");
            String ip = "";
            int portVal = 0;
            if (addressParts.length == 2) {
                ip = addressParts[0].trim();
                portVal = Integer.parseInt(addressParts[1].trim());
            }

            // Build the JSON structure
            ObjectNode clientDetails = mapper.createObjectNode();
            clientDetails.put("data_path", dataPath);
            clientDetails.put("port", portVal);
            clientDetails.put("ip", ip);
            clientDetails.put("id", clientId);

            networkArray.add(clientDetails);
        }

        clientsNode.set("network", networkArray);
        clientsNode.put("lr", clientLr);

        // Build the final JSON
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.set("clients", clientsNode);
        rootNode.set("server", serverNode);
        rootNode.set("training", trainingNode);
        rootNode.set("dataset", datasetNode);
        rootNode.set("model", modelNode);

        String prettyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);

        String escapedJson = prettyJson.replace("\n", "\\n");

        logger.info("Data to be sent: \n " + escapedJson);
        return prettyJson;
    }

    @Override
    public Pair<StreamGraph, List<StreamDataContainer>> getStreamDataInputs() throws UserError {
        StreamGraph graph = ((StreamingNest) getExecutionUnit().getEnclosingOperator()).getGraph();
        return new Pair<>(graph, Collections.emptyList());
    }

    @Override
    public List<StreamProducer> addToGraph(StreamGraph graph, List<StreamDataContainer> streamDataInputs) {
        String jsonContent = null;
        try {
            jsonContent = generateJSON();
        } catch (UndefinedParameterError e) {
            throw new RuntimeException(e);
        } catch (JsonProcessingException e) {
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

    }

    @Override
    public void doWork() throws OperatorException {
//
//        Pair<StreamGraph, List<StreamDataContainer>> inputs = getStreamDataInputs();
//        StreamGraph graph = inputs.getFirst();
//        logProcessing(graph.getName());
//
//        List<StreamProducer> streamProducers = null;
//        streamProducers = addToGraph(graph, inputs.getSecond());
//        deliverStreamDataOutputs(graph, streamProducers);

        if (getExecutionUnit().getEnclosingOperator() instanceof StreamingNest) {
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


}

