package com.trusona.forgerock.node;

import com.sun.identity.shared.debug.Debug;
import com.trusona.forgerock.auth.TrusonaDebug;
import org.apache.commons.lang3.StringUtils;
import org.forgerock.openam.auth.node.api.Action;

import java.util.function.Supplier;

import static com.trusona.forgerock.node.TrusonaOutcomes.ERROR_OUTCOME;

public class ErrorState implements Supplier<Action> {
  private final String error;
  private final Debug debug;


  public ErrorState(String error, Debug debug) {
    this.error = error;
    this.debug = debug;
  }

  public ErrorState(String error) {
    this(error, TrusonaDebug.getInstance());
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
    return "ErrorState[" + "error=\"" + error + "\"]";
  }
}
