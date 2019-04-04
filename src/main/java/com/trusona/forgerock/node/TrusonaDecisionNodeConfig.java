package com.trusona.forgerock.node;

import com.sun.identity.sm.RequiredValueValidator;
import com.trusona.forgerock.auth.authenticator.Trusonaficator.AuthenticationLevel;
import java.util.Set;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.sm.annotations.adapters.Password;

public interface TrusonaDecisionNodeConfig {

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