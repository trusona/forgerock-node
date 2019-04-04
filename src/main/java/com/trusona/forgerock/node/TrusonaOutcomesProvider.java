package com.trusona.forgerock.node;

import static com.trusona.forgerock.node.TrusonaOutcomes.ACCEPTED_OUTCOME;
import static com.trusona.forgerock.node.TrusonaOutcomes.ERROR_OUTCOME;
import static com.trusona.forgerock.node.TrusonaOutcomes.EXPIRED_OUTCOME;
import static com.trusona.forgerock.node.TrusonaOutcomes.REJECTED_OUTCOME;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.OutcomeProvider;
import org.forgerock.util.i18n.PreferredLocales;

public class TrusonaOutcomesProvider implements OutcomeProvider {

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