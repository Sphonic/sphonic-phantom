package com.sphonic.phantom

import com.datastax.driver.core.Cluster

package object connector {

  type ClusterBuilder = (Cluster.Builder => Cluster.Builder)
  
}
