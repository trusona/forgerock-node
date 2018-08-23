package com.trusona.forgerock.node

import com.sun.identity.authentication.callbacks.HiddenValueCallback
import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback
import com.trusona.forgerock.auth.callback.CallbackFactory
import spock.lang.Specification

import static com.trusona.forgerock.auth.Constants.ERROR
import static com.trusona.forgerock.auth.Constants.PAYLOAD
import static com.trusona.forgerock.auth.Constants.TRUCODE_ID
import static com.trusona.forgerock.auth.Constants.TRUSONAFICATION_ID

class InitialStateSpec extends Specification {

  CallbackFactory callbackFactory = Mock(CallbackFactory)

  InitialState sut = new InitialState(callbackFactory)

  def "get() should send our initial callbacks"() {
    given:
    callbackFactory.makeScriptCallback(_ as String) >> { args -> new ScriptTextOutputCallback((String) args[0]) }

    when:
    def res = sut.get()

    then:
    res.sendingCallbacks()
    res.callbacks.findIndexOf { cb -> cb instanceof ScriptTextOutputCallback && cb.message == "app.run();" } == 0
    res.callbacks.findIndexOf { cb -> cb instanceof HiddenValueCallback && cb.id == TRUCODE_ID } == 1
    res.callbacks.findIndexOf { cb -> cb instanceof HiddenValueCallback && cb.id == ERROR } == 2
    res.callbacks.findIndexOf { cb -> cb instanceof HiddenValueCallback && cb.id == PAYLOAD } == 3
    res.callbacks.findIndexOf { cb -> cb instanceof HiddenValueCallback && cb.id == TRUSONAFICATION_ID } == 4
  }
}
