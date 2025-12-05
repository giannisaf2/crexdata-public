/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.rapidminer.connection.ConnectionInformation
 *  com.rapidminer.connection.gui.AbstractConnectionGUI
 *  com.rapidminer.connection.gui.ConnectionGUIProvider
 *  com.rapidminer.repository.RepositoryLocation
 */
package com.rapidminer.extension.admin.gui;

import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.gui.AbstractConnectionGUI;
import com.rapidminer.connection.gui.ConnectionGUIProvider;
import com.rapidminer.repository.RepositoryLocation;

import java.awt.*;

public class AIHubConnectionGUIProvider
implements ConnectionGUIProvider {
    public AbstractConnectionGUI edit(Window parent, ConnectionInformation connection, RepositoryLocation location, boolean editable) {
        return new AIHubConnectionGui(parent, connection, location, editable);
    }
}

