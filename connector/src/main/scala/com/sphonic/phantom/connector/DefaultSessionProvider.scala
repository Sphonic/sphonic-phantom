package com.sphonic.phantom.connector

import com.datastax.driver.core.Session
import com.datastax.driver.core.Cluster

class DefaultSessionProvider (builder: ClusterBuilder) extends SessionProvider {

  
  lazy val cluster: Cluster = {
    // TODO - the original phantom modules had .withoutJMXReporting().withoutMetrics() as defaults, discuss best choices
    val cb = Cluster.builder
    builder(cb).build
  }
  
  def getSession (keySpace: String): Session = ???
    
  
}
