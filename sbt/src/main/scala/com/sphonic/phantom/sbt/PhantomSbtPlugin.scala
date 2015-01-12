package com.sphonic.phantom.sbt

import sbt._
import sbt.Keys._
import scala.concurrent.blocking
import scala.util.control.NonFatal
import org.cassandraunit.utils.EmbeddedCassandraServerHelper

object PhantomSbtPlugin extends Plugin {

  
  object PhantomKeys {
    
	  val startEmbeddedCassandra = TaskKey[Unit]("Starts embedded Cassandra")
	  
	  val cassandraConfig = SettingKey[Option[File]]("YAML file for Cassandra configuration")
	
  }
  
  
  object PhantomPlugin {
    import PhantomKeys._
    
    val defaults: Seq[Setting[_]] = Seq(
        
      cassandraConfig := None,
      
	    startEmbeddedCassandra := EmbeddedCassandra.start(cassandraConfig.value),
	    
	    test in Test <<= (test in Test).dependsOn(startEmbeddedCassandra),
	    
	    fork := true
	  )
  }
  
  
}

object EmbeddedCassandra {
  
  println("Initialize EmbeddedCassandra singleton.")
  
  private var started: Boolean = false
  
  def start (config: Option[File]): Unit = {
    this.synchronized {
      if (!started) {
        blocking {
          try {
            EmbeddedCassandraServerHelper.mkdirs()
          } catch {
            case NonFatal(e) => {
              // logger.error(e.getMessage)
            }
          }
          config match {
              case Some(file) =>
                println("Starting Cassandra in embedded mode with configuration from $file.")
                EmbeddedCassandraServerHelper.startEmbeddedCassandra(file, 
                    EmbeddedCassandraServerHelper.DEFAULT_TMP_DIR, EmbeddedCassandraServerHelper.DEFAULT_STARTUP_TIMEOUT)
              case None =>
                println("Starting Cassandra in embedded mode with default configuration.")
                EmbeddedCassandraServerHelper.startEmbeddedCassandra()
            }
        }
        started = true
      }
    }
  }

  
}
