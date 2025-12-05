/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.JsonNode
 *  com.rapidminer.connection.ConnectionInformation
 *  com.rapidminer.connection.ConnectionInformationContainerIOObject
 *  com.rapidminer.connection.configuration.ConnectionConfiguration
 *  com.rapidminer.connection.valueprovider.handler.ValueProviderHandlerRegistry
 *  com.rapidminer.gui.tools.VersionNumber
 *  com.rapidminer.operator.Operator
 *  com.rapidminer.operator.OperatorException
 *  com.rapidminer.tools.LogService
 *  org.apache.commons.lang3.StringUtils
 *  org.eclipse.jgit.api.TransportConfigCallback
 *  org.eclipse.jgit.transport.TransportHttp
 */
package com.rapidminer.extension.admin.connection;

import com.fasterxml.jackson.databind.JsonNode;
import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.ConnectionInformationContainerIOObject;
import com.rapidminer.connection.configuration.ConnectionConfiguration;
import com.rapidminer.connection.valueprovider.handler.ValueProviderHandlerRegistry;
import com.rapidminer.extension.admin.AssertUtility;
import com.rapidminer.extension.admin.operator.aihubapi.exceptions.AdminToolsException;
import com.rapidminer.extension.admin.operator.aihubapi.exceptions.AdminToolsRestEmptyBodyException;
import com.rapidminer.extension.admin.operator.aihubapi.exceptions.AdminToolsRestException;
import com.rapidminer.extension.admin.operator.aihubapi.exceptions.AdminToolsRestParsingException;
import com.rapidminer.extension.admin.rest.RequestMethod;
import com.rapidminer.extension.admin.rest.RequestPath;
import com.rapidminer.extension.admin.rest.RestUtility;
import com.rapidminer.extension.admin.rest.responses.aihub.AuthInfoResponse;
import com.rapidminer.extension.admin.rest.responses.aihub.InstanceUserOrigin;
import com.rapidminer.extension.admin.rest.responses.aihub.InstanceV10Response;
import com.rapidminer.extension.admin.rest.responses.aihub.InstanceV9Response;
import com.rapidminer.gui.tools.VersionNumber;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.LogService;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.transport.TransportHttp;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class AIHubConnectionManager {
    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER = "Bearer";
    private static final String BEARER_FORMATTER = "%s %s";
    private static final String RESPONSE_PAYLOAD_ACCESS_TOKEN = "access_token";
    private static final String MEDIA_TYPE_FORM_ENCODED = "application/x-www-form-urlencoded";
    private static final Logger LOGGER = LogService.getRoot();
    private static final String LOG_INVOKE_REST_REQUEST = "Invoking REST request to '%s' and method '%s'";
    private static final String LOG_INVOKE_REST_RESPONSE_STATUS = "Response code '%s' retrieved from REST request to '%s' and method '%s'";
    private static final VersionNumber ZIP_ENDPOINT_AVAILABLE_VERSION = new VersionNumber(10, 4, 0);
    private ConnectionInformationContainerIOObject conObject;
    private String aiHubUrl;
    private String refreshToken;
    private String grantType;
    private String clientSecret;
    private String username;
    private String password;
    private String serverVersion;
    private String clientId;
    private String realmId;

    public AIHubConnectionManager(ConnectionInformation information, Operator operator) {
        this.initManager(information, operator);
    }

    public AIHubConnectionManager(ConnectionInformationContainerIOObject conObject, Operator operator) {
        this.conObject = conObject;
        this.initManager(conObject.getConnectionInformation(), operator);
    }

    private static Map<String, String> getInjectedValues(ConnectionInformation connection, Operator operator, boolean onlyInjected) {
        if (operator != null) {
            LOGGER.fine(() -> "Injecting values for operator '" + operator.getName() + "'");
        } else {
            LOGGER.fine(() -> "Injecting values without an operator");
        }
        return ValueProviderHandlerRegistry.getInstance().injectValues(connection, operator, onlyInjected);
    }

    private void initManager(ConnectionInformation information, Operator operator) {
        String groupKey = "aihub.";
        Map<String, String> valueMap = AIHubConnectionManager.getInjectedValues(information, operator, false);
        this.clientSecret = valueMap.get(groupKey + "client_secret");
        this.refreshToken = valueMap.get(groupKey + "refresh_token");
        this.aiHubUrl = valueMap.get(groupKey + "server_url");
        this.serverVersion = valueMap.get(groupKey + "server_version");
        this.username = valueMap.get(groupKey + "user_name");
        this.password = valueMap.get(groupKey + "password");
        this.grantType = valueMap.get(groupKey + "grant_type");
        if (this.serverVersion == null) {
            this.serverVersion = "below_10.0";
        }
        this.clientId = valueMap.get(groupKey + "client_id");
        if (this.clientId == null) {
            this.serverVersion = "aihub-studio";
        }
        this.realmId = valueMap.get(groupKey + "realm_id");
        if (this.realmId == null) {
            this.realmId = "master";
        }
    }

    public String getIdentityProviderToken() throws OperatorException {
        try {
            if ("above_10.0".equals(this.serverVersion)) {
                return this.getBearerTokenV10();
            }
            return this.getBearerTokenV9();
        }
        catch (AdminToolsException e) {
            throw new OperatorException(e.getMessage(), (Throwable)e);
        }
    }

    public String getAuthToken(String identityProviderToken) throws AdminToolsException {
        AssertUtility.hasText(identityProviderToken, "'identityProviderToken' cannot be blank");
        if ("above_10.0".equals(this.serverVersion)) {
            return identityProviderToken;
        }
        if (StringUtils.isBlank((CharSequence)identityProviderToken)) {
            throw new AdminToolsException("Identity Provider token was blank, cannot proceed");
        }
        OkHttpClient client = new OkHttpClient().newBuilder().build();
        Request request = new Request.Builder().url(String.format("%s/api/rest/saml/tokenservice", this.getAIHubUrl())).method(RequestMethod.GET.name(), null).addHeader(AUTHORIZATION, String.format(BEARER_FORMATTER, BEARER, identityProviderToken)).build();
        Response response = null;
        try {
            response = client.newCall(request).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            ResponseBody body = response.body();
            if (body == null) {
                throw new AdminToolsRestEmptyBodyException();
            }
            String string = body.string();
            if (response != null) {
                response.close();
            }
            return string;
        }
        catch (Throwable throwable) {
            try {
                if (response != null) {
                    try {
                        response.close();
                    }
                    catch (Throwable throwable2) {
                        throwable.addSuppressed(throwable2);
                    }
                }
                throw throwable;
            }
            catch (IOException e) {
                throw new AdminToolsException(e.getMessage(), e);
            }
        }
    }

    public Request.Builder addAuthentication(Request.Builder builder) throws AdminToolsException, OperatorException {
        AssertUtility.notNull(builder, "'builder' cannot be null");
        String groupKey = "aihub.";
        ConnectionConfiguration config = this.conObject.getConnectionInformation().getConfiguration();
        String authMethod = config.getParameter(groupKey + "authentication").getValue();
        if ("token".equals(authMethod)) {
            String bearerToken = this.getIdentityProviderToken();
            String jwtToken = this.getAuthToken(bearerToken);
            builder.addHeader(AUTHORIZATION, String.format(BEARER_FORMATTER, BEARER, jwtToken));
        } else if ("basic_auth".equals(authMethod)) {
            String user = config.getParameter(groupKey + "user_name").getValue();
            String pass = config.getParameter(groupKey + "password").getValue();
            String credential = Credentials.basic(user, pass);
            builder.addHeader(AUTHORIZATION, credential);
        }
        return builder;
    }

    public TransportConfigCallback getTransportConfigCallback(String jwtToken) {
        return transport -> {
            if (jwtToken != null && transport instanceof TransportHttp) {
                TransportHttp httpTransport = (TransportHttp)transport;
                HashMap<String, String> map = new HashMap<String, String>();
                map.put(AUTHORIZATION, String.format(BEARER_FORMATTER, BEARER, jwtToken));
                httpTransport.setAdditionalHeaders(map);
            }
        };
    }

    public String getGrantType() {
        return this.grantType;
    }

    public String getRefreshToken() {
        return this.refreshToken;
    }

    public String getClientId() {
        return this.clientId;
    }

    public String getClientSecret() {
        return this.clientSecret;
    }

    public String getRealmId() {
        return this.realmId;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public String getAIHubUrl() {
        return this.aiHubUrl;
    }

    public String getServerVersion() {
        return this.serverVersion;
    }

    public InstanceV10Response getV10InstanceInfo() throws AdminToolsException {
        OkHttpClient client = new OkHttpClient().newBuilder().build();
        Request request = new Request.Builder().url(String.format("%s%s", this.getAIHubUrl(), RestUtility.getEndpointPath(RequestPath.INSTANCE, this.getServerVersion()))).method(RequestMethod.GET.name(), null).build();
        LOGGER.fine(() -> String.format(LOG_INVOKE_REST_REQUEST, request.url(), request.method()));
        Response response = null;
        try {
            response = client.newCall(request).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new AdminToolsRestEmptyBodyException();
            }
            Response finalResponse = response;
            LOGGER.fine(() -> String.format(LOG_INVOKE_REST_RESPONSE_STATUS, finalResponse.code(), request.url(), request.method()));
            InstanceV10Response instanceV10Response = (InstanceV10Response)RestUtility.getMapper().readValue(responseBody.string(), InstanceV10Response.class);
            if (response != null) {
                response.close();
            }
            return instanceV10Response;
        }
        catch (Throwable throwable) {
            try {
                if (response != null) {
                    try {
                        response.close();
                    }
                    catch (Throwable throwable2) {
                        throwable.addSuppressed(throwable2);
                    }
                }
                throw throwable;
            }
            catch (IOException e) {
                throw new AdminToolsRestParsingException(e);
            }
        }
    }

    public InstanceV9Response getV9InstanceInfo() throws AdminToolsException {
        OkHttpClient client = new OkHttpClient().newBuilder().build();
        Request request = new Request.Builder().url(String.format("%s%s", this.getAIHubUrl(), RestUtility.getEndpointPath(RequestPath.INSTANCE, this.getServerVersion()))).method(RequestMethod.GET.name(), null).build();
        LOGGER.fine(() -> String.format(LOG_INVOKE_REST_REQUEST, request.url(), request.method()));
        Response response = null;
        try {
            response = client.newCall(request).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new AdminToolsRestEmptyBodyException();
            }
            Response finalResponse = response;
            LOGGER.fine(() -> String.format(LOG_INVOKE_REST_RESPONSE_STATUS, finalResponse.code(), request.url(), request.method()));
            InstanceV9Response instanceV9Response = (InstanceV9Response)RestUtility.getMapper().readValue(responseBody.string(), InstanceV9Response.class);
            if (response != null) {
                response.close();
            }
            return instanceV9Response;
        }
        catch (Throwable throwable) {
            try {
                if (response != null) {
                    try {
                        response.close();
                    }
                    catch (Throwable throwable2) {
                        throwable.addSuppressed(throwable2);
                    }
                }
                throw throwable;
            }
            catch (IOException e) {
                throw new AdminToolsRestParsingException(e);
            }
        }
    }

    public AuthInfoResponse getAuthInfo() throws AdminToolsException {
        OkHttpClient client = new OkHttpClient().newBuilder().build();
        Request request = new Request.Builder().url(String.format("%s%s", this.getAIHubUrl(), RestUtility.getEndpointPath(RequestPath.AUTH, this.getServerVersion()))).method(RequestMethod.GET.name(), null).build();
        LOGGER.fine(() -> String.format(LOG_INVOKE_REST_REQUEST, request.url(), request.method()));
        Response response = null;
        try {
            response = client.newCall(request).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new AdminToolsRestEmptyBodyException();
            }
            Response finalResponse = response;
            LOGGER.fine(() -> String.format(LOG_INVOKE_REST_RESPONSE_STATUS, finalResponse.code(), request.url(), request.method()));
            AuthInfoResponse authInfoResponse = (AuthInfoResponse)RestUtility.getMapper().readValue(responseBody.string(), AuthInfoResponse.class);
            if (response != null) {
                response.close();
            }
            return authInfoResponse;
        }
        catch (Throwable throwable) {
            try {
                if (response != null) {
                    try {
                        response.close();
                    }
                    catch (Throwable throwable2) {
                        throwable.addSuppressed(throwable2);
                    }
                }
                throw throwable;
            }
            catch (IOException e) {
                throw new AdminToolsRestParsingException(e);
            }
        }
    }

    private RequestBody generateIdentityProviderRequestBody() throws AdminToolsException {
        StringBuilder sb = new StringBuilder();
        switch (this.getGrantType()) {
            case "password": {
                if (StringUtils.isBlank((CharSequence)this.getClientId()) || StringUtils.isBlank((CharSequence)this.getUsername()) || StringUtils.isBlank((CharSequence)this.getPassword())) {
                    throw new AdminToolsException("Missing one of clientId, username or password for grant_type 'password', please ensure they are properly set");
                }
                sb.append("grant_type=password");
                sb.append("&client_id=");
                sb.append(this.getClientId());
                sb.append("&username=");
                sb.append(this.getUsername());
                sb.append("&password=");
                sb.append(this.getPassword());
                break;
            }
            case "refresh_token": {
                if (StringUtils.isBlank((CharSequence)this.getClientId()) || StringUtils.isBlank((CharSequence)this.getRefreshToken()) || StringUtils.isBlank((CharSequence)this.getClientSecret())) {
                    throw new AdminToolsException("Missing one of clientId, clientSecret or refreshToken for grant_type 'refresh_token', please ensure they are properly set");
                }
                sb.append("grant_type=refresh_token");
                sb.append("&client_id=");
                sb.append(this.getClientId());
                sb.append("&client_secret=");
                sb.append(this.getClientSecret());
                sb.append("&refresh_token=");
                sb.append(this.getRefreshToken());
                break;
            }
            case "client_credentials": {
                if (StringUtils.isBlank((CharSequence)this.getClientId()) || StringUtils.isBlank((CharSequence)this.getClientSecret())) {
                    throw new AdminToolsException("Missing one of clientId or clientSecret for grant_type 'client_credentials', please ensure they are properly set");
                }
                sb.append("grant_type=client_credentials");
                sb.append("&client_id=");
                sb.append(this.getClientId());
                sb.append("&client_secret=");
                sb.append(this.getClientSecret());
                break;
            }
        }
        return RequestBody.create(sb.toString(), MediaType.parse(MEDIA_TYPE_FORM_ENCODED));
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private String getBearerTokenV9() throws AdminToolsException {
        InstanceV9Response instance = this.getV9InstanceInfo();
        if (!InstanceUserOrigin.KC.equals((Object)instance.getUserOrigin())) {
            LOGGER.warning(() -> String.format("Received '%s' as user origin", new Object[]{instance.getUserOrigin()}));
            throw new AdminToolsException("Only Keycloak is supported in admin extension");
        }
        OkHttpClient client = new OkHttpClient().newBuilder().build();
        if (StringUtils.isBlank((CharSequence)this.clientSecret) || StringUtils.isBlank((CharSequence)this.refreshToken) || !"refresh_token".equals(this.grantType)) {
            throw new AdminToolsException(String.format("AI Hub Server '%s' requires to use the grant type 'refresh_token' to utilize offline tokens.", this.getServerVersion()));
        }
        Request request = new Request.Builder().url(String.format("%s/auth/realms/%s/protocol/openid-connect/token", this.getAIHubUrl(), this.getRealmId())).method(RequestMethod.POST.name(), this.generateIdentityProviderRequestBody()).addHeader("Content-Type", MEDIA_TYPE_FORM_ENCODED).build();
        try (Response response = client.newCall(request).execute();){
            ResponseBody responseBody = response.body();
            if (!response.isSuccessful() || responseBody == null) {
                throw new AdminToolsRestException(String.format("Cannot connect to AI Hub. Error Code: %s. Message: %s. Details: %s", response.code(), response.message(), responseBody != null ? responseBody.string() : "Null given as response"));
            }
            JsonNode responseNode = (JsonNode)RestUtility.getMapper().readValue(responseBody.string(), JsonNode.class);
            if (!responseNode.has(RESPONSE_PAYLOAD_ACCESS_TOKEN)) throw new AdminToolsRestParsingException("Cannot find access_token property");
            String string = responseNode.get(RESPONSE_PAYLOAD_ACCESS_TOKEN).asText();
            return string;
        }
        catch (IOException e) {
            throw new AdminToolsException(e.getMessage(), e);
        }
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private String getBearerTokenV10() throws AdminToolsException {
        InstanceV10Response instance = this.getV10InstanceInfo();
        if (instance == null || instance.getUserOrigin() == null || !InstanceUserOrigin.KC.equals((Object)instance.getUserOrigin())) {
            LOGGER.warning(() -> String.format("Received '%s' as user origin", new Object[]{instance.getUserOrigin()}));
            throw new AdminToolsException("Only Keycloak is supported in admin extension");
        }
        AuthInfoResponse authInfo = this.getAuthInfo();
        if (authInfo.getStudio() == null || StringUtils.isBlank((CharSequence)authInfo.getStudio().getAuthUrl())) {
            throw new AdminToolsException(String.format("Cannot derive necessary auth information from AI Hub '%s'. In AI Hub Server, please set 'authUrl' for the 'aihub-studio' client which is returned by /auth/info endpoint", this.getServerVersion()));
        }
        OkHttpClient client = new OkHttpClient().newBuilder().build();
        Request request = new Request.Builder().url(String.format("%s/realms/%s/protocol/openid-connect/token", authInfo.getStudio().getAuthUrl(), this.getRealmId())).method(RequestMethod.POST.name(), this.generateIdentityProviderRequestBody()).addHeader("Content-Type", MEDIA_TYPE_FORM_ENCODED).build();
        try (Response response = client.newCall(request).execute();){
            ResponseBody responseBody = response.body();
            if (!response.isSuccessful() || responseBody == null) {
                throw new AdminToolsRestException(String.format("Cannot connect to Identity Provider. Error Code: %s. Message: %s. Details: %s", response.code(), response.message(), responseBody != null ? responseBody.string() : "Null given as response"));
            }
            JsonNode responseNode = (JsonNode)RestUtility.getMapper().readValue(responseBody.string(), JsonNode.class);
            if (!responseNode.has(RESPONSE_PAYLOAD_ACCESS_TOKEN)) throw new AdminToolsRestParsingException("Cannot find access_token property");
            String string = responseNode.get(RESPONSE_PAYLOAD_ACCESS_TOKEN).asText();
            return string;
        }
        catch (IOException e) {
            throw new AdminToolsException(String.format("Retrieving tokens from v10 AI Hub instances is not yet supported. Reason: %s", e.getMessage()), e);
        }
    }

    public VersionNumber getAiHubVersion() throws AdminToolsException {
        if ("above_10.0".equals(this.getServerVersion())) {
            InstanceV10Response res = this.getV10InstanceInfo();
            return new VersionNumber(res.getServerVersion());
        }
        InstanceV9Response res = this.getV9InstanceInfo();
        return new VersionNumber(res.getServerVersion());
    }

    public String getAiHubProjectArchivalPathSegment() throws AdminToolsException {
        VersionNumber aiHubVersion = this.getAiHubVersion();
        return aiHubVersion.isAtLeast(ZIP_ENDPOINT_AVAILABLE_VERSION) ? "zip" : "archive";
    }
}

