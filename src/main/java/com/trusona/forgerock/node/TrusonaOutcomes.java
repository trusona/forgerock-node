package com.trusona.forgerock.node;

import com.trusona.sdk.resources.dto.TrusonaficationStatus;
import org.forgerock.openam.auth.node.api.OutcomeProvider.Outcome;

public class TrusonaOutcomes {
  //TODO: Remove these classes and stick with just strings, since that is what the API uses.
  public static final Outcome ACCEPTED_OUTCOME = new Outcome(TrusonaficationStatus.ACCEPTED.name(), "Accepted");
  public static final Outcome REJECTED_OUTCOME = new Outcome(TrusonaficationStatus.REJECTED.name(), "Rejected");
  public static final Outcome EXPIRED_OUTCOME = new Outcome(TrusonaficationStatus.EXPIRED.name(), "Expired");
  public static final Outcome ERROR_OUTCOME = new Outcome("ERROR", "Error");
}
