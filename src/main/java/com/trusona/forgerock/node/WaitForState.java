package com.trusona.forgerock.node;

import com.sun.identity.shared.debug.Debug;
import com.trusona.forgerock.auth.TrusonaDebug;
import com.trusona.forgerock.auth.principal.PrincipalMapper;
import com.trusona.sdk.resources.TrusonaApi;
import com.trusona.sdk.resources.dto.TrusonaficationResult;
import com.trusona.sdk.resources.exception.TrusonaException;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.authentication.callbacks.PollingWaitCallback;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.forgerock.openam.authentication.callbacks.PollingWaitCallback.PollingWaitCallbackBuilder;

import static com.trusona.forgerock.auth.Constants.WAIT_TIME;
import static com.trusona.forgerock.node.TrusonaOutcomes.*;
import static com.trusona.sdk.resources.dto.TrusonaficationStatus.EXPIRED;
import static com.trusona.sdk.resources.dto.TrusonaficationStatus.IN_PROGRESS;
import static com.trusona.sdk.resources.dto.TrusonaficationStatus.REJECTED;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;

public class WaitForState implements Supplier<Action> {
  private final TrusonaApi      trusona;
  private final PrincipalMapper principalMapper;
  private final UUID            trusonaficationId;
  private final JsonValue       currentState;
  private final Debug           debug;

  public WaitForState(TrusonaApi trusona, PrincipalMapper principalMapper, UUID trusonaficationId, JsonValue currentState) {
    this.trusona = trusona;
    this.principalMapper = principalMapper;
    this.trusonaficationId = trusonaficationId;
    this.currentState = currentState;
    this.debug = TrusonaDebug.getInstance();
  }

  @Override
  public Action get() {
    debug.message("in WaitForState");
    try {
     return actionForResult(trusona.getTrusonaficationResult(trusonaficationId)).build();
    }
    catch (TrusonaException e) {
      debug.error("Got a Trusona API exception when trying to get TrusonaficationResult", e);
      return Action.goTo(ERROR_OUTCOME.id).build();
    }
  }

  private Action.ActionBuilder actionForResult(TrusonaficationResult result) {
    if (result.isSuccessful()) {
      Optional<Action.ActionBuilder> action = principalMapper.mapPrincipal(result).map(p ->
        Action.goTo(ACCEPTED_OUTCOME.id)
          .replaceSharedState(stateWithUsername(p.getName())));

      if (!action.isPresent()) {
        debug.error("Unable to find a user with TrusonaficationResult => {}", result);
      }

      return action.orElseGet(() -> Action.goTo(ERROR_OUTCOME.id));
      
    } else if (result.getStatus().equals(REJECTED)) {
      return Action.goTo(REJECTED_OUTCOME.id);
    } else if (result.getStatus().equals(EXPIRED)) {
      return Action.goTo(EXPIRED_OUTCOME.id);
    } else if (result.getStatus().equals(IN_PROGRESS)) {
      PollingWaitCallbackBuilder builder = new PollingWaitCallbackBuilder().withWaitTime(WAIT_TIME);
      return Action.send(builder.build());
    } else {
      debug.error("Got an unexpected Trusonafication Result: " + result.toString());
      return Action.goTo(ERROR_OUTCOME.id);
    }
  }

  private JsonValue stateWithUsername(String username) {
    JsonValue newState = currentState.copy();
    newState.put(USERNAME, username);

    return newState;
  }

  @Override
  public String toString() {
    return "WaitForState[trusonaficationId=" + trusonaficationId.toString() + "]";
  }
}
