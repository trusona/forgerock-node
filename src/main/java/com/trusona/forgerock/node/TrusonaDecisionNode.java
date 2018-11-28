package com.trusona.forgerock.node;

import static com.trusona.forgerock.auth.Constants.CALLBACK_ZERO;
import static com.trusona.forgerock.auth.Constants.ENDPOINT_URL_PRODUCTION;
import static com.trusona.forgerock.auth.Constants.ENDPOINT_URL_UAT;
import static com.trusona.forgerock.node.TrusonaOutcomes.ACCEPTED_OUTCOME;
import static com.trusona.forgerock.node.TrusonaOutcomes.ERROR_OUTCOME;
import static com.trusona.forgerock.node.TrusonaOutcomes.EXPIRED_OUTCOME;
import static com.trusona.forgerock.node.TrusonaOutcomes.REJECTED_OUTCOME;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.RequiredValueValidator;
import com.trusona.client.TrusonaClient;
import com.trusona.client.config.TrusonaClientConfig;
import com.trusona.client.v1.TrusonaClientV1;
import com.trusona.forgerock.auth.TrusonaEnvResolver;
import com.trusona.forgerock.auth.authenticator.Trusonaficator;
import com.trusona.forgerock.auth.authenticator.Trusonaficator.AuthenticationLevel;
import com.trusona.forgerock.auth.callback.CallbackFactory;
import com.trusona.sdk.Trusona;
import com.trusona.sdk.TrusonaEnvironment;
import com.trusona.sdk.resources.exception.TrusonaException;
import java.net.URL;
import java.util.List;
import java.util.Set;
import org.forgerock.guava.common.collect.ImmutableList;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.OutcomeProvider;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.sm.annotations.adapters.Password;
import org.forgerock.util.i18n.PreferredLocales;

@Node.Metadata(outcomeProvider = TrusonaDecisionNode.TrusonaOutcomeProvider.class,
  configClass = TrusonaDecisionNode.Config.class)
public class TrusonaDecisionNode implements Node {
  private final Config config;
  private final CoreWrapper coreWrapper;
  private final StateDelegate stateDelegate;


  @Inject
  public TrusonaDecisionNode(@Assisted Config config, CoreWrapper coreWrapper) {
    this.config = config;
    this.coreWrapper = coreWrapper;

    TrusonaEnvironment trusonaEnvironment = new TrusonaEnvResolver().getEnvironment();
    Trusona            trusona  = new Trusona(config.apiToken(), new String(config.apiSecret()), trusonaEnvironment);

    String webSdkConfig;

    try {
      webSdkConfig = trusona.getWebSdkConfig();
    }
    catch (TrusonaException e) {
      throw new RuntimeException("Could not get Web SDK Config. Please verify your Trusona API Token", e);
    }

    TrusonaClientConfig trusonaClientConfig = new TrusonaClientConfig();
    trusonaClientConfig.setAccessToken(config.apiToken());
    trusonaClientConfig.setMacKey(new String(config.apiSecret()));
    trusonaClientConfig.setEndpointUrl(getEndpointUrl(trusonaEnvironment));

    TrusonaClient trusonaClient = new TrusonaClientV1(trusonaClientConfig);


    stateDelegate = new StateDelegate(
      new CallbackFactory(webSdkConfig, config.deeplinkUrl(), CALLBACK_ZERO),
      new Trusonaficator(trusona, config.action(), config.resource(), config.authenticationLevel()),
      trusona,
      trusonaClient,
      config.userAliasList(),
      DNMapper::orgNameToDN
      );
  }

  @Override
  public Action process(TreeContext treeContext) throws NodeProcessException {
    return stateDelegate.getState(treeContext).get();
  }

  interface Config {

    @Attribute(order = 1, validators = {RequiredValueValidator.class})
    String apiToken();

    @Attribute(order = 2, validators = {RequiredValueValidator.class})
    @Password
    char[] apiSecret();

    @Attribute(order = 3, validators = {RequiredValueValidator.class})
    default AuthenticationLevel authenticationLevel() {
      return AuthenticationLevel.ESSENTIAL;
    }

    @Attribute(order = 4, validators = {RequiredValueValidator.class})
    String action();

    @Attribute(order = 5, validators = {RequiredValueValidator.class})
    String resource();

    @Attribute(order = 6)
    String deeplinkUrl();

    @Attribute(order = 7)
    Set<String> userAliasList();

  }

  public static class TrusonaOutcomeProvider implements OutcomeProvider {
    @Override
    public List<Outcome> getOutcomes(PreferredLocales preferredLocales, JsonValue jsonValue) {
      //TODO: Localization
      return ImmutableList.of(
        ACCEPTED_OUTCOME,
        REJECTED_OUTCOME,
        EXPIRED_OUTCOME,
        ERROR_OUTCOME
      );
    }
  }

  private URL getEndpointUrl(TrusonaEnvironment trusonaEnvironment) {
    switch (trusonaEnvironment) {
      case UAT:
        return ENDPOINT_URL_UAT;

      case PRODUCTION:
        return ENDPOINT_URL_PRODUCTION;

      default:
        throw new RuntimeException("Invalid Trusona environment configured: " + trusonaEnvironment);
    }
  }
}
