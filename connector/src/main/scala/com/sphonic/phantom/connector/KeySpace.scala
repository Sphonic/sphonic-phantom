package com.sphonic.phantom.connector

import com.datastax.driver.core.Session

class KeySpace (val name: String, val provider: SessionProvider) { outer =>

  lazy val session: Session = provider.getSession(name)
  
  trait Connector extends com.sphonic.phantom.connector.Connector {
    
    lazy val provider = outer.provider
    
    lazy val keySpace = outer.name
    
  }
  
}
