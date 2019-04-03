package com.trusona.forgerock.node;

import static com.trusona.forgerock.auth.Constants.TRUSONAFICATION_ID;
import static com.trusona.forgerock.auth.Constants.WAIT_TIME;
import static com.trusona.forgerock.node.TrusonaOutcomes.ERROR_OUTCOME;

import com.sun.identity.authentication.spi.AuthLoginException;
import com.trusona.forgerock.auth.TrusonaDebug;
import com.trusona.forgerock.auth.authenticator.Authenticator;
import com.trusona.forgerock.auth.callback.CallbackFactory;
import com.trusona.forgerock.auth.callback.TrucodeIdCallback;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.authentication.callbacks.PollingWaitCallback.PollingWaitCallbackBuilder;

public class TrucodeState implements Supplier<Action> {

  private final Authenticator authenticator;
  private final CallbackFactory callbackFactory;
  private final JsonValue currentState;
  private final UUID trucodeId;
  private final String payload;

  public TrucodeState(Authenticator authenticator, CallbackFactory callbackFactory, JsonValue currentState, UUID trucodeId,
                      String payload) {
    this.authenticator = authenticator;
    this.callbackFactory = callbackFactory;
    this.currentState = currentState;
    this.trucodeId = trucodeId;
    this.payload = payload;
  }

  @Override
  public Action get() {
    TrusonaDebug.getInstance().message("In TrucodeState");
    Action.ActionBuilder action = Action.goTo(ERROR_OUTCOME.id);
    JsonValue newState = currentState.copy();

    try {
      UUID trusonaficationId = authenticator.createTrusonafication(new TrucodeIdCallback(trucodeId));
      newState.add(TRUSONAFICATION_ID, trusonaficationId.toString());

      if (StringUtils.isNotBlank(payload)) {
        action = Action.send(
          callbackFactory.makeScriptCallback(String.format("app.saveTrusonaficationCookie('%s');", trusonaficationId)),
          callbackFactory.makeRedirectCallback(payload)
        );
      }
      else {
        PollingWaitCallbackBuilder builder = new PollingWaitCallbackBuilder().withWaitTime(WAIT_TIME);
        action = Action.send(builder.build());
      }
    }
    catch (AuthLoginException e) {
      TrusonaDebug.getInstance().error("Error when creating Trusonafication", e);
    }

    return action.replaceSharedState(newState).build();
  }

  @Override
  public String toString() {
    return "TrucodeState[trucodeId=" + trucodeId.toString() + ", payload=" + Optional.ofNullable(payload).orElse("null") + "]";
  }
}
