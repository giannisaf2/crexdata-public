/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.rapidminer.connection.ConnectionHandler
 *  com.rapidminer.connection.ConnectionInformation
 *  com.rapidminer.connection.ConnectionInformationBuilder
 *  com.rapidminer.connection.configuration.ConfigurationParameter
 *  com.rapidminer.connection.configuration.ConnectionConfiguration
 *  com.rapidminer.connection.configuration.ConnectionConfigurationBuilder
 *  com.rapidminer.connection.util.ParameterUtility
 *  com.rapidminer.connection.util.TestExecutionContext
 *  com.rapidminer.connection.util.TestResult
 *  com.rapidminer.connection.util.ValidationResult
 *  org.apache.commons.lang3.StringUtils
 */
package com.rapidminer.extension.admin.connection;

import com.rapidminer.connection.ConnectionHandler;
import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.ConnectionInformationBuilder;
import com.rapidminer.connection.configuration.ConfigurationParameter;
import com.rapidminer.connection.configuration.ConnectionConfiguration;
import com.rapidminer.connection.configuration.ConnectionConfigurationBuilder;
import com.rapidminer.connection.util.ParameterUtility;
import com.rapidminer.connection.util.TestExecutionContext;
import com.rapidminer.connection.util.TestResult;
import com.rapidminer.connection.util.ValidationResult;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;

