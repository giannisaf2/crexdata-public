/**
 * Authors: Dimitrios Banelas, Ourania Ntouni
 * <p>
 * Copyright (C) 2025-2026 Technical University of Crete
 */
package com.rapidminer.extension.streaming.optimizer.connection;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rapidminer.tools.LogService;
import org.springframework.messaging.simp.stomp.*;

import com.rapidminer.extension.streaming.optimizer.settings.OptimizerMessage;
import com.rapidminer.extension.streaming.optimizer.settings.OptimizerResponse;

public class OptimizerStompSessionHandler extends StompSessionHandlerAdapter {

    private static final Logger LOGGER = LogService.getRoot();
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private Map<String, BlockingQueue<String>> receivedMessages = new LinkedHashMap<>();

    public static final String BROADCAST_TOPIC = "/topic/broadcast";
    public static final String ECHO_TOPIC = "/user/queue/echo";
    public static final String INFO_TOPIC = "/user/queue/info";
    public static final String ERRORS_TOPIC = "/user/queue/errors";
    public static final String OPTIMIZATION_RESULT_TOPIC = "/user/queue/optimization_results";

    private StompSession session;
    private String requestID;
    private OptimizerResponse latestResponse = null;
    private boolean optimizationFinished = false;

    public OptimizerStompSessionHandler() {}

    @Override
    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
        this.session = session;

        List<String> topics = Arrays.asList(
                BROADCAST_TOPIC,
                ECHO_TOPIC,
                INFO_TOPIC,
                ERRORS_TOPIC,
                OPTIMIZATION_RESULT_TOPIC
        );

        topics.forEach(dest -> {
            receivedMessages.put(dest, new ArrayBlockingQueue<>(1000));
            session.subscribe(dest, this);
            LOGGER.info("Subscribed to: " + dest);
        });
    }

    @Override
    public Type getPayloadType(StompHeaders headers) {
        return String.class;
    }

    public static <T> T fromString(String jsonString, Class<T> valueType) throws IOException {
        try {
            return JSON_MAPPER.readValue(jsonString, valueType);
        } catch (JsonParseException | OutOfMemoryError |JsonMappingException e ) {
            LOGGER.severe("Error message" + e.getMessage());
            return null;
        }
    }

    @Override
    public synchronized void handleFrame(StompHeaders headers, Object payload) {
        String topic = headers.getDestination();
        String msg = (String) payload;

        LOGGER.info("Handling frame for topic " + topic);
        try {
            if (OPTIMIZATION_RESULT_TOPIC.equals(topic)) {

                OptimizerResponse response = fromString(msg, OptimizerResponse.class);

                if (response != null && requestID != null &&
                        requestID.equals(response.getOptimizationRequestId())) {

                    latestResponse = response;
                    receivedMessages.get(topic).offer(msg);
                    notifyAll();

                    LOGGER.info("Optimization result stored and notified");

                } else {
                    LOGGER.warning("Optimization result ignored due to unmatched requestID");
                }

            } else {
                LOGGER.info("Received message on topic " + topic + ": " + msg);

                if (INFO_TOPIC.equals(topic)) {
                    OptimizerMessage optimizerMessage = fromString(msg, OptimizerMessage.class);
                    if (optimizerMessage != null &&
                            OptimizerMessage.OPTIMIZATION_COMPLETED_ACTION.equals(optimizerMessage.getAction()) &&
                            requestID.equals(optimizerMessage.getId())) {
                        optimizationFinished = true;
                    }
                }

                receivedMessages.get(topic).offer(msg);
            }

        } catch (Exception e) {
            LOGGER.warning("Error handling frame: " + e.getMessage());
        }
    }

    public String pollMessage(String topic, int timeoutMillis) throws InterruptedException {
        return receivedMessages.containsKey(topic)
                ? receivedMessages.get(topic).poll(timeoutMillis, TimeUnit.MILLISECONDS)
                : null;
    }

    public OptimizerResponse getOptimizationResult() {
        return latestResponse;
    }

    public boolean isOptimizationFinished() {
        return optimizationFinished;
    }

    public synchronized OptimizerResponse waitForOptimizationResult(int timeoutMillis)
            throws InterruptedException {

        LOGGER.info("Waiting for optimization result...");

        if (latestResponse == null) {
            wait(timeoutMillis);
        }

        debugPrintAllMessages();
        return latestResponse;
    }

    private void debugPrintAllMessages() {
        receivedMessages.forEach((topic, queue) ->
                LOGGER.info(topic + " → " + queue.toString()));
    }

    public boolean newOptimizationResultsAvailable() {
        return latestResponse != null;
    }

    public void setRequestID(String requestID) {
        this.requestID = requestID;
        LOGGER.info("Set requestID = " + requestID);
    }
}