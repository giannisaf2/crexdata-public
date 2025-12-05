/**
 * Authors: Ourania Ntouni
 * <p>
 * Copyright (C) 2025-2026 Technical University of Crete
 */
package com.rapidminer.extension.admin.operator.rtsa;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.Column.TypeId;
import com.rapidminer.belt.table.MixedRowWriter;
import com.rapidminer.belt.table.Writers;
import com.rapidminer.connection.ConnectionInformationContainerIOObject;
import com.rapidminer.connection.configuration.ConfigurationParameter;
import com.rapidminer.extension.admin.AssertUtility;
import com.rapidminer.extension.admin.connection.AIHubConnectionManager;
import com.rapidminer.extension.admin.operator.aihubapi.exceptions.AdminToolsException;
import com.rapidminer.extension.admin.operator.aihubapi.requests.CreateDeploymentProcessLocationRequest;
import com.rapidminer.extension.admin.operator.aihubapi.requests.CreateDeploymentRequest;
import com.rapidminer.extension.admin.rest.LargeDownloadCallback;
import com.rapidminer.extension.admin.rest.RequestMethod;
import com.rapidminer.extension.admin.rest.RequestPath;
import com.rapidminer.extension.admin.rest.RestUtility;
import com.rapidminer.extension.admin.rest.responses.aihub.ProjectContentItemType;
import com.rapidminer.extension.admin.rest.responses.aihub.ProjectContentResponse;
import com.rapidminer.gui.tools.VersionNumber;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeEnumeration;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.LogService;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

public class DeployOnRTSAOperator extends AbstractRTSAOperator {
    public static final String PARAMETER_DEPLOYMENT_NAME = "deployment_name";
    public static final String PARAMETER_REPOSITORY_LOCATION = "deployment_location";
    public static final String PARAMETER_REPOSITORY_NAME = "project_name";
    public static final String PARAMETER_REPOSITORY_REF = "git_reference";
    public static final String PARAMETER_TIMEOUT = "time_out";
    public static final String PARAMETER_CONTINUOUS = "continuous_execution";
    public static final String PARAMETER_SLEEP_TIME = "sleep_time";
    public static final String PARAMETER_CONNECTIONS = "connections";
    public static final String PARAMETER_CONNECTION_NAME = "connection_name";
    private static final String LOG_WAIT_REST_REQUEST = "Waiting until REST request to '%s' and method '%s' finished...";
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String BEARER_TEMPLATE = "Bearer %s";
    private static final String REPOSITORIES_ENDPOINT_PLACEHOLDER = "REPOSITORIES_PLACEHOLDER";
    private static final Logger LOGGER = LogService.getRoot();
    private static final VersionNumber VERSION_BELOW_10_2_0 = new VersionNumber(10, 1, 99);
    static List<String> columnLabels = Arrays.asList("DeploymentName", "DeploymentLocation", "ProjectName", "GitReference", "TimeOut", "ContinuousExecution", "SleepTime", "RTSAResponseCode", "RTSAResponseMessage");
    static List<Column.TypeId> columnTypes;
    public InputPort aiHubConnection = this.getInputPorts().createPort("aihub_connection", ConnectionInformationContainerIOObject.class);

    public DeployOnRTSAOperator(OperatorDescription description) {
        super(description);
        this.getTransformer().addPassThroughRule(this.conInput, this.conOutput);
    }

