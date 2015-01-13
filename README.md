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
* Make it easier to configure the underlying Cassandra driver
* Avoid fragile magic that has proven to not work reliably in the old modules (e.g. the
  attempts to detect a running Cassandra)
* Avoid unnecessary dependencies (e.g. the old modules always pull in Zookeeper dependencies,
  no matter if you actually want to use it)
* Avoid code quality issues of the original modules, namely:
    * Messy naming and packaging
    * Unnecessary mutability
    * Broken lifecycle management (e.g. in broken logic to attempt to recreate a closed Cluster instance)
    * Broken contracts (e.g. each Connector trait allows to define a keySpace, but only one of them will "win")
    * Both too much and too little separation of concerns in various parts of the old modules
      (e.g. too much in extensive delegation between multiple trait hierarchies that primarily
      deal with simply providing a Session, too little in entangling Zookeeper lookup logic with
      general Cluster creation) 
    * Dead code
       


The Basics
----------

Every well-behaved application stores a bunch of Foos and Bars. With the new connector
module you define tables the same way as before:

```scala
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
```    
    
The class mixes in the abstract `Connector` trait which provides an implicit `Session`
to the operations you define in this class. Let's assume the Bars table class looks
almost identical.

If you want to use these two tables in your application, you can simply
mix in a fully configured `Connector` trait like this:

```scala
val hosts = Seq("35.0.0.1", "35.0.0.2")
val keySpace = ContactPoints(hosts).keySpace("myApp")
    
object foos extends Foos with keySpace.Connector
object bars extends Bars with keySpace.Connector
```
    
Creating the traits dynamically allows for more flexibility, in particular
when it is desired to externalize the configuration or instantiate the tables
with different connectors for test and production environments, as demonstrated
in the following sections.

Note that the otherwise equally valid syntax:

```
val foos = new Foos with keySpace.Connector
val bars = new Bars with keySpace.Connector
```

does not work due to the way how the `CassandraTable` implementation determines
the table name by reflection.



Externalizing Configuration
---------------------------

In most applications you want to externalize the configuration which
is specific to the environment, like the contact points shown in the previous
section.

To make this more convenient, you can wrap the table creation in a container
class that expects the fully configured `KeySpace` as a parameter:

```scala
class MyTables (keySpace: KeySpace) {
    
  object foos extends Foos with keySpace.Connector
  object bars extends Bars with keySpace.Connector
      
}
```
    
And then use it like this:

```scala
val hosts = Seq("35.0.0.1", "35.0.0.2")
val keySpace = ContactPoints(hosts).keySpace("myApp")
    
val tables = new MyTables(keySpace)
    
tables.foos.store(Foo(UUID.randomUUID, "value"))
```     

The connector module does not prescribe a particular model here, but in most
cases you want to separate the plumbing of Connector mixin (which always stays the
same) from the dynamic keySpace creation (which depends on the environment).
        


Configuring the Driver
----------------------

The previous examples only showed how to define the initial contact points
for the driver. The API offers additional hooks for configuring
a keySpace:

```scala
// Not using the default port:
val hosts = Seq("35.0.0.1", "35.0.0.2")
val port = 9099
val keySpace = ContactPoints(hosts, port).keySpace("myApp")
    
// Embedded Cassandra for testing
val keySpace = ContactPoint.embedded.keySpace("myApp-test")
```

Additionally the API exposes the original `Cluster.Builder` API from
the Java driver for further configuration:

```scala
val hosts = Seq("35.0.0.1", "35.0.0.2")
val keySpace = ContactPoints(hosts).withClusterBuilder(
  _.withRetryPolicy(DowngradingConsistencyRetryPolicy.INSTANCE)
   .withReconnectionPolicy(new ConstantReconnectionPolicy(100L))
).keySpace("myApp")
```

It does not wrap any Scala-sugar around this part of the API as the gain
would be minimal in this case.



Using Multiple KeySpaces
------------------------

In the rare event of combining multiple keySpaces in your application,
you just need to prepare the table container class to expect two different
`KeySpace` instances:

```scala
class MyTables (fooSpace: KeySpace, barSpace: KeySpace) {
    
  object foos extends Foos with fooSpace.Connector
  object bars extends Bars with barSpace.Connector
      
}
```

And then use this container like this:

```scala
val hosts = Seq("35.0.0.1", "35.0.0.2")

val builder = ContactPoints(hosts)
val fooSpace = builder.keySpace("myFoos")
val barSpace = builder.keySpace("myBars")
    
val tables = new MyTables(fooSpace, barSpace)
```
    
Just make sure that you create all keySpaces from the same builder,
to let them share the underlying Cassandra `Cluster` instance.



Using Zookeeper to look up Contact Points
-----------------------------------------

There is now a separate module `phantom-zookeeper` that you can use in addition to
the `phantom-connector` module described in the previous sections. This module
replicates most of the functionality of the original `phantom-zookeeper` module,
but is focused on retro-fitting the old functionality on top of the new connector
module and thus become a very tiny module with just the logic which is specific to Zookeeper.

It is debatable how much value the lookup of contact points via Zookeeper adds. First,
the contact points you pass to the Cassandra driver are just the initial set of hosts
to try to connect to. Only a single contact point is required, but it is usually a
good idea to provide more in case one node is temporarily down. But once the driver
has connected to one node, it retrieves the addresses of all other nodes. Therefore
you can easily add and remove nodes to your cluster without updating the phantom configuration
unless one of the specified contact points gets removed. Secondly, the new connector module
makes it quite easy to externalize this configuration, minimizing the value the zookeeper
module adds to the connector.

But in case you want to continue using Zookeeper for lookup, the API is very similar
to that of the connector module:

