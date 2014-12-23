package com.sphonic.phantom.connector

import com.datastax.driver.core.Session
import com.datastax.driver.core.Cluster

class DefaultSessionProvider (builder: ClusterBuilder) extends SessionProvider {

  
  def cluster: Cluster = ???
  
  def getSession (keySpace: String): Session = ???
    
  
}
