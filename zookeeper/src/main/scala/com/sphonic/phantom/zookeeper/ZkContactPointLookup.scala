package com.sphonic.phantom.zookeeper

import com.datastax.driver.core.Cluster
import com.sphonic.phantom.connector.KeySpaceBuilder
import com.twitter.finagle.exp.zookeeper.ZooKeeper
import com.twitter.finagle.exp.zookeeper.client.ZkClient
import com.twitter.conversions.time._
import com.twitter.util.Future
import com.twitter.util.Try
import com.twitter.util.Await
import com.twitter.util.Duration
import scala.collection.JavaConverters._
import java.net.InetSocketAddress

class ZkContactPointLookup (host: String, 
                            port: Int, 
                            path: String = "/cassandra", 
                            timeout: Duration = 2.seconds) extends (Cluster.Builder => Cluster.Builder) {
  
  
  def apply (builder: Cluster.Builder): Cluster.Builder = {
    val ports = Await.result(retrieveContactPoints, timeout)
    builder.addContactPointsWithPorts(ports.asJava)
  }
  
  
  protected lazy val zkClient: ZkClient = {
    val conn = s"$host:$port"
    val client = ZooKeeper.newRichClient(conn)
    Await.result(client.connect(), timeout)
    client
  }
  
  protected def retrieveContactPoints: Future[Seq[InetSocketAddress]] = 
    zkClient.getData(path, watch = false) map {
      res => Try {
        parseContactPoints(new String(res.data))
      } getOrElse Seq.empty[InetSocketAddress]
    }
  
  protected def parseContactPoints(data: String): Seq[InetSocketAddress] = {
    data.split("\\s*,\\s*").map(_.split(":")).map {
      case Array(hostname, port) => new InetSocketAddress(hostname, port.toInt)
    }.toSeq
  }
  
}

object ZkContactPointLookup {

    
  val defaultPort = 2181
  
  
  lazy val local = apply(defaultPort)

  def apply (port: Int): KeySpaceBuilder = apply("localhost", port)
  
  def apply (host: String, port: Int): KeySpaceBuilder = 
    new KeySpaceBuilder(new ZkContactPointLookup(host,port))
  
  def apply (host: String, port: Int, path: String, timeout: Duration): KeySpaceBuilder = 
    new KeySpaceBuilder(new ZkContactPointLookup(host,port,path,timeout))
  
}
