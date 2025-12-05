
/**
 * Authors: Ourania Ntouni
 * <p>
 * Copyright (C) 2025-2026 Technical University of Crete
 */

package com.rapidminer.extension.admin.operator.rtsa;

import com.rapidminer.connection.ConnectionInformationContainerIOObject;
import com.rapidminer.extension.admin.operator.projects.DeleteProjectOperator;
import com.rapidminer.operator.*;
import com.rapidminer.operator.ports.PortPairExtender;
import com.rapidminer.operator.repository.RepositoryEntryCopyOperator;
import com.rapidminer.operator.io.RepositorySource;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.repository.*;
import com.rapidminer.repository.local.LocalRepository;
import com.rapidminer.tools.encryption.EncryptionProvider;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.SubprocessTransformRule;
import com.rapidminer.parameter.*;
import com.rapidminer.extension.admin.operator.projects.CreateProjectOperator;
import com.rapidminer.extension.admin.operator.projects.AddContentsToProjectOperator;
import com.rapidminer.tools.LogService;
import groovy.lang.Tuple2;
import com.rapidminer.Process;


import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;

public class EdgeProcessing extends OperatorChain implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final String PARAMETER_REPOSITORY_PATH = "repository_path";
    public InputPort aiHubConnection = this.getInputPorts().createPort("aihub_connection", ConnectionInformationContainerIOObject.class);
    public InputPort rtsaConnection = this.getInputPorts().createPort("rtsa_connection", ConnectionInformationContainerIOObject.class);
    private final PortPairExtender inputPortPairExtender = new PortPairExtender(
            "in", getInputPorts(), getSubprocess(0).getInnerSources());

    private final PortPairExtender outputPortPairExtender = new PortPairExtender(
            "out", getSubprocess(0).getInnerSinks(), getOutputPorts());

    private static final Logger LOGGER =  LogService.getRoot();

    public EdgeProcessing(OperatorDescription description) {
        super(description, "EdgeProcessing");
        inputPortPairExtender.start();
        outputPortPairExtender.start();

        getTransformer().addRule(inputPortPairExtender.makePassThroughRule());
        getTransformer().addRule(new SubprocessTransformRule(getSubprocess(0)));
        getTransformer().addRule(outputPortPairExtender.makePassThroughRule());
    }

    public PortPairExtender getInputPortPairExtender() {
        return inputPortPairExtender;
    }

    public PortPairExtender getOutputPortPairExtender() {
        return outputPortPairExtender;
    }


    @Override
    public void doWork() throws OperatorException {
        createProjectOnAIHub();
        Path tempRepoDir;
        try {
            tempRepoDir = Files.createTempDirectory("temp_local_repo");
        } catch (IOException e) {
            throw new OperatorException("Could not create temporary repository directory", e);
        }

        LocalRepository localRepo = null;
        try {
            localRepo = new LocalRepository("TempRepo", new File(tempRepoDir.toString()));
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }

        RepositoryManager.getInstance((RepositoryAccessor) null)
                .addRepository(localRepo);

        String fileName = "workflow.rmp";
        Tuple2<List<String>, String> rmpContent = generateRMPContent(fileName);
        String xml = rmpContent.getSecond();
        List<String> connections = rmpContent.getFirst();
        try {
            // 1) Create /newProject and a sample process (your existing lines)
            Folder newProjectFolder = localRepo.createFolder("newProject");
            newProjectFolder.createProcessEntry("test", xml);

        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }

        AddContentsToProjectOperator addOp =
                new AddContentsToProjectOperator(getOperatorDescription());
        ConnectionInformationContainerIOObject conn =
                aiHubConnection.getData(ConnectionInformationContainerIOObject.class);

        ConnectionInformationContainerIOObject rtsaConn =
                rtsaConnection.getData(ConnectionInformationContainerIOObject.class);
        addOp.getInputPorts().getPortByName("tar_aihub_connection").receive(conn);
        addOp.getInputPorts().getPortByName("src_aihub_connection").receive(conn);

        addOp.setParameter("source_repository_type",   "LOCAL");
        addOp.setParameter("source_repository_folder", "//TempRepo/");
        addOp.setParameter("target_project_name", getParameterAsString("name"));
        addOp.doWork();
        RepositoryLocation dataDir = getParameterAsRepositoryLocationData(PARAMETER_REPOSITORY_PATH, DataEntry.class);
        dataDir.setLocationType(RepositoryLocationType.UNKNOWN);
        LOGGER.info("the path is " + dataDir.getPath());

        addOp.setParameter("source_repository_type",   "LOCAL");
        addOp.setParameter("source_repository_folder", "//Local Repository" + dataDir.getPath());
        addOp.setParameter("target_project_name", getParameterAsString("name") );
        addOp.setParameter("target_location", "/newProject/" + dataDir.getName() );

        addOp.doWork();

        RepositoryManager.getInstance((RepositoryAccessor) null)
                .removeRepository(localRepo);

        DeployOnRTSAOperator deployOnRTSAOperator = new DeployOnRTSAOperator(getOperatorDescription());
        deployOnRTSAOperator.getInputPorts().getPortByName("aihub_connection").receive(conn);
        deployOnRTSAOperator.getInputPorts().getPortByName("rtsa_connection").receive(rtsaConn);

        deployOnRTSAOperator.setParameter("deployment_name", "new_deployment");
        deployOnRTSAOperator.setParameter("deployment_location", "/newProject");

        LOGGER.info(() -> "Connections used in the workflow: " + String.join(",", connections));
        deployOnRTSAOperator.setParameter("connections", String.join(",", connections));
        deployOnRTSAOperator.setParameter("project_name", this.getParameterAsString("name"));

        deployOnRTSAOperator.setParameter("continuous_execution", String.valueOf(this.getParameterAsBoolean("continuous_execution")));
        deployOnRTSAOperator.setParameter("sleep_time", String.valueOf(this.getParameterAsInt("sleep_time")));
        deployOnRTSAOperator.doWork();

        DeleteProjectOperator deleteProjectOperator = new DeleteProjectOperator(getOperatorDescription());
        deleteProjectOperator.setParameter("project_name", getParameterAsString("name"));
        deleteProjectOperator.getInputPorts().getPortByName("con").receive(conn);
        deleteProjectOperator.doWork();


        try {
            deleteDirectoryRecursively(tempRepoDir.toFile());
        } catch (IOException ignored) { /* best effort */ }
    }

    private void deleteDirectoryRecursively(File directory) throws IOException {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectoryRecursively(file);
                }
            }
        }
        if (!directory.delete()) {
            throw new IOException("Failed to delete file or directory: " + directory.getAbsolutePath());
        }
    }

    private static String ensureAbsolute(String path, String repoRoot /* e.g. "//Local Repository" */) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Repository path cannot be null or empty");
        }
        if (path.startsWith("//")) return path;           // already absolute
        if (path.startsWith("/"))  return repoRoot + path; // make absolute
        return repoRoot + "/" + path;                      // safety
    }
    private static List<String> copySubprocess(ExecutionUnit src, ExecutionUnit dst) throws OperatorException {

        List<String> connections = new ArrayList<>();
        Map<Operator, Operator> clones = new HashMap<>();
        final String SRC_ROOT  = "//Local Repository"; // where your existing Connections live
        final String DEST_ROOT = "//TempRepo";

        for (Operator op : src.getEnabledOperators()) {
            Operator copy = op.cloneOperator(op.getName(), true);   // deep copy
            if ( op instanceof com.rapidminer.operator.io.RepositorySource) {
                RepositorySource source = (RepositorySource) op;
                String className = source.getOutputPorts().getPortByIndex(0).getOpposite().getMetaData().getClass().getName();

                if (!className.equals("com.rapidminer.operator.ports.metadata.ConnectionInformationMetaData")) {
                    dst.addOperator(copy);
                    clones.put(op, copy);
                    continue;
                }
                String connectionPath = copy.getParameter("repository_entry");
                if (connectionPath == null || connectionPath.isEmpty()) {
                    throw new IllegalArgumentException("Path cannot be null or empty");
                }
                String srcParam = op.getParameter("repository_entry");  // e.g. "/Connections/Kafka connection"
                String absSrc   = ensureAbsolute(srcParam, SRC_ROOT);   // -> "//Local Repository/Connections/Kafka connection"

                // 2) Compute the *destination* absolute path
                String baseName = absSrc.substring(absSrc.lastIndexOf('/') + 1);
                String absDst   = DEST_ROOT + "/Connections/" + baseName; // -> "//TempRepo/Connections/Kafka connection"
                String repoPath = "/Connections/" + baseName;
                copy.setParameter("repository_entry", repoPath);
                connections.add(baseName);
                RepositoryEntryCopyOperator copyOperator =
                        new RepositoryEntryCopyOperator(copy.getOperatorDescription());
                copyOperator.setParameter("source entry", absSrc);
                copyOperator.setParameter("destination", absDst);
                copyOperator.setParameter("overwrite", "true");

                copyOperator.doWork();
            }

            dst.addOperator(copy);
            clones.put(op, copy);
        }

        for (Operator oldOp : src.getEnabledOperators()) {
            Operator newOp = clones.get(oldOp);

            for (InputPort oldIn : oldOp.getInputPorts().getAllPorts()) {
                if (!oldIn.isConnected()) continue;                  // nothing to wire

                OutputPort oldSrc = oldIn.getSource();               // <-- real API
                InputPort  newIn  = newOp.getInputPorts()
                        .getPortByName(oldIn.getName());

                OutputPort newSrc;
                // 2a) source is another operator inside the selection
                if (clones.containsKey(oldSrc.getPorts().getOwner().getOperator())) {
                    newSrc = clones.get(oldSrc.getPorts().getOwner().getOperator())
                            .getOutputPorts()
                            .getPortByName(oldSrc.getName());
                }
                // 2b) source is a subprocess INPUT (inner-source)
                else {
                    newSrc = dst.getInnerSources()
                            .getPortByName(oldSrc.getName());
                }

                if (newSrc != null && newIn != null) {
                    newSrc.connectTo(newIn);
                }
            }
        }

        for (InputPort oldSink : src.getInnerSinks().getAllPorts()) {
            if (!oldSink.isConnected()) continue;

            OutputPort oldSrc = oldSink.getSource();
            Operator   oldSrcOp = oldSrc.getPorts().getOwner().getOperator();

            OutputPort newSrc = clones.get(oldSrcOp)
                    .getOutputPorts()
                    .getPortByName(oldSrc.getName());
            InputPort  newSink = dst.getInnerSinks()
                    .getPortByName(oldSink.getName());

            if (newSrc != null && newSink != null) {
                newSrc.connectTo(newSink);
            }
        }
        return connections;
    }

    private Tuple2<List<String>, String> generateRMPContent(String fileName) throws OperatorException {

        Process embedded = new Process();                            // empty process
        ProcessRootOperator root = (ProcessRootOperator) embedded.getRootOperator();

        List<String> connections = copySubprocess(getSubprocess(0), root.getSubprocess(0));     // clone whole sub

        String xml = root.getXML(true, EncryptionProvider.DEFAULT_CONTEXT);
        LOGGER.info(() -> "Generated XML for " + fileName + ":\n" + xml);

        return new Tuple2<>(connections,xml);
    }

    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> types = super.getParameterTypes();
        types.add(new ParameterTypeString("name", "name", false));
        types.add(new ParameterTypeString("display_name", "display name", false));
        types.add(new ParameterTypeString("description", "description", true));
        ParameterType isCont = new ParameterTypeBoolean("continuous_execution", "set to continuous execution", false, true);
        ParameterType sleepTime = new ParameterTypeInt("sleep_time", "sleep time for continuous execution in ms", 0, Integer.MAX_VALUE, 0);
        sleepTime.registerDependencyCondition(new BooleanParameterCondition(this, "continuous_execution", true, true));
        types.add(isCont);
        types.add(sleepTime);
        ParameterType permissionSettings = new ParameterTypeList("permissions", "Advanced parameters that can be set.", new ParameterTypeString("group_name", "group name", true, true), new ParameterTypeCategory("privilege_name", "privilege", new String[]{"ACCESS", "OWNERSHIP"}, 0), true);
        permissionSettings.setOptional(true);
        types.add(permissionSettings);
        ParameterTypeRepositoryLocation folder = new ParameterTypeRepositoryLocation(PARAMETER_REPOSITORY_PATH, "Folder in the repository to iterate over", false, true, true);
        folder.setExpert(false);
        folder.setPrimary(true);
        types.add(folder);
