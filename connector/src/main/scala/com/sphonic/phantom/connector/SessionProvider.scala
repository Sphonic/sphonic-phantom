package com.sphonic.phantom.connector

import com.datastax.driver.core.Cluster
import com.datastax.driver.core.Session

trait SessionProvider {

  def cluster: Cluster
  
  def getSession (keySpace: String): Session
  
}
