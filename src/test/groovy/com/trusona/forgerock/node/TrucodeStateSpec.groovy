package com.trusona.forgerock.node

import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback
import com.sun.identity.authentication.spi.AuthLoginException
import com.sun.identity.authentication.spi.RedirectCallback
import com.trusona.forgerock.auth.authenticator.Authenticator
import com.trusona.forgerock.auth.callback.CallbackFactory
import com.trusona.forgerock.auth.callback.TrucodeIdCallback
import org.forgerock.json.JsonValue
import org.forgerock.openam.authentication.callbacks.PollingWaitCallback
import spock.lang.Specification

import static com.trusona.forgerock.auth.Constants.TRUSONAFICATION_ID
import static com.trusona.forgerock.node.TrusonaOutcomes.ERROR_OUTCOME

class TrucodeStateSpec extends Specification {

  Authenticator authenticator
  CallbackFactory callbackFactory
  UUID trucodeId
  TrucodeState sut

  def setup() {
    authenticator = Mock(Authenticator)
    callbackFactory = Mock(CallbackFactory)
    trucodeId = UUID.randomUUID()
    sut = new TrucodeState(authenticator, callbackFactory, new JsonValue([:]), trucodeId, null)
  }

  def "goTo error state when authenticator throws an exception"() {
    given:
    authenticator.createTrusonafication(_) >> { a -> throw new AuthLoginException("something went wrong") }

    when:
    def res = sut.get()

    then:
    res.outcome == ERROR_OUTCOME.id
  }

  def "send a PollingWaitCallback when there is no payload"() {
    given:
    sut = new TrucodeState(authenticator, callbackFactory, new JsonValue([:]), trucodeId, payload)
    authenticator.createTrusonafication(_ as TrucodeIdCallback) >> UUID.randomUUID()

    when:
    def res = sut.get()

    then:
    res.sendingCallbacks()
    res.callbacks[0] instanceof PollingWaitCallback

    where:
    payload << [ null, "", "   " ]
  }

  def "saves the trusonafication ID in the shared state"() {
    given:
    def trusonaficationId = UUID.randomUUID()
    authenticator.createTrusonafication(_) >> trusonaficationId

    when:
    def res = sut.get()

    then:
    res.sharedState.get(TRUSONAFICATION_ID).asString() == trusonaficationId.toString()
  }

  def "sends a ScriptTextCallback and a RedirectCallback when a payload is present"() {
    given:
    def trusonaficationId = UUID.randomUUID()
    sut = new TrucodeState(authenticator, callbackFactory, new JsonValue([:]), trucodeId, "apayload")

    authenticator.createTrusonafication(_ as TrucodeIdCallback) >> trusonaficationId
    callbackFactory.makeScriptCallback("app.saveTrusonaficationCookie('${trusonaficationId.toString()}');") >>
      new ScriptTextOutputCallback("do stuff")
    callbackFactory.makeRedirectCallback("apayload") >> new RedirectCallback()

    when:
    def res = sut.get()

    then:
    res.sendingCallbacks()
    res.callbacks[0] instanceof ScriptTextOutputCallback
    res.callbacks[1] instanceof RedirectCallback

  }
}
