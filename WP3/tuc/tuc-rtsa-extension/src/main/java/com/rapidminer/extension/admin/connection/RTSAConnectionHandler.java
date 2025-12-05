/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.rapidminer.connection.ConnectionHandler
 *  com.rapidminer.connection.ConnectionInformation
 *  com.rapidminer.connection.ConnectionInformationBuilder
 *  com.rapidminer.connection.ConnectionInformationContainerIOObject
 *  com.rapidminer.connection.configuration.ConfigurationParameter
 *  com.rapidminer.connection.configuration.ConnectionConfiguration
 *  com.rapidminer.connection.configuration.ConnectionConfigurationBuilder
 *  com.rapidminer.connection.util.ParameterUtility
 *  com.rapidminer.connection.util.TestExecutionContext
 *  com.rapidminer.connection.util.TestResult
 *  com.rapidminer.connection.util.ValidationResult
 *  com.rapidminer.operator.OperatorException
 *  org.apache.commons.lang3.StringUtils
 */
package com.rapidminer.extension.admin.connection;

import com.rapidminer.connection.ConnectionHandler;
import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.ConnectionInformationBuilder;
import com.rapidminer.connection.ConnectionInformationContainerIOObject;
import com.rapidminer.connection.configuration.ConfigurationParameter;
import com.rapidminer.connection.configuration.ConnectionConfiguration;
import com.rapidminer.connection.configuration.ConnectionConfigurationBuilder;
import com.rapidminer.connection.util.ParameterUtility;
import com.rapidminer.connection.util.TestExecutionContext;
import com.rapidminer.connection.util.TestResult;
import com.rapidminer.connection.util.ValidationResult;
import com.rapidminer.extension.admin.operator.rtsa.RTSAApiManager;
import com.rapidminer.operator.OperatorException;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class RTSAConnectionHandler
implements ConnectionHandler {
    public static final String GROUP_KEY = "rtsa";
    public static final String PARAMETER_RTSA_URL = "server_url";
    public static final String PARAMETER_AUTH_METHOD = "authentication";
    public static final String VALUE_AUTH_METHOD_NONE = "no_authentication";
    public static final String VALUE_AUTH_BASIC_AUTH = "basic_auth";
    public static final String PARAMETER_USER_NAME = "user_name";
    public static final String PARAMETER_PASSWORD = "password";
    public static final String VALUE_AUTH_BEARER_AUTH = "bearer_auth";
    public static final String PARAMETER_BEARER_TOKEN = "bearer_token";
    public static final String PARAMETER_RTSA_VERSION = "server_version";
    public static final String VALUE_RTSA_VERSION_TEN = "above_10.0";
    public static final String VALUE_RTSA_VERSION_NINE = "below_10.0";
    private static final RTSAConnectionHandler INSTANCE = new RTSAConnectionHandler();

    public static RTSAConnectionHandler getInstance() {
        return INSTANCE;
    }

    public ConnectionInformation createNewConnectionInformation(String name) {
        ArrayList<ConfigurationParameter> clusterConfig = new ArrayList<ConfigurationParameter>();
        clusterConfig.add((ConfigurationParameter)ParameterUtility.getCPBuilder((String)PARAMETER_RTSA_URL).build());
        clusterConfig.add((ConfigurationParameter)ParameterUtility.getCPBuilder((String)PARAMETER_RTSA_VERSION, (boolean)false).withValue(VALUE_RTSA_VERSION_TEN).build());
        clusterConfig.add((ConfigurationParameter)ParameterUtility.getCPBuilder((String)PARAMETER_AUTH_METHOD, (boolean)false).withValue(VALUE_AUTH_METHOD_NONE).build());
        clusterConfig.add((ConfigurationParameter)ParameterUtility.getCPBuilder((String)PARAMETER_USER_NAME).build());
        clusterConfig.add((ConfigurationParameter)ParameterUtility.getCPBuilder((String)PARAMETER_PASSWORD, (boolean)true).build());
        clusterConfig.add((ConfigurationParameter)ParameterUtility.getCPBuilder((String)PARAMETER_BEARER_TOKEN, (boolean)true).build());
        ConnectionConfiguration config = new ConnectionConfigurationBuilder(name, this.getType()).withDescription("This is an RTSA connection").withKeys(GROUP_KEY, clusterConfig).build();
        ConnectionInformationBuilder infBuilder = new ConnectionInformationBuilder(config);
        return infBuilder.build();
    }

    public void initialize() {
    }

    public boolean isInitialized() {
        return false;
    }

    public String getType() {
        return "admin:rtsa";
    }

    public ValidationResult validate(ConnectionInformation object) {
        String rtsVersion = "rtsa.server_version";
        ConfigurationParameter rtsConfigVersion = object.getConfiguration().getParameter(rtsVersion);
        boolean isVersionSet = ParameterUtility.isValueSet((ConfigurationParameter)rtsConfigVersion);
        if (!isVersionSet) {
            return ValidationResult.failure((String)"validation.failed", Collections.singletonMap(rtsVersion, "server_version cannot be empty, have you migrated to Admin Tools v3 already?"), (Object[])new Object[0]);
        }
        String rtsURL = "rtsa.server_url";
        ConfigurationParameter rtsConfigUrl = object.getConfiguration().getParameter(rtsURL);
        boolean isURLSet = ParameterUtility.isValueSet((ConfigurationParameter)rtsConfigUrl);
        if (!isURLSet) {
            return ValidationResult.failure((String)"validation.failed", Collections.singletonMap(rtsURL, "server_url cannot be empty"), (Object[])new Object[0]);
        }
        if (StringUtils.endsWith((CharSequence)rtsURL, (CharSequence)"/")) {
            return ValidationResult.failure((String)"validation.failed", Collections.singletonMap(rtsURL, "server_url cannot end with /"), (Object[])new Object[0]);
        }
        return ValidationResult.success((String)"validation.success");
    }

    public TestResult test(TestExecutionContext<ConnectionInformation> testContext) {
        String groupKey;
        ConnectionInformationContainerIOObject object = new ConnectionInformationContainerIOObject((ConnectionInformation)testContext.getSubject());
        ConnectionConfiguration config = object.getConnectionInformation().getConfiguration();
        String url = config.getParameter((groupKey = "rtsa.") + PARAMETER_RTSA_URL).getValue();
        if (StringUtils.endsWith((CharSequence)url, (CharSequence)"/")) {
            return TestResult.failure((String)"URL cannot end with /.", new HashMap(), (Object[])new Object[0]);
        }
        try {
            RTSAApiManager.getDeployments(object, null);
        }
        catch (OperatorException e) {
            return TestResult.failure((String)("Cannot connect to RTSA: " + e.getMessage()), new HashMap(), (Object[])new Object[0]);
        }
        return TestResult.success((String)"test.success");
    }
}