//        types.add(new ParameterTypeDirectory(PARAMETER_REPOSITORY_PATH, "Path to directory that will be included. ", true));
        types.add(new ParameterTypeBoolean("initialize", "initialize", true, true));
        types.add(new ParameterTypeBoolean("lfs_enabled", "lfs enabled", true, true));
        types.add(new ParameterTypeBoolean("fail_on_error", "fail if there is an error in creating the Project", true));
        types.add(new ParameterTypeString("secret", "secret", true));


        return types;
    }



    private void createProjectOnAIHub() throws OperatorException {
        // Retrieve the AIHub connection from the input port
        ConnectionInformationContainerIOObject aihubConnection = this.aiHubConnection.getData(ConnectionInformationContainerIOObject.class);

        // Create an instance of CreateProjectOperator
        CreateProjectOperator createProjectOperator = new CreateProjectOperator(getOperatorDescription());

        // Pass the AIHub connection to the CreateProjectOperator's input port
        createProjectOperator.getInputPorts().getPortByName("con").receive(aihubConnection);
        createProjectOperator.setParameter("name", getParameterAsString("name"));
        createProjectOperator.setParameter("display_name", getParameterAsString("display_name"));
        createProjectOperator.setParameter("description", getParameterAsString("description"));

        List<String[]> permissions = getParameterList("permissions");
        StringBuilder serializedPermissions = new StringBuilder();
        for (String[] pair : permissions) {
            serializedPermissions.append(pair[0]).append(":").append(pair[1]).append(";");
        }
        createProjectOperator.setParameter("permissions", serializedPermissions.toString());
        createProjectOperator.setParameter("initialize", String.valueOf(getParameterAsBoolean("initialize")));
        createProjectOperator.setParameter("lfs_enabled", String.valueOf(getParameterAsBoolean("lfs_enabled")));
        createProjectOperator.setParameter("fail_on_error", String.valueOf(getParameterAsBoolean("fail_on_error")));
        createProjectOperator.setParameter("secret", getParameterAsString("secret"));

        createProjectOperator.doWork();
    }




}
