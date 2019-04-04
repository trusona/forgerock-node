package com.trusona.forgerock.node

import org.forgerock.openam.sm.AnnotatedServiceRegistry
import spock.lang.Specification

class TrusonaDecisionNodePluginSpec extends Specification {

  TrusonaDecisionNodePlugin nodePlugin

  def setup() {
    nodePlugin = new TrusonaDecisionNodePlugin(Mock(AnnotatedServiceRegistry))
  }

  def "plugin version will not be unspecified"() {
    expect:
    nodePlugin.pluginVersion != "unspecified"
  }

  def "plugin version will not be default value"() {
    expect:
    nodePlugin.pluginVersion != "@project_version@"
  }

  def "plugin version will be in normal semver form"() {
    expect:
    nodePlugin.pluginVersion.split("\\.").length == 3
  }
}