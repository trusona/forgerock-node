package com.trusona.forgerock.node

import com.sun.identity.shared.debug.Debug
import spock.lang.Specification

class ErrorStateSpec extends Specification {

  ErrorState sut

  def "should log the error and go to the ERROR outcome"() {
    given:
    def error = "Something went wrong!"
    def mockDebug = Mock(Debug)

    sut = new ErrorState(error, mockDebug)

    when:
    def res = sut.get()

    then:
    ! mockDebug.error(error)
    ! res.sendingCallbacks()
    res.outcome == TrusonaOutcomes.ERROR_OUTCOME.id
  }
}
