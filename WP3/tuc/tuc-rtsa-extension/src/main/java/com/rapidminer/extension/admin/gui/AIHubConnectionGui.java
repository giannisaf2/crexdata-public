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
 *  javafx.beans.value.ObservableBooleanValue
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
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;

import javax.swing.*;
import java.awt.*;

public class AIHubConnectionGui
extends DefaultConnectionGUI {
    protected AIHubConnectionGui(Window parent, ConnectionInformation connection, RepositoryLocation location, boolean editable) {
        super(parent, connection, location, editable);
    }

    public JComponent getComponentForGroup(ConnectionParameterGroupModel groupModel, ConnectionModel connectionModel) {
        ConnectionParameterModel authMethod = this.getOrCreateParameter(groupModel, "authentication");
        ConnectionParameterModel userName = this.getOrCreateParameter(groupModel, "user_name");
        ConnectionParameterModel password = this.getOrCreateParameter(groupModel, "password");
        ConnectionParameterModel grantType = this.getOrCreateParameter(groupModel, "grant_type");
        ConnectionParameterModel realmId = this.getOrCreateParameter(groupModel, "realm_id");
        ConnectionParameterModel clientId = this.getOrCreateParameter(groupModel, "client_id");
        ConnectionParameterModel clientSecret = this.getOrCreateParameter(groupModel, "client_secret");
        ConnectionParameterModel refreshToken = this.getOrCreateParameter(groupModel, "refresh_token");
        userName.enabledProperty().bind((ObservableValue)authMethod.valueProperty().isEqualTo("basic_auth").or((ObservableBooleanValue)authMethod.valueProperty().isEqualTo("token").and((ObservableBooleanValue)grantType.valueProperty().isEqualTo("password"))));
        password.enabledProperty().bind((ObservableValue)authMethod.valueProperty().isEqualTo("basic_auth").or((ObservableBooleanValue)authMethod.valueProperty().isEqualTo("token").and((ObservableBooleanValue)grantType.valueProperty().isEqualTo("password"))));
        refreshToken.enabledProperty().bind((ObservableValue)authMethod.valueProperty().isEqualTo("token").and((ObservableBooleanValue)grantType.valueProperty().isEqualTo("refresh_token")));
        clientSecret.enabledProperty().bind((ObservableValue)authMethod.valueProperty().isEqualTo("token").and((ObservableBooleanValue)grantType.valueProperty().isEqualTo("refresh_token").or((ObservableBooleanValue)grantType.valueProperty().isEqualTo("client_credentials"))));
        clientId.enabledProperty().bind((ObservableValue)authMethod.valueProperty().isEqualTo("token"));
        realmId.enabledProperty().bind((ObservableValue)authMethod.valueProperty().isEqualTo("token"));
        grantType.enabledProperty().bind((ObservableValue)authMethod.valueProperty().isEqualTo("token"));
        return super.getComponentForGroup(groupModel, connectionModel);
    }

    protected JComponent getParameterInputComponent(String type, ConnectionParameterModel parameter) {
        String paramName = parameter.getName();
        if (paramName.equals("authentication")) {
            return InjectableComponentWrapper.getInjectableCombobox((ConnectionParameterModel)parameter, (String[])new String[]{"token", "basic_auth"}, (String)"token");
        }
        if (paramName.equals("server_version")) {
            return InjectableComponentWrapper.getInjectableCombobox((ConnectionParameterModel)parameter, (String[])new String[]{"above_10.0", "below_10.0"}, (String)"above_10.0");
        }
        if (paramName.equals("grant_type")) {
            return InjectableComponentWrapper.getInjectableCombobox((ConnectionParameterModel)parameter, (String[])new String[]{"password", "client_credentials", "refresh_token"}, (String)"password");
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