    public void doWork() throws OperatorException {
        ConnectionInformationContainerIOObject conObject = this.conInput.getData(ConnectionInformationContainerIOObject.class);
        ConnectionInformationContainerIOObject aihubConObject = this.aiHubConnection.getData(ConnectionInformationContainerIOObject.class);

        this.conOutput.deliver(conObject);
        LOGGER.info("rtsa HUB VERSION" + conObject.getConnectionInformation().toString());

        LOGGER.info("AI HUB VERSION" + aihubConObject.getConnectionInformation().getConfiguration().getParameter("aihub.server_version").getValue());
        File p = FileSystemService.getPluginRapidMinerDir("rmx_admin");
        File downloadedDeploymentZip = new File(p, "deployment.zip");
        List<String> connectionNames = new ArrayList();
        String additionalEntries = this.getParameterAsString("connections");
        String endpoint;
        String rtsVersion;
        if (additionalEntries != null) {
            Iterator var6 = ParameterTypeEnumeration.transformString2List(additionalEntries).iterator();

            while(var6.hasNext()) {
                endpoint = (String)var6.next();
                rtsVersion = StringUtils.replace(endpoint, ".conninfo", "");
                String connection = String.format("%s.conninfo", rtsVersion);
                connectionNames.add(connection);
            }
        }

        VersionNumber versionNumber;
        try {
            versionNumber = (new AIHubConnectionManager(aihubConObject, this)).getAiHubVersion();
            endpoint = this.getDeploymentEndpoint(versionNumber, connectionNames);
        } catch (AdminToolsException var26) {
            throw new OperatorException(String.format("Cannot determine version of source AI Hub or target endpoint. Reason: %s", var26.getMessage()), var26);
        }

        rtsVersion = "rtsa.server_version";
        ConfigurationParameter rtsConfigVersion = conObject.getConnectionInformation().getConfiguration().getParameter(rtsVersion);
        LOGGER.info(() -> {
            return String.format("RTSA version: %s", rtsConfigVersion.getValue());
        });
        int rtsMajorVersion = 9;
        if ("above_10.0".equals(rtsConfigVersion.getValue())) {
            rtsMajorVersion = 10;
        }

        if (rtsMajorVersion < versionNumber.getMajorNumber()) {
            throw new OperatorException("Incompatible versions found. Deployments generated with AI Hub v10 are not supported by RTSA v9.");
        } else {
            try {
                this.getDeploymentZip(downloadedDeploymentZip.toPath(), versionNumber, endpoint, connectionNames);
                String deploymentName = this.getParameterAsString("deployment_name");
                LOGGER.info("The deployment name is " + deploymentName);
                if (RTSAApiManager.deploymentExists(conObject, deploymentName, this)) {
                    try {
                        RTSAApiManager.deleteDeployment(conObject, deploymentName, this);
                    } catch (AdminToolsException var25) {
                        throw new OperatorException(var25.getMessage(), var25);
                    }
                }

                Response rtsResponse = RTSAApiManager.deployOnRTSA(conObject, downloadedDeploymentZip.getAbsolutePath(), this.getParameterAsString("deployment_name"), (long)this.getParameterAsInt("time_out"), this);
                if (rtsResponse.body() == null) {
                    throw new IOException("Deployment failed, did not receive any response body");
                }

                String rtsResponseAsString = rtsResponse.body().string();
                int code = rtsResponse.code();
                MixedRowWriter writer = Writers.mixedRowWriter(columnLabels, columnTypes, 1, false);
                writer.move();
                writer.set(0, this.getParameterAsString("deployment_name"));
                writer.set(1, this.getSanitizedRepositoryLocation());
                writer.set(2, this.getParameterAsString("project_name"));
                writer.set(3, this.getParameterAsString("git_reference"));
                writer.set(4, (double)this.getParameterAsInt("time_out"));
                writer.set(5, this.getParameterAsString("continuous_execution"));
                writer.set(6, (double)this.getParameterAsInt("sleep_time"));
                writer.set(7, (double)code);
                writer.set(8, rtsResponseAsString);
                this.exaOut.deliver(new IOTable(writer.create()));
            } catch (IOException | AdminToolsException var27) {
                throw new OperatorException(var27.getMessage());
            } finally {
                try {
                    FileUtils.forceDelete(downloadedDeploymentZip);
                } catch (IOException var24) {
                    LOGGER.info(ExceptionUtils.getStackTrace(var24));
                }

            }

        }
    }

    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeString("deployment_name", "name of your deployment", "my-deployment", false));
        types.add(new ParameterTypeString("project_name", "repository name", "sample-test", false));
        types.add(new ParameterTypeString("deployment_location", "deployment location", "/"));
        types.add(new ParameterTypeString("git_reference", "ref", "master"));
        types.add(new ParameterTypeEnumeration("connections", "connections", new ParameterTypeString("connection_name", "connection name", true), false));
        types.add(new ParameterTypeInt("time_out", "time out (seconds)", 0, Integer.MAX_VALUE, 0));
        ParameterType isCont = new ParameterTypeBoolean("continuous_execution", "set to continuous execution", false, true);
        ParameterType sleepTime = new ParameterTypeInt("sleep_time", "sleep time for continuous execution in ms", 0, Integer.MAX_VALUE, 0);
        sleepTime.registerDependencyCondition(new BooleanParameterCondition(this, "continuous_execution", true, true));
        types.add(isCont);
        types.add(sleepTime);
        return types;
    }

    public List<String> getColumnLabels() {
        return columnLabels;
    }

    public List<Column.TypeId> getColumnTypes() {
        return columnTypes;
    }

    private void getDeploymentZip(Path target, VersionNumber versionNumber, String endpoint, List<String> connections) throws OperatorException, AdminToolsException {
        AssertUtility.notNull(target, "'target' cannot be null");
        AssertUtility.notNull(versionNumber, "'versionNumber' cannot be null");
        AssertUtility.hasText(endpoint, "'endpoint' cannot be blank");
        AssertUtility.notNull(connections, "'connections' cannot be null");
        ConnectionInformationContainerIOObject conObject = (ConnectionInformationContainerIOObject)this.aiHubConnection.getData(ConnectionInformationContainerIOObject.class);
        AIHubConnectionManager manager = new AIHubConnectionManager(conObject, this);

        try {
            LOGGER.info("Receiving Bearer and JWT Token");
            String bearerToken = manager.getIdentityProviderToken();
            String jwtToken = manager.getAuthToken(bearerToken);
            OkHttpClient client = (new OkHttpClient()).newBuilder().readTimeout((long)this.getParameterAsInt("time_out"), TimeUnit.SECONDS).writeTimeout((long)this.getParameterAsInt("time_out"), TimeUnit.SECONDS).build();
            LOGGER.info(() -> {
                return String.format("Calling endpoint to get deployment.zip: %s", endpoint);
            });
            Request request;
            if (versionNumber.isAtMost(VERSION_BELOW_10_2_0)) {
                request = (new Request.Builder()).url(endpoint).method(RequestMethod.GET.name(), (RequestBody)null).addHeader("Authorization", String.format("Bearer %s", jwtToken)).build();
            } else {
                RequestBody body = this.getDeploymentCreationRequestBody(manager.getAIHubUrl(), jwtToken, this.getParameterAsString("project_name"), this.getSanitizedRepositoryLocation(), this.getParameterAsString("git_reference"), connections);
                request = (new Request.Builder()).url(endpoint).method(RequestMethod.POST.name(), body).addHeader("Authorization", String.format("Bearer %s", jwtToken)).addHeader("Content-Type", "application/json").build();
            }

            LargeDownloadCallback largeDownloadCallback = LargeDownloadCallback.builder().target(target).build();
            client.newCall(request).enqueue(largeDownloadCallback);

            while(!largeDownloadCallback.isReady()) {
                LOGGER.fine(() -> {
                    return String.format("Waiting until REST request to '%s' and method '%s' finished...", request.url(), request.method());
                });

                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException var13) {
                    throw new AdminToolsException(var13.getMessage());
                }
            }

            if (!largeDownloadCallback.isSuccess()) {
                throw new AdminToolsException(String.format("Download failed. Reason: %s", largeDownloadCallback.getError().getMessage()), largeDownloadCallback.getError());
            }
        } catch (AdminToolsException | OperatorException var14) {
            LOGGER.info(ExceptionUtils.getStackTrace(var14));
            throw new OperatorException(String.format("Error in receiving the deployment.zip from your AIHub: %s", var14.getMessage()));
        }
    }

    private String getDeploymentEndpoint(VersionNumber versionNumber, List<String> connections) throws OperatorException, AdminToolsException {
        AssertUtility.notNull(versionNumber, "'versionNumber' cannot be null");
        ConnectionInformationContainerIOObject conObject = (ConnectionInformationContainerIOObject)this.aiHubConnection.getData(ConnectionInformationContainerIOObject.class);
        AIHubConnectionManager manager = new AIHubConnectionManager(conObject, this);
        String aiHubUrl = manager.getAIHubUrl();
        String endpointPath = RestUtility.getEndpointPath(RequestPath.REPOSITORIES, manager.getServerVersion());
        StringBuilder endpointBuilder = new StringBuilder();
        endpointBuilder.append("REPOSITORIES_PLACEHOLDER");
        endpointBuilder.append("/");
        endpointBuilder.append(this.getParameterAsString("project_name"));
        endpointBuilder.append("/deployment/");
        endpointBuilder.append(this.getParameterAsString("git_reference"));
        if (versionNumber.isAtMost(VERSION_BELOW_10_2_0)) {
            endpointBuilder.append("?deploymentLocation=");
            endpointBuilder.append(this.getSanitizedRepositoryLocation());
            endpointBuilder.append("&deploymentName=");
            endpointBuilder.append(this.getParameterAsString("deployment_name"));
            endpointBuilder.append("&continuous=");
            endpointBuilder.append(this.getParameterAsString("continuous_execution"));
            endpointBuilder.append("&sleep=");
            endpointBuilder.append(this.getParameterAsInt("sleep_time"));
            if (CollectionUtils.isNotEmpty(connections)) {
                endpointBuilder.append("&connection=");
                endpointBuilder.append(String.join(",", connections));
            }
        }

        String endpoint = endpointBuilder.toString();
        return String.format("%s%s", aiHubUrl, endpoint.replaceFirst("REPOSITORIES_PLACEHOLDER", endpointPath));
    }

    private RequestBody getDeploymentCreationRequestBody(String baseUrl, String token, String projectName, String baseLocation, String ref, List<String> connections) throws AdminToolsException, OperatorException {
        AssertUtility.hasText(baseUrl, "'baseUrl' cannot be blank");
        AssertUtility.hasText(token, "'token' cannot be blank");
        AssertUtility.hasText(projectName, "'projectName' cannot be blank");
        AssertUtility.hasText(baseLocation, "'baseLocation' cannot be blank");
        AssertUtility.hasText(ref, "'ref' cannot be blank");
        AssertUtility.notNull(connections, "'connections' cannot be null");
        MediaType mediaType = MediaType.parse("application/json");

        try {
            Map<String, String> processes = this.getProcesses(baseUrl, token, projectName, baseLocation, ref);
            if (processes.isEmpty()) {
                throw new OperatorException(String.format("No processes in base location '%s' of project '%s' found", baseLocation, projectName));
            } else {
                Set<CreateDeploymentProcessLocationRequest> processLocations = new HashSet(0);
                Iterator var10 = processes.entrySet().iterator();

                while(var10.hasNext()) {
                    Map.Entry<String, String> entry = (Map.Entry)var10.next();
                    String processLocation = (String)entry.getKey();
                    String alias = (String)entry.getValue();
                    CreateDeploymentProcessLocationRequest processLocationRequest = CreateDeploymentProcessLocationRequest.builder().path(alias).processLocation(processLocation).build();
                    processLocations.add(processLocationRequest);
                }

                Set<String> additionalLocations = new HashSet(0);
                if (!StringUtils.endsWithIgnoreCase(baseLocation, "/")) {
                    additionalLocations.add(baseLocation + "/");
                } else {
                    additionalLocations.add(baseLocation);
                }

                connections.forEach((c) -> {
                    if (StringUtils.isNotBlank(c)) {
                        if (!StringUtils.endsWithIgnoreCase(c, ".conninfo")) {
                            c = c + ".conninfo";
                        }

                        additionalLocations.add(String.format("/Connections/%s", c));
                    }

                });
                CreateDeploymentRequest request = new CreateDeploymentRequest();
                request.setDeploymentName(this.getParameterAsString("deployment_name"));
                request.setProcessLocations(processLocations);
                request.setAdditionalLocations(additionalLocations);
                int sleep = this.getParameterAsInt("sleep_time");
                if (sleep > 0) {
                    request.setSleep((long)sleep);
                }

                boolean continuous = this.getParameterAsBoolean("continuous_execution");
                if (continuous) {
                    request.setContinuous(true);
                }

                String json = (new ObjectMapper()).writeValueAsString(request);
                LOGGER.fine(() -> {
                    return String.format("Creating deployment.zip request body is %s", json);
                });
                return RequestBody.create(json, mediaType);
            }
        } catch (JsonProcessingException var15) {
            throw new OperatorException(String.format("JSON cannot be processed. Reason: %s", var15.getMessage()), var15);
        } catch (AdminToolsException var16) {
            throw new OperatorException(var16.getMessage(), var16);
        }
    }

    private Map<String, String> getProcesses(String baseUrl, String token, String projectName, String location, String ref) throws AdminToolsException, OperatorException {
        AssertUtility.hasText(baseUrl, "'baseUrl' cannot be blank");
        AssertUtility.hasText(token, "'token' cannot be blank");
        AssertUtility.hasText(projectName, "'projectName' cannot be blank");
        AssertUtility.hasText(location, "'location' cannot be blank");
        AssertUtility.hasText(ref, "'ref' cannot be blank");
        String endpoint = RestUtility.getEndpointPath(RequestPath.REPOSITORIES, "above_10.0");
        String url = baseUrl + endpoint + "/" + projectName + "/contents/" + ref + "?location=" + location;
        LOGGER.info(() -> {
            return String.format("Calling endpoint to get list of available processes: %s", url);
        });
        Request request = (new Request.Builder()).url(url).method(RequestMethod.GET.name(), (RequestBody)null).addHeader("Authorization", String.format("Bearer %s", token)).addHeader("Content-Type", "application/json").build();
        Map<String, String> processes = new HashMap(0);
        OkHttpClient client = (new OkHttpClient()).newBuilder().readTimeout((long)this.getParameterAsInt("time_out"), TimeUnit.SECONDS).build();

        try {
            Response response = client.newCall(request).execute();

            try {
                int code = response.code();
                if (!response.isSuccessful()) {
                    throw new OperatorException(String.format("%d: %s", code, RestUtility.parseErrorFromResponse(response)));
                }

                ResponseBody body = response.body();
                if (body == null) {
                    throw new OperatorException("Expected a body for retrieving process list, but none found");
                }

                String jsonString = body.string();
                ObjectMapper objectMapper = RestUtility.getMapper();
                List<ProjectContentResponse> result = (List)objectMapper.readValue(jsonString, new TypeReference<List<ProjectContentResponse>>() {
                });
                result.stream().filter((p) -> {
                    return ProjectContentItemType.FILE.equals(p.getType()) && StringUtils.endsWithIgnoreCase(p.getName(), ".rmp");
                }).forEach((p) -> {
                    String alias = StringUtils.removeEndIgnoreCase(p.getName(), ".rmp");
                    String processLocation;
                    if (!"/".equals(location)) {
                        processLocation = location + "/" + p.getName();
                    } else {
                        processLocation = p.getName();
                    }

                    processes.put(processLocation, alias);
                });
            } catch (Throwable var18) {
                if (response != null) {
                    try {
                        response.close();
                    } catch (Throwable var17) {
                        var18.addSuppressed(var17);
                    }
                }

                throw var18;
            }

            if (response != null) {
                response.close();
            }
        } catch (IOException var19) {
            LOGGER.info(ExceptionUtils.getStackTrace(var19));
            throw new OperatorException(String.format("Error in receiving process list for repository location '%s' from your AIHub: %s", this.getSanitizedRepositoryLocation(), var19.getMessage()));
        }

        LOGGER.fine(() -> {
            return String.format("Get list of available processes: %s", processes);
        });
        return processes;
    }

    private String getSanitizedRepositoryLocation() throws OperatorException {
        String repositoryLocation = this.getParameterAsString("deployment_location");
        if (!StringUtils.startsWith(repositoryLocation, "/")) {
            repositoryLocation = "/" + repositoryLocation;
        }

        return repositoryLocation;
    }

    static {
        columnTypes = Arrays.asList(TypeId.NOMINAL, TypeId.NOMINAL, TypeId.NOMINAL, TypeId.NOMINAL, TypeId.INTEGER_53_BIT, TypeId.NOMINAL, TypeId.INTEGER_53_BIT, TypeId.INTEGER_53_BIT, TypeId.NOMINAL);
    }
}
