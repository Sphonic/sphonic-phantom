sphonic-phantom
===============

Replacements for the official phantom-zookeeper and phantom-test artifacts



Motivation
----------

This project aims to offer replacements for the official 
`phantom-zookeeper` and `phantom-test` artifacts. The problems
with the design and implementation of these modules are severe
enough to warrant a complete rewrite. The replacements aim to
solve the following issues with the original modules:

* Make it easier to externalize configuration
* Make it easier to use different configurations in test and production environments
* Avoid fragile magic that has proven to not work reliably in the old modules
* Avoid unnecessary dependencies (e.g. the old modules always pull in Zookeeper dependencies,
  no matter if you actually want to use it)
* Avoid code quality issues of the original modules, namely:
    * Messy naming and packaging
    * Unnecessary mutability
    * Both too much and too little separation of concerns in various parts of the old modules
    * Broken lifecycle management (e.g. in broken logic to attempt to recreate a closed Cluster instance)
    * Broken contracts (e.g. each Connector trait allows to define a keySpace, but only one of them will "win")
    * Dead code
       


The Basics
----------

Every well-behaved application stores a bunch of Foos and Bars. With the new connector
module you define tables the same way as before:

    case class Foo (id: UUID, value: String)

    abstract class Foos extends CassandraTable[Foos, Foo] with Connector {
  
      object id extends UUIDColumn(this) with PartitionKey[UUID]
      object value extends StringColumn(this)

      def fromRow(row: Row): Foo = Foo(id(row), value(row))
  
      override def store(foo: Foo): Future[Unit] = {
        insert
          .value(_.id, foo.id)
          .value(_.value, foo.value)
          .consistencyLevel_=(ConsistencyLevel.LOCAL_QUORUM)
          .execute() map (_ => ())
      }

    }
    
The class mixes in the abstract `Connector` trait which provides an implicit `Session`
to the operations you define in this class. Let's assume the Bars table class looks
almost identical.

If you want to use these two tables in your application, you can simply
mix in a fully configured `Connector` trait like this:

    val hosts: Seq[String] = Seq("35.0.0.1", "35.0.0.2")
    val keySpace = ContactPoints(hosts).keySpace("myApp")
    
    val foos = new Foos with keySpace.Connector
    val bars = new Bars with keySpace.Connector
    
Creating the traits dynamically allows for more flexibility, in particular
when it is desired to externalize the configuration or instantiate the tables
with different connectors for test and production environments, as demonstrated
in the following sections.



Externalizing Configuration
---------------------------

In most applications you want to externalize the configuration which
is specific to the environment, like the contact points shown in the previous
section.

To make this more convenient, you can wrap the table creation in a container
class that expects the fully configured `KeySpace` as a parameter:

    class MyTables (keySpace: KeySpace) {
    
      val foos = new Foos with keySpace.Connector
      val bars = new Bars with keySpace.Connector
      
    }
    
And then use it like this:

    val hosts: Seq[String] = Seq("35.0.0.1", "35.0.0.2")
    val keySpace = ContactPoints(hosts).keySpace("myApp")
    
    val tables = new MyTables(keySpace)
    
    tables.foos.store(Foo(UUID.randomUUID, "value"))
            


Configuring the Driver
----------------------

The previous examples only showed how to define the initial contact points
for the driver. The API offers additional hooks for configuring
a keySpace:

    // Not using the default port:
    val hosts: Seq[String] = Seq("35.0.0.1", "35.0.0.2")
    val port = 9099
    val keySpace = ContactPoints(hosts, port).keySpace("myApp")
    
    // Embedded Cassandra for testing
    val keySpace = ContactPoint.embedded.keySpace("myApp-test")

Additionally the API exposes the original `Cluster.Builder` API from
the Java driver for further configuration:

    val hosts: Seq[String] = Seq("35.0.0.1", "35.0.0.2")
    val keySpace = ContactPoints(hosts).withClusterBuilder(
      _.withRetryPolicy(DowngradingConsistencyRetryPolicy.INSTANCE)
       .withReconnectionPolicy(new ConstantReconnectionPolicy(100L))
    ).keySpace("myApp")

It does not wrap any Scala-sugar around this part of the API as the gain
would be minimal in this case.



Using Multiple KeySpaces
------------------------

In the rare event of combining multiple keySpaces in your application,
you just need to prepare the table container class to expect two different
`KeySpace` instances:

    class MyTables (fooSpace: KeySpace, barSpace: KeySpace) {
    
      val foos = new Foos with fooSpace.Connector
      val bars = new Bars with barSpace.Connector
      
    }

And then use this container like this:

    val hosts: Seq[String] = Seq("35.0.0.1", "35.0.0.2")
    val builder = ContactPoints(hosts)
    val fooSpace = builder.keySpace("myFoos")
    val barSpace = builder.keySpace("myBars")
    
    val tables = new MyTables(fooSpace, barSpace)
    
Just make sure that you create all keySpaces from the same builder,
to let them share the underlying Cassandra `Cluster` instance.



