package com.sphonic.phantom.connector

object ContactPoint {

  object DefaultPorts {
    
    val live = 9042

    val embedded = 9142
    
  } 
  
  
  lazy val local = apply(DefaultPorts.live)

  lazy val embedded = apply(DefaultPorts.embedded)
  
  def apply (port: Int): KeySpaceBuilder = apply("localhost", port)
  
  def apply (host: String, port: Int): KeySpaceBuilder = 
    new KeySpaceBuilder(_.addContactPoint(host).withPort(port))
  
}

object ContactPoints {
  
  def apply (hosts: Seq[String]): KeySpaceBuilder = 
    new KeySpaceBuilder(_.addContactPoints(hosts:_*))
    
  def apply (hosts: Seq[String], port: Int): KeySpaceBuilder = 
    new KeySpaceBuilder(_.addContactPoints(hosts:_*).withPort(port))
  
}
