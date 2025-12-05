/**
 * Authors: Ourania Ntouni
 * <p>
 * Copyright (C) 2025-2026 Technical University of Crete
 */
package com.rapidminer.extension.streaming.operator.tuc;


import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Ontology;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.fatigueModel.KNNPrediction;

// MAIN DEPENDENCIES TO ADD:
// Inside Altair app /lib/ dir
// you need to add the following jars:
// jakarta.activation-api-2.1.4.jar
// jaxb-core-4.0.5.jar
// jaxb-runtime-4.0.5.jar
// jakarta.xml.bind-api-4.0.4.jar

public class KNNMentalFatigue extends Operator {


    private InputPort exampleSetInput = getInputPorts().createPort("example set");
    private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

    private String PARAMETER_FEATURES_LIST = "feature names list";
    protected static final Logger LOGGER = LogService.getRoot();

    private String[] featureNames = {
            "mean pupil diameter",
            "fixation count",
            "saccade count",
            "blink count",
            "mean fixation duration",
            "mean saccade duration",
            "peak saccade velocity",
            "mean saccade velocity",
            "mean saccade amplitude",
    };

    public KNNMentalFatigue(OperatorDescription description) {
        super(description);
    }

    @Override
    public void doWork() throws OperatorException {
        ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);
        Attributes attributes = exampleSet.getAttributes();
        List<double[]> records = new ArrayList<>();

        for (Example example : exampleSet) {
            double[] inputValues = new double[featureNames.length];
            int i = 0;
            for (String featureName : featureNames) {
                Attribute attribute = attributes.get(featureName);
                if (attribute == null) {
                    throw new OperatorException("Attribute " + featureName + " not found in the ExampleSet.");
                }
                String value = example.getValueAsString(attributes.get(featureName));
                double parsedValue = Double.parseDouble(value);
                inputValues[i] = parsedValue;
                i++;
            }
            records.add(inputValues);
        }

        KNNPrediction predictor;
        predictor = new KNNPrediction("/Users/raniaduni/CREXDATA/rm-crexdata-extensions/rapidminer-streaming-extension/src/main/resources/auxFiles/pipeline.pmml");
            int[] predictions = predictor.predict(records);
            String newName = "predicted label";
            Attribute targetAttribute = AttributeFactory.createAttribute(newName, Ontology.INTEGER);
            targetAttribute.setTableIndex(attributes.size());
            exampleSet.getExampleTable().addAttribute(targetAttribute);
            attributes.addRegular(targetAttribute);
            int counter = 0;
            for(Example example:exampleSet){
                example.setValue(targetAttribute, predictions[counter]);
                counter++;
            }
            exampleSetOutput.deliver(exampleSet);

    }

}
