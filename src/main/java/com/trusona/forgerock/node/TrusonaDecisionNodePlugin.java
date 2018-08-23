package com.trusona.forgerock.node;

import com.google.inject.Inject;
import org.forgerock.openam.auth.node.api.AbstractNodeAmPlugin;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.plugins.PluginException;
import org.forgerock.openam.sm.AnnotatedServiceRegistry;

import java.util.Collections;


public class TrusonaDecisionNodePlugin extends AbstractNodeAmPlugin {

    private final AnnotatedServiceRegistry serviceRegistry;

    @Inject
    public TrusonaDecisionNodePlugin(AnnotatedServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    @Override
    public String getPluginVersion() {
        return "1.0.0";
    }

    @Override
    public void onStartup() throws PluginException {
        for (Class<? extends Node> nodeClass : getNodes()) {
            pluginTools.registerAuthNode(nodeClass);
        }
    }

    @Override
    protected Iterable<? extends Class<? extends Node>> getNodes() {
        return Collections.singletonList(TrusonaDecisionNode.class);
    }
}