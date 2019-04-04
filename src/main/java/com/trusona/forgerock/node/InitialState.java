package com.trusona.forgerock.node;

import static com.trusona.forgerock.auth.Constants.ERROR;
import static com.trusona.forgerock.auth.Constants.PAYLOAD;
import static com.trusona.forgerock.auth.Constants.TRUCODE_ID;
import static com.trusona.forgerock.auth.Constants.TRUSONAFICATION_ID;

import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.trusona.forgerock.auth.TrusonaDebug;
import com.trusona.forgerock.auth.callback.CallbackFactory;
import java.util.function.Supplier;
import org.forgerock.openam.auth.node.api.Action;

public class InitialState implements Supplier<Action> {

  private final CallbackFactory callbackFactory;

  public InitialState(CallbackFactory callbackFactory) {
    this.callbackFactory = callbackFactory;
  }

  @Override
  public Action get() {
    TrusonaDebug.getInstance().message("In InitialState");

    return Action.send(
      callbackFactory.makeScriptCallback("app.run();"),
      new HiddenValueCallback(TRUCODE_ID),
      new HiddenValueCallback(ERROR),
      new HiddenValueCallback(PAYLOAD),
      new HiddenValueCallback(TRUSONAFICATION_ID)
    ).build();
  }

  @Override
  public String toString() {
    return "InitialState[]";
  }
}