public class AIHubConnectionHandler
implements ConnectionHandler {
    public static final String GROUP_KEY = "aihub";
    public static final String PARAMETER_SERVER_URL = "server_url";
    public static final String PARAMETER_CLIENT_SECRET = "client_secret";
    public static final String PARAMETER_REFRESH_TOKEN = "refresh_token";
    public static final String PARAMETER_AUTH_METHOD = "authentication";
    public static final String VALUE_AUTH_METHOD_TOKEN = "token";
    public static final String VALUE_AUTH_BASIC_AUTH = "basic_auth";
    public static final String PARAMETER_USER_NAME = "user_name";
    public static final String PARAMETER_PASSWORD = "password";
    public static final String PARAMETER_SERVER_VERSION = "server_version";
    public static final String VALUE_SERVER_VERSION_TEN = "above_10.0";
    public static final String VALUE_SERVER_VERSION_NINE = "below_10.0";
    public static final String PARAMETER_CLIENT_ID = "client_id";
    public static final String VALUE_DEFAULT_CLIENT_ID = "aihub-studio";
    public static final String PARAMETER_REALM_ID = "realm_id";
    public static final String VALUE_DEFAULT_REALM_ID = "master";
    public static final String PARAMETER_GRANT_TYPE = "grant_type";
    public static final String VALUE_GRANT_TYPE_PASSWORD = "password";
    public static final String VALUE_GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials";
    public static final String VALUE_GRANT_TYPE_CLIENT_REFRESH_TOKEN = "refresh_token";
    private static final AIHubConnectionHandler INSTANCE = new AIHubConnectionHandler();

    public static AIHubConnectionHandler getInstance() {
        return INSTANCE;
    }

    public ConnectionInformation createNewConnectionInformation(String name) {
        LinkedList<ConfigurationParameter> hubConfig = new LinkedList<ConfigurationParameter>();
        hubConfig.add((ConfigurationParameter)ParameterUtility.getCPBuilder((String)PARAMETER_SERVER_URL).build());
        hubConfig.add((ConfigurationParameter)ParameterUtility.getCPBuilder((String)PARAMETER_SERVER_VERSION, (boolean)false).withValue(VALUE_SERVER_VERSION_TEN).build());
        hubConfig.add((ConfigurationParameter)ParameterUtility.getCPBuilder((String)PARAMETER_AUTH_METHOD, (boolean)false).withValue(VALUE_AUTH_METHOD_TOKEN).build());
        hubConfig.add((ConfigurationParameter)ParameterUtility.getCPBuilder((String)PARAMETER_USER_NAME).build());
        hubConfig.add((ConfigurationParameter)ParameterUtility.getCPBuilder((String)"password", (boolean)true).build());
        hubConfig.add((ConfigurationParameter)ParameterUtility.getCPBuilder((String)PARAMETER_GRANT_TYPE, (boolean)false).withValue("password").build());
        hubConfig.add((ConfigurationParameter)ParameterUtility.getCPBuilder((String)PARAMETER_REALM_ID, (boolean)false).withValue(VALUE_DEFAULT_REALM_ID).build());
        hubConfig.add((ConfigurationParameter)ParameterUtility.getCPBuilder((String)PARAMETER_CLIENT_ID, (boolean)false).withValue(VALUE_DEFAULT_CLIENT_ID).build());
        hubConfig.add((ConfigurationParameter)ParameterUtility.getCPBuilder((String)PARAMETER_CLIENT_SECRET, (boolean)true).build());
        hubConfig.add((ConfigurationParameter)ParameterUtility.getCPBuilder((String)"refresh_token", (boolean)true).build());
        ConnectionConfiguration config = new ConnectionConfigurationBuilder(name, this.getType()).withDescription("This is an AI Hub connection").withKeys(GROUP_KEY, hubConfig).build();
        ConnectionInformationBuilder infBuilder = new ConnectionInformationBuilder(config);
        return infBuilder.build();
    }

    public void initialize() {
    }

    public boolean isInitialized() {
        return false;
    }

    public String getType() {
        return "admin:aihub";
    }

    public ValidationResult validate(ConnectionInformation object) {
        String serverVersion = "aihub.server_version";
        ConfigurationParameter hubVersion = object.getConfiguration().getParameter(serverVersion);
        boolean isVersionSet = ParameterUtility.isValueSet((ConfigurationParameter)hubVersion);
        if (!isVersionSet) {
            return ValidationResult.failure((String)"validation.failed", Collections.singletonMap(serverVersion, "server_version cannot be empty, have you migrated to Admin Tools v2 already?"), (Object[])new Object[0]);
        }
        String serverURL = "aihub.server_url";
        ConfigurationParameter hubURL = object.getConfiguration().getParameter(serverURL);
        boolean isURLSet = ParameterUtility.isValueSet((ConfigurationParameter)hubURL);
        if (!isURLSet) {
            return ValidationResult.failure((String)"validation.failed", Collections.singletonMap(serverURL, "server_url cannot be empty"), (Object[])new Object[0]);
        }
        if (StringUtils.endsWith((CharSequence)serverURL, (CharSequence)"/")) {
            return ValidationResult.failure((String)"validation.failed", Collections.singletonMap(serverURL, "server_url cannot end with /"), (Object[])new Object[0]);
        }
        return ValidationResult.success((String)"validation.success");
    }

    public TestResult test(TestExecutionContext<ConnectionInformation> testContext) {
        ConnectionInformation info = (ConnectionInformation)testContext.getSubject();
        ConnectionConfiguration config = info.getConfiguration();
        AIHubConnectionManager manager = new AIHubConnectionManager(info, null);
        String groupKey = "aihub.";
        String url = config.getParameter(groupKey + PARAMETER_SERVER_URL).getValue();
        if (StringUtils.isBlank((CharSequence)url)) {
            return TestResult.failure((String)"test.connection_failed", Collections.singletonMap(url, "server_url cannot be empty"), (Object[])new Object[0]);
        }
        if (url.endsWith("/")) {
            return TestResult.failure((String)"URL must not end with a trailing slash.", new HashMap(), (Object[])new Object[0]);
        }
        try {
            manager.getIdentityProviderToken();
        }
        catch (Exception e) {
            return TestResult.failure((String)"test.connection_failed", (Object[])new Object[]{e.getMessage()});
        }
        return TestResult.success((String)"test.success");
    }
}

