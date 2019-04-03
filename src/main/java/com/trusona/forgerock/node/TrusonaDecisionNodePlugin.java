package com.trusona.forgerock.node;

import com.google.inject.Inject;
import com.trusona.forgerock.auth.TrusonaDebug;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import org.forgerock.openam.auth.node.api.AbstractNodeAmPlugin;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.plugins.PluginException;
import org.forgerock.openam.plugins.StartupType;
import org.forgerock.openam.sm.AnnotatedServiceRegistry;


public class TrusonaDecisionNodePlugin extends AbstractNodeAmPlugin {

  private final AnnotatedServiceRegistry serviceRegistry;
  private final String version;

  @Inject
  public TrusonaDecisionNodePlugin(AnnotatedServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
    this.version = initVersion();
  }

  @Override
  public String getPluginVersion() {
    return version;
  }

  @Override
  public void onStartup(StartupType startupType) throws PluginException {
    switch (startupType) {
      case FIRST_TIME_INSTALL:
      case FIRST_TIME_DEMO_INSTALL:
      case NORMAL_STARTUP:
        start();
        break;
    }
  }

  @Override
  protected Map<String, Iterable<? extends Class<? extends Node>>> getNodesByVersion() {
    return Collections.singletonMap(version, Collections.singletonList(TrusonaDecisionNode.class));
  }

  private String initVersion() {
    String path = "com/trusona/forgerock/node/plugin-version.properties";
    Properties properties = new Properties();

    try {
      properties.load(getClass().getClassLoader().getResourceAsStream(path));
    }
    catch (NullPointerException | IOException e) {
      TrusonaDebug.getInstance().error("failed to load version", e);
    }

    return properties.getProperty("version", "unspecified");
  }

  private void start() throws PluginException {
    for (Iterable<? extends Class<? extends Node>> iterable : getNodesByVersion().values()) {
      for (Class<? extends Node> clazz : iterable) {
        pluginTools.installAuthNode(clazz);
        pluginTools.startAuthNode(clazz);
      }
    }
  }
}