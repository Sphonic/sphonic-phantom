package com.sphonic.phantom.connector

/**
 * A builder for KeySpace instances.
 * 
 * When using multiple keySpaces in the same Cassandra cluster,
 * it is recommended to create all `KeySpace` instances from the
 * same builder instance.
 */
class KeySpaceBuilder (clusterBuilder: ClusterBuilder) {

  /**
   * Specify an additional builder to be applied when creating the Cluster instance.
   * This hook exposes the underlying Java API of the builder API of the Cassandra
   * driver.
   */
  def withClusterBuilder (builder: ClusterBuilder): KeySpaceBuilder = 
    new KeySpaceBuilder(clusterBuilder andThen builder)
  
  private lazy val sessionProvider = new DefaultSessionProvider(clusterBuilder)
  
  /**
   * Create a new keySpace with the specified name.
   */
  def keySpace (name: String): KeySpace = 
    new KeySpace(name, sessionProvider)
  
}
