package com.sphonic.phantom.connector

import com.datastax.driver.core.Session

/**
 * Represents a single Cassandra keySpace.
 * 
 * Provides access to the associated `Session` as well as to a 
 * `Connector` trait that can be mixed into `CassandraTable`
 * instances.
 * 
 * @param name the name of the keySpace
 * @param provider the provider for this keySpace
 */
class KeySpace (val name: String, val provider: SessionProvider) { outer =>

  /**
   * The Session associated with this keySpace.
   */
  lazy val session: Session = provider.getSession(name)
  
  /**
   * Trait that can be mixed into `CassandraTable`
   * instances.  
   */
  trait Connector extends com.sphonic.phantom.connector.Connector {
    
    lazy val provider = outer.provider
    
    lazy val keySpace = outer.name
    
  }
  
}
