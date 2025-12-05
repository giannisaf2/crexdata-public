/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.utility.graph;

import com.rapidminer.extension.streaming.utility.graph.crexdata.synopsis.SynopsisDataProducer;
import com.rapidminer.extension.streaming.utility.graph.crexdata.synopsis.SynopsisEstimateQuery;
import com.rapidminer.extension.streaming.utility.graph.sink.KafkaSink;
import com.rapidminer.extension.streaming.utility.graph.source.KafkaSource;
import com.rapidminer.extension.streaming.utility.graph.transform.AggregateTransformer;
import com.rapidminer.extension.streaming.utility.graph.transform.ConnectTransformer;
import com.rapidminer.extension.streaming.utility.graph.transform.DuplicateStreamTransformer;
import com.rapidminer.extension.streaming.utility.graph.transform.FilterTransformer;
import com.rapidminer.extension.streaming.utility.graph.transform.JoinTransformer;
import com.rapidminer.extension.streaming.utility.graph.transform.MapTransformer;
import com.rapidminer.extension.streaming.utility.graph.transform.ParseFieldTransformer;
import com.rapidminer.extension.streaming.utility.graph.transform.RapidMinerModelApplierTransformer;
import com.rapidminer.extension.streaming.utility.graph.transform.SelectTransformer;
import com.rapidminer.extension.streaming.utility.graph.transform.StringifyFieldTransformer;
import com.rapidminer.extension.streaming.utility.graph.source.SpringFinancialSource;
import com.rapidminer.extension.streaming.utility.graph.transform.TimestampTransformer;
import com.rapidminer.extension.streaming.utility.graph.transform.UnionTransformer;


/**
 * Functionality defined for graph visitors (e.g.: translation managers)
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public interface StreamGraphNodeVisitor {

	/**
	 * Initializes visitor
	 */
	void initialize();

	/**
	 * Visit Kafka sink
	 *
	 * @param kafkaSink
	 */
	void visit(KafkaSink kafkaSink);

	/**
	 * Visit Kafka source
	 *
	 * @param kafkaSource
	 */
	void visit(KafkaSource kafkaSource);

	/**
	 * Visit Map transformer
	 *
	 * @param mapTransformer
	 */
	void visit(MapTransformer mapTransformer);

	/**
	 * Visit TIMESTAMP transformer
	 *
	 * @param timestampTransformer
	 */
	void visit(TimestampTransformer timestampTransformer);

	void visit(JsonDataProducer synopsisDataProducer);

	/**
	 * Visit Select transformer
	 *
	 * @param selectTransformer
	 */
	void visit(SelectTransformer selectTransformer);

	/**
	 * Visit Join transformer
	 *
	 * @param joinTransformer
	 */
	void visit(JoinTransformer joinTransformer);

	/**
	 * Visit Filter transformer
	 *
	 * @param filterTransformer
	 */
	void visit(FilterTransformer filterTransformer);

	/**
	 * Visit Aggregate transformer
	 *
	 * @param aggregateTransformer
	 */
	void visit(AggregateTransformer aggregateTransformer);

	/**
	 * Visit Duplicate transformer
	 *
	 * @param duplicateStreamTransformer
	 */
	void visit(DuplicateStreamTransformer duplicateStreamTransformer);

	/**
	 * Visit Connect transformer
	 *
	 * @param connectTransformer
	 */
	void visit(ConnectTransformer connectTransformer);

	/**
	 * Visit ParseField transformer
	 *
	 * @param parseFieldTransformer
	 */
	void visit(ParseFieldTransformer parseFieldTransformer);

	/**
	 * Visit StringifyFieldTransformer transformer
	 *
	 * @param stringifyFieldTransformer
	 */
	void visit(StringifyFieldTransformer stringifyFieldTransformer);

	/**
	 * Visit Synopsis estimate query
	 *
	 * @param synopsisEstimateQuery
	 */
	void visit(SynopsisEstimateQuery synopsisEstimateQuery);

	/**
	 * Visit Synopsis data producer
	 *
	 * @param synopsisDataProducer
	 */
	void visit(SynopsisDataProducer synopsisDataProducer);

	/**
	 * Visit Spring Financial Server source
	 *
	 * @param springFinancialSource
	 */
	void visit(SpringFinancialSource springFinancialSource);

	/**
	 * Visit Union transformer
	 *
	 * @param unionTransformer
	 */
	void visit(UnionTransformer unionTransformer);

	/**
	 * Visit RapidMiner Model Applier transformer
	 *
	 * @param rmModelApplierTransformer
	 */
	void visit(RapidMinerModelApplierTransformer rmModelApplierTransformer);

}