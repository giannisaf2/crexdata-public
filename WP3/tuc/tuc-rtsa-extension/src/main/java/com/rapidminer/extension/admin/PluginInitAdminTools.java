/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.rapidminer.connection.ConnectionHandlerRegistry
 *  com.rapidminer.connection.gui.ConnectionGUIProvider
 *  com.rapidminer.connection.gui.ConnectionGUIRegistry
 *  com.rapidminer.connection.util.GenericHandler
 *  com.rapidminer.gui.MainFrame
 */
package com.rapidminer.extension.admin;

import com.rapidminer.connection.ConnectionHandler;
import com.rapidminer.connection.ConnectionHandlerRegistry;
import com.rapidminer.connection.gui.ConnectionGUIProvider;
import com.rapidminer.connection.gui.ConnectionGUIRegistry;
import com.rapidminer.connection.util.GenericHandler;
import com.rapidminer.extension.admin.connection.AIHubConnectionHandler;
import com.rapidminer.extension.admin.connection.RTSAConnectionHandler;
import com.rapidminer.extension.admin.gui.AIHubConnectionGUIProvider;
import com.rapidminer.extension.admin.gui.RTSAConnectionGUIProvider;
import com.rapidminer.gui.MainFrame;

public final class PluginInitAdminTools {
    private PluginInitAdminTools() {
    }

    public static void initPlugin() {
        ConnectionHandlerRegistry.getInstance().registerHandler((ConnectionHandler) new AIHubConnectionHandler());
        ConnectionHandlerRegistry.getInstance().registerHandler((ConnectionHandler) new RTSAConnectionHandler());
    }

    public static void initGui(MainFrame mainframe) {
        ConnectionGUIRegistry.INSTANCE.registerGUIProvider((ConnectionGUIProvider)new RTSAConnectionGUIProvider(), RTSAConnectionHandler.getInstance().getType());
        ConnectionGUIRegistry.INSTANCE.registerGUIProvider((ConnectionGUIProvider)new AIHubConnectionGUIProvider(), AIHubConnectionHandler.getInstance().getType());
    }

    public static void initFinalChecks() {
    }

    public static void initPluginManager() {
    }
}

