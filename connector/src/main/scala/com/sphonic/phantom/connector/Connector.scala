package com.sphonic.phantom.connector

import com.datastax.driver.core.Session

/* Trait to be mixed into the table classes that extend
 * phantom's CassandraTable.
 * 
 * The main purpose of this trait is to provide an implicit 
 * `Session` to the query and update logic in your table
 * class.
 * 
 * The most convenient approach is to mix in this abstract
 * base trait into your abstract table classes, so that
 * the implicit `Session` is available in your table 
 * implementation and then instantiate a sub-class
 * that mixes in the concrete trait from a `KeySpace`
 * instance.
 * 
 * {{{
 * // table class:
 * abstract class Foos extends CassandraTable[Foos, Foo] with Connector {
 *   [...]
 * }
 * 
 * // concrete instance:
 * val hosts = Seq("35.0.0.1", "35.0.0.2")
 * val keySpace = ContactPoints(hosts).keySpace("myApp")
 *   
 * object foos extends Foos with keySpace.Connector
 * }}}
 */
trait Connector {

  /**
   * The name of the keyspace this Connector should use.
   */
  def keySpace: String
  
  /**
   * The provider for the session instance.
   */
  def provider: SessionProvider
  
  /**
   * The implicit Session instance for the
   * query and update operations in phantom
   * table implementations.
   */
  implicit lazy val session: Session = provider.getSession(keySpace)
  
}
