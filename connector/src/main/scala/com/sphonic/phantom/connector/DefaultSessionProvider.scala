package com.sphonic.phantom.connector

import com.datastax.driver.core.Session
import com.datastax.driver.core.Cluster
import scala.collection.concurrent.TrieMap

class DefaultSessionProvider (builder: ClusterBuilder) extends SessionProvider {

  
  private val sessionCache = new Cache[String,Session]
  
  
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
  
  protected def createSession (keySpace: String): Session = {
    // TODO - the connect method might throw exceptions, decide on error handling
    val session = cluster.connect
    initKeySpace(session, keySpace)
  }
  
  def getSession (keySpace: String): Session = {
    sessionCache.getOrElseUpdate(keySpace, createSession(keySpace))
  }
  
}

class Cache[K,V] {

  /* this implementation uses putIfAbsent from the underlying TrieMap as
   * getOrElseUpdate is not thread-safe. */
  
  private val map = TrieMap[K,Lazy]()
  
  private class Lazy (value: => V) {
    lazy val get: V = value
  }
  
  def getOrElseUpdate (key: K, op: => V): V = {
    val lazyOp = new Lazy(op)
    map.putIfAbsent(key, lazyOp) match {
      case Some(oldval) =>
        // don't evaluate the new lazyOp, return existing value
        oldval.get
      case _ =>
        // no existing value for key, evaluate lazyOp
        lazyOp.get
    }
  }

}
