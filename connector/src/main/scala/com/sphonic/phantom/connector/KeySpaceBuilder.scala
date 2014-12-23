package com.sphonic.phantom.connector

class KeySpaceBuilder (clusterBuilder: ClusterBuilder) {

  def withClusterBuilder (builder: ClusterBuilder): KeySpaceBuilder = 
    new KeySpaceBuilder(clusterBuilder andThen builder)
  
  private lazy val sessionProvider = new DefaultSessionProvider(clusterBuilder)
  
  def keySpace (name: String): KeySpace = 
    new KeySpace(name, sessionProvider)
  
}
