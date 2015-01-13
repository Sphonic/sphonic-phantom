package com.sphonic.phantom.connector

/**
 * Entry point for defining a keySpace based
 * on a single contact point (Cassandra node).
 * 
 * Using a single contact point only is usually
 * only recommended for testing purposes.
 */
object ContactPoint {

  /**
   * Cassandra's default ports.
   */
  object DefaultPorts {
    
    val live = 9042

    val embedded = 9142
    
  } 
  
  /**
   * A keyspace builder based on a single
   * contact point running on the default
   * port on localhost.
   */
  lazy val local = apply(DefaultPorts.live)

  /**
   * A keyspace builder based on a single
   * contact point running on the default
   * port of embedded Cassandra.
   */
  lazy val embedded = apply(DefaultPorts.embedded)
  
  /**
   * A keyspace builder based on a single
   * contact point running on the specified
   * port on localhost.
   */
  def apply (port: Int): KeySpaceBuilder = apply("localhost", port)
  
  /**
   * A keyspace builder based on a single
   * contact point running on the specified
   * host and port.
   */
  def apply (host: String, port: Int): KeySpaceBuilder = 
    new KeySpaceBuilder(_.addContactPoint(host).withPort(port))
  
}

/**
 * Entry point for defining a keySpace based
 * on multiple contact points (Cassandra nodes).
 * 
 * Even though the Cassandra driver technically only
 * needs a single contact point and will then fetch
 * the metadata for all other Cassandra nodes, it is
 * recommended to specify more than just one contact
 * point in case one node is down the moment the driver
 * initializes.
 * 
 * Since the driver finds additional nodes on its own,
 * the initial list of contact points only needs to be 
 * updated when you remove one of the specified contact
 * points, not when merely adding new nodes to the cluster.
 */
object ContactPoints {
  
  /**
   * A keyspace builder based on the specified
   * contact points, all running on the default port.
   */
  def apply (hosts: Seq[String]): KeySpaceBuilder = 
    new KeySpaceBuilder(_.addContactPoints(hosts:_*))
    
  /**
   * A keyspace builder based on the specified
   * contact points, all running on the specified port.
   */
  def apply (hosts: Seq[String], port: Int): KeySpaceBuilder = 
    new KeySpaceBuilder(_.addContactPoints(hosts:_*).withPort(port))
  
}
