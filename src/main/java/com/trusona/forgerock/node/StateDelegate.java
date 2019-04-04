package com.trusona.forgerock.node;

import static com.trusona.forgerock.auth.Constants.ERROR;
import static com.trusona.forgerock.auth.Constants.PAYLOAD;
import static com.trusona.forgerock.auth.Constants.TRUCODE_ID;
import static com.trusona.forgerock.auth.Constants.TRUSONAFICATION_ID;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;

import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.sun.identity.shared.debug.Debug;
import com.trusona.client.TrusonaClient;
import com.trusona.forgerock.auth.TrusonaDebug;
import com.trusona.forgerock.auth.authenticator.Authenticator;
import com.trusona.forgerock.auth.callback.CallbackFactory;
import com.trusona.forgerock.auth.principal.DefaultPrincipalMapper;
import com.trusona.forgerock.auth.principal.IdentityFinder;
import com.trusona.forgerock.auth.principal.PrincipalMapper;
import com.trusona.sdk.resources.TrusonaApi;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.security.auth.callback.Callback;
import org.apache.commons.lang3.StringUtils;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.TreeContext;

public class StateDelegate {

  private final static Debug debug = TrusonaDebug.getInstance();

  private final CallbackFactory callbackFactory;
  private final Authenticator authenticator;
  private final TrusonaApi trusona;
  private final TrusonaClient trusonaClient;
  private final Set<String> userAliases;
  private final Function<String, String> orgFromRealm;

  public StateDelegate(CallbackFactory callbackFactory, Authenticator authenticator,
                       TrusonaApi trusona, TrusonaClient trusonaClient,
                       Set<String> userAliases, Function<String, String> orgFromRealm) {
    this.callbackFactory = callbackFactory;
    this.authenticator = authenticator;
    this.trusona = trusona;
    this.trusonaClient = trusonaClient;
    this.userAliases = userAliases;
    this.orgFromRealm = orgFromRealm;
  }

  public Supplier<Action> getState(TreeContext treeContext) {
    debug.message("in getState() with {} callbacks", treeContext.getAllCallbacks().size());
    debug.message("sharedState => {}", treeContext.sharedState);

    if (treeContext.sharedState.isDefined(TRUSONAFICATION_ID)) {
      return parseUUID(treeContext.sharedState.get(TRUSONAFICATION_ID).asString())
        .map(t -> waitForStateFromTrusonaficationId(treeContext, t))
        .orElseGet(() -> new ErrorState("A trusonafication ID was saved in the session state, but it is not a valid UUID"));
    }

    Supplier<Action> state = new ErrorState("We received unexpected input. Please try again.");

    List<? extends Callback> callbackList = treeContext.getAllCallbacks();

    if (callbackList.isEmpty()) {
      state = new InitialState(callbackFactory);
    }
    else if (callbackList.size() == 5) {
      Optional<String> errorCallback = getHiddenValueCallback(treeContext, ERROR)
        .filter(StringUtils::isNotBlank);

      if (errorCallback.isPresent()) {
        return new ErrorState(errorCallback.get());
      }

      String payload = getHiddenValueCallback(treeContext, PAYLOAD)
        .orElse(null);

      Optional<UUID> trucodeId = getHiddenValueCallback(treeContext, TRUCODE_ID)
        .flatMap(this::parseUUID);

      Optional<String> trusonaficationId = getHiddenValueCallback(treeContext, TRUSONAFICATION_ID);

      debug.message("trucode_id => {}", trucodeId.map(UUID::toString).orElse("EMPTY"));
      debug.message("error => {}", errorCallback.orElse("EMPTY"));
      debug.message("payload => {}", Optional.ofNullable(payload).orElse("EMPTY"));
      debug.message("trusonafication_id => {}", getHiddenValueCallback(treeContext, TRUSONAFICATION_ID).orElse("EMPTY"));

      if (trucodeId.isPresent()) {
        state = new TrucodeState(authenticator, callbackFactory, treeContext.sharedState, trucodeId.get(), payload);
      }

      if (trusonaficationId.isPresent()) {
        debug.message("Trusonaficationid is present, trying to move to wait state");

        state = parseUUID(trusonaficationId.get())
          .map(t -> waitForStateFromTrusonaficationId(treeContext, t))
          .orElseGet(() -> new ErrorState("The trusonafication ID is not a UUID"));
      }
    }

    debug.message("Dropping out of getState with state {}", state);
    return state;
  }

  private Supplier<Action> waitForStateFromTrusonaficationId(TreeContext treeContext, UUID trusonaficationId) {
    String realm = treeContext.sharedState.get(REALM).asString();
    IdentityFinder identityFinder = new IdentityFinder(userAliases, realm);
    PrincipalMapper principalMapper = new DefaultPrincipalMapper(trusonaClient, identityFinder);

    return new WaitForState(trusona, principalMapper, trusonaficationId, treeContext.sharedState);
  }

  private Optional<String> getHiddenValueCallback(TreeContext treeContext, String id) {
    return treeContext.getAllCallbacks().stream()
      .filter(cb -> cb instanceof HiddenValueCallback)
      .map(cb -> (HiddenValueCallback) cb)
      .filter(cb -> hasId(cb, id))
      .filter(this::valueIsNotId) //Bug in ForgeRock that sets value == id when there is no value
      .map(cb -> Optional.ofNullable(cb.getValue()))
      .flatMap(o -> o.map(Stream::of).orElse(Stream.empty()))
      .findFirst();
  }

  private Optional<UUID> parseUUID(String s) {
    try {
      return Optional.of(UUID.fromString(s));
    }
    catch (IllegalArgumentException e) {
      debug.error("Error parsing UUID", e);
      return Optional.empty();
    }
  }

  private boolean hasId(HiddenValueCallback cb, String id) {
    boolean result = cb.getId().equals(id);
    debug.message("HiddenValueCallback[id={}, value={}] hasId => {}", cb.getId(), cb.getValue(), result);
    return result;
  }

  private boolean valueIsNotId(HiddenValueCallback cb) {
    boolean result = !cb.getId().equals(cb.getValue());
    debug.message("HiddenValueCallback[id={}, value={}] valueIsNotId => {}", cb.getId(), cb.getValue(), result);
    return result;
  }
}