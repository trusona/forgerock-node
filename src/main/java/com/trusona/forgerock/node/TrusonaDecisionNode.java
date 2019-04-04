package com.trusona.forgerock.node;

import static com.trusona.forgerock.auth.Constants.CALLBACK_ZERO;
import static com.trusona.forgerock.auth.Constants.ENDPOINT_URL_PRODUCTION;
import static com.trusona.forgerock.auth.Constants.ENDPOINT_URL_UAT;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.sun.identity.sm.DNMapper;
import com.trusona.client.TrusonaClient;
import com.trusona.client.config.TrusonaClientConfig;
import com.trusona.client.v1.TrusonaClientV1;
import com.trusona.forgerock.auth.TrusonaEnvResolver;
import com.trusona.forgerock.auth.authenticator.Trusonaficator;
import com.trusona.forgerock.auth.callback.CallbackFactory;
import com.trusona.sdk.Trusona;
import com.trusona.sdk.TrusonaEnvironment;
import com.trusona.sdk.resources.exception.TrusonaException;
import java.net.URL;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.CoreWrapper;

@Node.Metadata(outcomeProvider = TrusonaOutcomesProvider.class,
  configClass = TrusonaDecisionNodeConfig.class)
public class TrusonaDecisionNode implements Node {

  private final TrusonaDecisionNodeConfig nodeConfig;
  private final StateDelegate stateDelegate;
  private final CoreWrapper coreWrapper;

  @Inject
  public TrusonaDecisionNode(@Assisted TrusonaDecisionNodeConfig nodeConfig, CoreWrapper coreWrapper) {
    this.nodeConfig = nodeConfig;
    this.coreWrapper = coreWrapper;

    TrusonaEnvironment trusonaEnvironment = new TrusonaEnvResolver().getEnvironment();
    Trusona trusona = new Trusona(nodeConfig.apiToken(), new String(nodeConfig.apiSecret()), trusonaEnvironment);

    String webSdkConfig;

    try {
      webSdkConfig = trusona.getWebSdkConfig();
    }
    catch (TrusonaException e) {
      throw new RuntimeException("Could not get Web SDK Config. Please verify your Trusona API Token", e);
    }

    TrusonaClientConfig trusonaClientConfig = new TrusonaClientConfig();
    trusonaClientConfig.setAccessToken(nodeConfig.apiToken());
    trusonaClientConfig.setMacKey(new String(nodeConfig.apiSecret()));
    trusonaClientConfig.setEndpointUrl(getEndpointUrl(trusonaEnvironment));

    TrusonaClient trusonaClient = new TrusonaClientV1(trusonaClientConfig);

    stateDelegate = new StateDelegate(
      new CallbackFactory(webSdkConfig, nodeConfig.deeplinkUrl(), CALLBACK_ZERO),
      new Trusonaficator(trusona, nodeConfig.action(), nodeConfig.resource(), nodeConfig.authenticationLevel()),
      trusona,
      trusonaClient,
      nodeConfig.userAliasList(),
      DNMapper::orgNameToDN
    );
  }

  @Override
  public Action process(TreeContext treeContext) throws NodeProcessException {
    return stateDelegate.getState(treeContext).get();
  }

  private URL getEndpointUrl(TrusonaEnvironment trusonaEnvironment) {
    switch (trusonaEnvironment) {
      case UAT:
        return ENDPOINT_URL_UAT;

      case PRODUCTION:
        return ENDPOINT_URL_PRODUCTION;

      default:
        throw new RuntimeException(String.format("Unsupported Trusona environment configured: %s", trusonaEnvironment));
    }
  }
}