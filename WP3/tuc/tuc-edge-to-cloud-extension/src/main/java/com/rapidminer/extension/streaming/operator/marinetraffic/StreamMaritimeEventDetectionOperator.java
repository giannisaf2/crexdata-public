/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.operator.marinetraffic;

import java.util.Collections;
import java.util.List;

import com.rapidminer.extension.streaming.ioobject.StreamDataContainer;
import com.rapidminer.extension.streaming.utility.api.crexdata.maritime.EventDetectionKafkaConfiguration;
import com.rapidminer.extension.streaming.utility.api.crexdata.maritime.JobType;
import com.rapidminer.extension.streaming.utility.api.crexdata.maritime.KafkaConfiguration;
import com.rapidminer.extension.streaming.utility.api.crexdata.maritime.TopicConfiguration;
import com.rapidminer.extension.streaming.utility.graph.StreamGraph;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.container.Pair;


/**
 * Operator for interfacing with Marine Traffic's Akka cluster
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class StreamMaritimeEventDetectionOperator extends AbstractMaritimeOperator {

	protected final InputPort input = getInputPorts().createPort("input stream", StreamDataContainer.class);

	public static final String PARAMETER_INPUT_TOPIC = "input_topic";

	public StreamMaritimeEventDetectionOperator(OperatorDescription description) {
		super(description);
		getTransformer().addPassThroughRule(input, output);
	}

	@Override
	public Pair<StreamGraph, List<StreamDataContainer>> getStreamDataInputs() throws UserError {
		StreamDataContainer inData = input.getData(StreamDataContainer.class);
		return new Pair<>(inData.getStreamGraph(), Collections.singletonList(inData));
	}

	@Override
	protected List<ParameterType> createInputTopicParameterTypes() {
		return Collections.singletonList(new ParameterTypeString(PARAMETER_INPUT_TOPIC, "Input topic", false));
	}

	@Override
	protected List<String> getTopicNames(int desiredSize) throws UndefinedParameterError {
		return Collections.singletonList(getParameterAsString(PARAMETER_INPUT_TOPIC));
	}

	@Override
	protected JobType getJobType() {
		return JobType.EVENTS;
	}

	@Override
	protected KafkaConfiguration getKafkaConfiguration(String brokers, String outTopic, int numberOfInputs) throws UndefinedParameterError {
		return new EventDetectionKafkaConfiguration(new TopicConfiguration(brokers,getParameterAsString(PARAMETER_INPUT_TOPIC)),
			new TopicConfiguration(brokers,outTopic));
	}


}