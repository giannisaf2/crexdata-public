/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.rapidminer.connection.ConnectionInformation
 *  com.rapidminer.connection.gui.DefaultConnectionGUI
 *  com.rapidminer.connection.gui.components.InjectableComponentWrapper
 *  com.rapidminer.connection.gui.model.ConnectionModel
 *  com.rapidminer.connection.gui.model.ConnectionParameterGroupModel
 *  com.rapidminer.connection.gui.model.ConnectionParameterModel
 *  com.rapidminer.repository.RepositoryLocation
 *  javafx.beans.value.ObservableValue
 */
package com.rapidminer.extension.admin.gui;

import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.gui.DefaultConnectionGUI;
import com.rapidminer.connection.gui.components.InjectableComponentWrapper;
import com.rapidminer.connection.gui.model.ConnectionModel;
import com.rapidminer.connection.gui.model.ConnectionParameterGroupModel;
import com.rapidminer.connection.gui.model.ConnectionParameterModel;
import com.rapidminer.repository.RepositoryLocation;
import javafx.beans.value.ObservableValue;

import javax.swing.*;
import java.awt.*;

public class RTSAConnectionGui
extends DefaultConnectionGUI {
    protected RTSAConnectionGui(Window parent, ConnectionInformation connection, RepositoryLocation location, boolean editable) {
        super(parent, connection, location, editable);
    }

    public JComponent getComponentForGroup(ConnectionParameterGroupModel groupModel, ConnectionModel connectionModel) {
        ConnectionParameterModel url = this.getOrCreateParameter(groupModel, "server_url");
        ConnectionParameterModel authMethod = this.getOrCreateParameter(groupModel, "authentication");
        ConnectionParameterModel bearerToken = this.getOrCreateParameter(groupModel, "bearer_token");
        ConnectionParameterModel userName = this.getOrCreateParameter(groupModel, "user_name");
        ConnectionParameterModel password = this.getOrCreateParameter(groupModel, "password");
        userName.enabledProperty().bind((ObservableValue)authMethod.valueProperty().isEqualTo("basic_auth"));
        password.enabledProperty().bind((ObservableValue)authMethod.valueProperty().isEqualTo("basic_auth"));
        bearerToken.enabledProperty().bind((ObservableValue)authMethod.valueProperty().isEqualTo("bearer_auth"));
        return super.getComponentForGroup(groupModel, connectionModel);
    }

    protected JComponent getParameterInputComponent(String type, ConnectionParameterModel parameter) {
        String paramName = parameter.getName();
        if (paramName.equals("authentication")) {
            return InjectableComponentWrapper.getInjectableCombobox((ConnectionParameterModel)parameter, (String[])new String[]{"no_authentication", "basic_auth", "bearer_auth"}, (String)"no_authentication");
        }
        if (paramName.equals("server_version")) {
            return InjectableComponentWrapper.getInjectableCombobox((ConnectionParameterModel)parameter, (String[])new String[]{"above_10.0", "below_10.0"}, (String)"above_10.0");
        }
        return super.getParameterInputComponent(type, parameter);
    }

    private ConnectionParameterModel getOrCreateParameter(ConnectionParameterGroupModel group, String paramName) {
        ConnectionParameterModel parameter = group.getParameter(paramName);
        if (parameter != null) {
            return parameter;
        }
        boolean isEncrypted = false;
        group.addOrSetParameter(paramName, "", isEncrypted, null, true);
        return group.getParameter(paramName);
    }
}

