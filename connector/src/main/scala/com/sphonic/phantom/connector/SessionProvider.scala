package com.sphonic.phantom.connector

import com.datastax.driver.core.Cluster
import com.datastax.driver.core.Session

/**
 * Responsible for providing Session instances of the
 * Cassandra driver for multiple keySpaces defined
 * in the same cluster. 
 */
trait SessionProvider {

  /**
   * The Cassandra driver's Cluster instance
   * used by this provider to create new
   * Session instances.
   */
  def cluster: Cluster
  
  /**
   * Returns a Session instance for the keySpace
   * with the specified name.
   * 
   * It is recommended that implementations
   * cache instances per keySpace, so that they
   * can hand out existing instances in case
   * a client asks for the same keySpace multiple
   * times.
   */
  def getSession (keySpace: String): Session
  
}