```scala
// Using localhost:2181
val keySpace = ZkContactPointLookup.local.keySpace("myApp")
    
object foos extends Foos with keySpace.Connector
object bars extends Bars with keySpace.Connector
```

Or providing the Zookeeper connection explicitly:

```scala
val keySpace = ZkContactPointLookup("37.0.0.5", 2282).keySpace("myApp")
    
object foos extends Foos with keySpace.Connector
object bars extends Bars with keySpace.Connector
```
 

Using the sbt Plugin
--------------------

The new sbt plugin starts Cassandra in embedded mode whenever the sbt test task
is run. This allows to run a test suite that uses phantom without the need to 
manually start a Cassandra server upfront.

First the plugin must be included in your `plugins.sbt`:

```scala
addSbtPlugin("com.sphonic" %% "phantom-sbt" % "0.2.1")
```

Then you can apply its default settings in `build.sbt` like this:

```scala
PhantomPlugin.defaults
```

In a multi-project Scala build, you also need to add the import:

```scala
import com.sphonic.phantom.sbt.PhantomSbtPlugin._

[...]

lazy val fooProject = Project(
  id = "foo",
  base = file("foo"),
  settings = someSharedSettings ++ PhantomPlugin.defaults
).settings(
  libraryDependencies ++= Seq(
    [...]
  )
)
```

Once the default settings have been added, the plugin does the following 
things:

- Automatically starts Cassandra in embedded mode whenever the test task is run
- Forces the tests for the projects that include the settings to always run in a
  forked JVM as this is the only way to make parallel tests using phantom work.
  (This is not caused by the implementation of this plugin or the new connector
  or zookeeper artifacts, this is caused by implementation details in the official
  `phantom-dsl` artifact, mainly the use of reflection which is not thread-safe
  in Scala 2.10)

If you want to specify a custom Cassandra configuration,
you can do that with a setting:

```scala
PhantomKeys.cassandraConfig := baseDirectory.value / "config" / "cassandra.yaml"
```


Design Guidelines for Testing
-----------------------------

When designing a test suite that uses phantom, you should be aware of the
three levels of parallelism that exist when tests are run by sbt
and ScalaTest:

* *Running tests for multiple projects in a single build*: Parallelism is controlled
  by sbt. Projects always build in parallel (unless they depend on each other), 
  per default within a single JVM, but with different ClassLoaders, alternatively 
  in a separate JVM if you set `fork := true` in your build.

* *Running multiple suites in a single project*: Parallelism is controlled
  by ScalaTest. By default suites run in parallel. When sequential execution
  is needed (which should be a rare case) ScalaTest offers a `Sequential` class
  that allows to define a list of suites that need to run sequentially.
  
* *Running multiple tests in a single suite*: Parallelism is controlled
  by ScalaTest. Here, by default the tests within a suite run sequentially.
  When parallel execution is desired, ScalaTest offers a `ParallelTestExecution`
  trait that can be mixed into your suite.
  
With these options in mind, the following guidelines apply for writing tests 
for phantom's sbt plugin that uses Cassandra in embedded mode:

* The plugin forces the tests to run in a forked JVM (you do not have to explicitly
  set this option yourself). This is solely due
  to implementation details of the main `phantom-dsl` artifact, that uses runtime
  reflection to determine things like the table name or the columns defined for
  a table. Since reflection is not thread-safe in Scala 2.10, phantom uses a lock
  for these lookups, but when the tests are not forked, the fact that sbt runs
  multiple projects in a separate ClassLoader circumnavigates the locks causing
  intermittent exceptions in the reflection code. 
  
* For all other levels of parallelism there are no restrictions, but you need to 
  keep in mind that all projects and all suites run against the same instance
  of embedded Cassandra. Therefore, if you use the same table within multiple
  suites, it is recommended to shield them from each other through the use of
  different keyspaces, which is easy to do with the new phantom-connector API.
  The names of the keyspaces for tests can be hardcoded within the suites as they do
  not depend on the environment.
  
* If you code a single suite with the default (tests run sequentially) and you use
  all test data in read-mode only, you can populate the tables in a `beforeAll`
  method and then use them in all tests. If the tests write data, you can truncate
  tables before each test method.
  


Background on Design Decisions for Testing
------------------------------------------

The old `phantom-testing` module was relying on checks for a running Cassandra
server that involved either checking available ports or checking Unix processes.
These checks have proven to be unreliable as they opened doors for all sorts of
race conditions, in particular when using phantom in a multi-project build.

An alternative approach had been tested that involved running all projects
in a single JVM and having traits mixed into the test classes that start
the embedded Cassandra server only once, controlled by a JVM-wide singleton
object and without even trying to look for an existing Cassandra server.

Unfortunatley this approach does not work for the following two reasons:

* phantom uses Scala SDK reflection to automatically determine the table name
  and columns for each table. This does not work reliably as SDK reflection is
  not thread-safe in Scala 2.10 (supposedly this is fixed in 2.11) and sbt
  apparently runs multiple projects in parallel in separate ClassLoaders when
  the JVM is not forked which eliminates the lock that phantom's `CassandraTable`
  uses around its reflection code, as the same table classes are loaded multiple
  times by the different ClassLoaders.
  
* For the same reason (sbt apparently using multiple ClassLoaders) there is also
  no way to have a TestSuite mixin delegate to a global stateful singleton, as
  each project with its own ClassLoader gets its own singleton instance. So there
  is no way from within test helpers to manage state globally for the whole JVM.
  
The only approach for running embedded Cassandra that appeared to be working in
multiple tests with the existing Eris Analytics project has been to let sbt
manage the embedded Cassandra (as an sbt Task that gets triggered before the
test task is run). When run as an sbt task, a singleton is really global for 
the whole build as the build classes do not get loaded multiple times.






