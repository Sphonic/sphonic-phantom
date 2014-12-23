package com.sphonic.phantom.connector

import com.datastax.driver.core.Session

trait Connector {

  def keySpace: String
  
  def provider: SessionProvider
  
  implicit lazy val session: Session = provider.getSession(keySpace)
  
}
