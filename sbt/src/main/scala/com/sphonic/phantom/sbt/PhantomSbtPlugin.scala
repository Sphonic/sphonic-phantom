package com.sphonic.phantom.sbt

import sbt._
import sbt.Keys._
import scala.concurrent.blocking
import scala.util.control.NonFatal
import org.cassandraunit.utils.EmbeddedCassandraServerHelper

object PhantomSbtPlugin extends Plugin {

  
  object PhantomKeys {
    
	val startEmbeddedCassandra = TaskKey[Unit]("startEmbeddedCassandra")
	
  }
  
  
  object PhantomPlugin {
    import PhantomKeys._
    
    val defaults: Seq[Setting[_]] = Seq(
	    startEmbeddedCassandra := EmbeddedCassandra.start(),
	    test in Test <<= (test in Test).dependsOn(startEmbeddedCassandra),
	    fork := true
	  )
  }
  
  
}

object EmbeddedCassandra {
  
  println("Initialize EmbeddedCassandra singleton.")
  
  // val logger = LoggerFactory.getLogger("com.websudos.phantom.testing") // TODO - enable logging
  
  private var started: Boolean = false
  
  def start (): Unit = {
    this.synchronized {
      if (!started) {
        blocking {
          try {
            // logger.info("Starting Cassandra in Embedded mode.")
            println("Starting Cassandra in Embedded mode.")
            EmbeddedCassandraServerHelper.mkdirs()
          } catch {
            case NonFatal(e) => {
              // logger.error(e.getMessage)
            }
          }
          EmbeddedCassandraServerHelper.startEmbeddedCassandra()
        }
        started = true
      }
    }
  }

  
}
