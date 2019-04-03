package com.trusona.forgerock.node;

import static com.trusona.forgerock.node.TrusonaOutcomes.ERROR_OUTCOME;

import com.sun.identity.shared.debug.Debug;
import com.trusona.forgerock.auth.TrusonaDebug;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.forgerock.openam.auth.node.api.Action;

public class ErrorState implements Supplier<Action> {
  private final static Debug debug = TrusonaDebug.getInstance();

  private final String error;

  public ErrorState(String error) {
    this.error = error;
  }

  @Override
  public Action get() {
    debug.message("In ErrorState");

    if (StringUtils.isNotBlank(error)) {
      debug.error(error);
      return Action.goTo(ERROR_OUTCOME.id).build();
    }
    return null;
  }

  @Override
  public String toString() {
    return String.format("ErrorState[error=\"%s\"]", error);
  }
}