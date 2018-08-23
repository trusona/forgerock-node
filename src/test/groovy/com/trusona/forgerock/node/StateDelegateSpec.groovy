package com.trusona.forgerock.node

import com.sun.identity.authentication.callbacks.HiddenValueCallback
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback
import com.trusona.client.TrusonaClient
import com.trusona.forgerock.auth.authenticator.Authenticator
import com.trusona.forgerock.auth.callback.CallbackFactory
import com.trusona.sdk.resources.TrusonaApi
import org.forgerock.json.JsonValue
import org.forgerock.openam.auth.node.api.ExternalRequestContext
import org.forgerock.openam.auth.node.api.TreeContext
import spock.lang.Specification

import static com.trusona.forgerock.auth.Constants.ERROR
import static com.trusona.forgerock.auth.Constants.PAYLOAD
import static com.trusona.forgerock.auth.Constants.TRUCODE_ID
import static com.trusona.forgerock.auth.Constants.TRUSONAFICATION_ID

class StateDelegateSpec extends Specification {

  StateDelegate sut

  def setup() {
    def callbackFactory = Mock(CallbackFactory)
    def authenticator = Mock(Authenticator)
    def trusona = Mock(TrusonaApi)
    def trusonaClient = Mock(TrusonaClient)
    def orgFromRealm = {r -> "organization"}

    sut = new StateDelegate(callbackFactory, authenticator, trusona, trusonaClient, Collections.emptySet(), orgFromRealm)
  }

  def "should send initial state for new request"() {
    given:
    def jsonValue = new JsonValue([:])
    def externalRequestContext = new ExternalRequestContext.Builder().build()

    def treeContext = new TreeContext(jsonValue, externalRequestContext, [])

    when:
    def state = sut.getState(treeContext)

    then:
    state instanceof InitialState
  }

  def "should send trucode state after initial state"() {
    given:
    def uuid = UUID.randomUUID()
    def jsonValue = new JsonValue([:])
    def externalRequestContext = new ExternalRequestContext.Builder().build()


    def callbackList = [new ScriptTextOutputCallback("callback"),
                        new HiddenValueCallback(TRUCODE_ID, uuid.toString()),
                        new HiddenValueCallback(ERROR),
                        new HiddenValueCallback(PAYLOAD),
                        new HiddenValueCallback(TRUSONAFICATION_ID)]
    def treeContext = new TreeContext(jsonValue, externalRequestContext, callbackList)

    when:
    def state = sut.getState(treeContext)

    then:
    state instanceof TrucodeState
  }

  def "should send WaitForState when we have a trusonafication id"() {
    given:
    def trusonaficationId = UUID.randomUUID()
    def jsonValue = new JsonValue([ trusonaficationId: trusonaficationId.toString() ])
    def externalRequestContext = new ExternalRequestContext.Builder().build()

    def treeContext = new TreeContext(jsonValue, externalRequestContext, [])

    when:
    def state = sut.getState(treeContext)

    then:
    state instanceof WaitForState
  }

  def "should send ErrorState when we get a client side error"() {
    given:
    def uuid = UUID.randomUUID()
    def jsonValue = new JsonValue([:])
    def externalRequestContext = new ExternalRequestContext.Builder().build()


    def callbackList = [new ScriptTextOutputCallback("callback"),
                        new HiddenValueCallback(TRUCODE_ID, uuid.toString()),
                        new HiddenValueCallback(ERROR, "some error"),
                        new HiddenValueCallback(PAYLOAD),
                        new HiddenValueCallback(TRUSONAFICATION_ID)]
    def treeContext = new TreeContext(jsonValue, externalRequestContext, callbackList)

    when:
    def state = sut.getState(treeContext)

    then:
    state instanceof ErrorState
  }

  def "should send ErrorState when the trucode_id is not a UUID"() {
    given:
    def uuid = UUID.randomUUID()
    def jsonValue = new JsonValue([:])
    def externalRequestContext = new ExternalRequestContext.Builder().build()


    def callbackList = [new ScriptTextOutputCallback("callback"),
                        new HiddenValueCallback(TRUCODE_ID, "not a uuid"),
                        new HiddenValueCallback(ERROR),
                        new HiddenValueCallback(PAYLOAD),
                        new HiddenValueCallback(TRUSONAFICATION_ID)]
    def treeContext = new TreeContext(jsonValue, externalRequestContext, callbackList)

    when:
    def state = sut.getState(treeContext)

    then:
    state instanceof ErrorState
  }

  def "should send ErrorState when the trusonafication_id is not a UUID"() {
    given:
    def jsonValue = new JsonValue([ trusonaficationId: "notauuid" ])
    def externalRequestContext = new ExternalRequestContext.Builder().build()

    def treeContext = new TreeContext(jsonValue, externalRequestContext, [])

    when:
    def state = sut.getState(treeContext)

    then:
    state instanceof ErrorState
  }

  def "should send ErrorSTate when we get nonsensical callbacks"() {
    given:
    def uuid = UUID.randomUUID()
    def jsonValue = new JsonValue([:])
    def externalRequestContext = new ExternalRequestContext.Builder().build()


    def callbackList = [new ScriptTextOutputCallback("callback"),
                        new HiddenValueCallback("trucode_id223", uuid.toString()),
                        new HiddenValueCallback("fizzbuzz"),
                        new HiddenValueCallback("foobar"),
                        new HiddenValueCallback("something like a trusonafication")]
    def treeContext = new TreeContext(jsonValue, externalRequestContext, callbackList)

    when:
    def state = sut.getState(treeContext)

    then:
    state instanceof ErrorState
  }
}