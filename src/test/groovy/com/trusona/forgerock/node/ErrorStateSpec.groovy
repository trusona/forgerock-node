package com.trusona.forgerock.node

import com.sun.identity.shared.debug.Debug
import spock.lang.Specification

class ErrorStateSpec extends Specification {

  ErrorState sut

  def "should go to the ERROR outcome when in error"() {
    given:
    def error = "Something went wrong!"

    sut = new ErrorState(error)

    when:
    def res = sut.get()

    then:
    ! res.sendingCallbacks()
    res.outcome == TrusonaOutcomes.ERROR_OUTCOME.id
  }
}