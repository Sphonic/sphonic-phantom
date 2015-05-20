/*
 * Copyright 2014-2015 Sphonic Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sphonic.phantom.connector

import com.datastax.driver.core.Session

/**
 * Represents a single Cassandra keySpace.
 * 
 * Provides access to the associated `Session` as well as to a 
 * `Connector` trait that can be mixed into `CassandraTable`
 * instances.
 * 
 * @param name the name of the keySpace
 * @param provider the provider for this keySpace
 */
class KeySpace (val name: String, val provider: SessionProvider) { outer =>

  /**
   * The Session associated with this keySpace.
   */
  lazy val session: Session = provider.getSession(name)
  
  /**
   * Trait that can be mixed into `CassandraTable`
   * instances.  
   */
  trait Connector extends com.sphonic.phantom.connector.Connector {
    
    lazy val provider = outer.provider
    
    lazy val keySpace = outer.name
    
  }
  
}
