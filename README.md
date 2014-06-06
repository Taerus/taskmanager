TaskManager
===========

A scala task manager library based on akka


Installation
------------

Builds are available on [the repository](http://taerus.github.io/taskmanager/repository/)

#### For SBT users
Add the following to your sbt build (for scala 2.10.4)
```scala
resolvers += "TaskManager GitHub Repository" at "http://taerus.github.io/taskmanager/repository"

dependencies ++= Seq(
  "akka.duke" %%  "taskmanager-core"    % "1.0.1-SNAPSHOT",
  "akka.duke" %%  "taskmanager-swing"   % "1.0.1-SNAPSHOT",
  "akka.duke" %%  "taskmanager-spring"  % "1.0.1-SNAPSHOT",
  "akka.duke" %%  "taskmanager-macros"  % "1.0.1-SNAPSHOT"
)
```


Licence
-------

Copyright (c) 2014 Jonathan Couroyer.  
See the LICENSE file for license rights and limitations (BSD 3-Clause).
