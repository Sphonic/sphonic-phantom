package com.sphonic.phantom.connector

import com.datastax.driver.core.Session
import com.datastax.driver.core.Cluster

class DefaultSessionProvider (builder: ClusterBuilder) extends SessionProvider {

  
  lazy val cluster: Cluster = {
    // TODO - the original phantom modules had .withoutJMXReporting().withoutMetrics() as defaults, discuss best choices
    val cb = Cluster.builder
    builder(cb).build
  }
  
  protected def initKeySpace (session: Session, keySpace: String): Session = {
    // TODO - verify replication settings make sense
    session.execute(s"CREATE KEYSPACE IF NOT EXISTS $keySpace WITH replication = {'class': 'SimpleStrategy', 'replication_factor' : 1};")
    session.execute(s"USE $keySpace;")
    session
  }
  
  def getSession (keySpace: String): Session = {
    // TODO - the connect method might throw exceptions, decide on error handling
    // TODO - cache sessions per keySpace
    val session = cluster.connect
    initKeySpace(session, keySpace)
  }
    
  
}
