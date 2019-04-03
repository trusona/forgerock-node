package com.trusona.forgerock.node;

import static com.trusona.forgerock.auth.Constants.WAIT_TIME;
import static com.trusona.forgerock.node.TrusonaOutcomes.ACCEPTED_OUTCOME;
import static com.trusona.forgerock.node.TrusonaOutcomes.ERROR_OUTCOME;
import static com.trusona.forgerock.node.TrusonaOutcomes.EXPIRED_OUTCOME;
import static com.trusona.forgerock.node.TrusonaOutcomes.REJECTED_OUTCOME;
import static com.trusona.sdk.resources.dto.TrusonaficationStatus.EXPIRED;
import static com.trusona.sdk.resources.dto.TrusonaficationStatus.IN_PROGRESS;
import static com.trusona.sdk.resources.dto.TrusonaficationStatus.REJECTED;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;

import com.sun.identity.shared.debug.Debug;
import com.trusona.forgerock.auth.TrusonaDebug;
import com.trusona.forgerock.auth.principal.PrincipalMapper;
import com.trusona.sdk.resources.TrusonaApi;
import com.trusona.sdk.resources.dto.TrusonaficationResult;
import com.trusona.sdk.resources.exception.TrusonaException;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.authentication.callbacks.PollingWaitCallback.PollingWaitCallbackBuilder;

public class WaitForState implements Supplier<Action> {

  private final static Debug debug = TrusonaDebug.getInstance();

  private final TrusonaApi trusona;
  private final PrincipalMapper principalMapper;
  private final UUID trusonaficationId;
  private final JsonValue currentState;

  public WaitForState(TrusonaApi trusona, PrincipalMapper principalMapper, UUID trusonaficationId, JsonValue currentState) {
    this.trusona = trusona;
    this.principalMapper = principalMapper;
    this.trusonaficationId = trusonaficationId;
    this.currentState = currentState;
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
      Optional<Action.ActionBuilder> action = principalMapper.mapPrincipal(result)
        .map(p -> Action.goTo(ACCEPTED_OUTCOME.id).replaceSharedState(stateWithUsername(p.getName())));

      if (!action.isPresent()) {
        debug.error("Unable to find a user with TrusonaficationResult => {}", result);
      }

      return action.orElseGet(() -> Action.goTo(ERROR_OUTCOME.id));

    }
    else if (result.getStatus().equals(REJECTED)) {
      return Action.goTo(REJECTED_OUTCOME.id);
    }
    else if (result.getStatus().equals(EXPIRED)) {
      return Action.goTo(EXPIRED_OUTCOME.id);
    }
    else if (result.getStatus().equals(IN_PROGRESS)) {
      PollingWaitCallbackBuilder builder = new PollingWaitCallbackBuilder().withWaitTime(WAIT_TIME);
      return Action.send(builder.build());
    }
    else {
      debug.error(String.format("Got an unexpected Trusonafication Result: %s", result));
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
    return String.format("WaitForState[trusonaficationId=%s]", trusonaficationId);
  }
}
