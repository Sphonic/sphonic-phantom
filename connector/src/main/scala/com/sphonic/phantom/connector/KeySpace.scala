package com.sphonic.phantom.connector

class KeySpace (val name: String, val provider: SessionProvider) { outer =>

  trait Connector extends com.sphonic.phantom.connector.Connector {
    
    lazy val provider = outer.provider
    
    lazy val keySpace = outer.name
    
  }
  
}
