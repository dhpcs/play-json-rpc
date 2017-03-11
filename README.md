scala-json-rpc
==============

[![Build status](https://travis-ci.org/dhpcs/scala-json-rpc.svg?branch=master)](https://travis-ci.org/dhpcs/scala-json-rpc)
[![codecov](https://codecov.io/gh/dhpcs/scala-json-rpc/branch/master/graph/badge.svg)](https://codecov.io/gh/dhpcs/scala-json-rpc)
[![Dependencies](https://app.updateimpact.com/badge/835521161172488192/scala-json-rpc-root.svg?config=compile)](https://app.updateimpact.com/latest/835521161172488192/scala-json-rpc-root)
[![Download](https://api.bintray.com/packages/dhpcs/maven/scala-json-rpc/images/download.svg)](https://bintray.com/dhpcs/maven/scala-json-rpc/_latestVersion)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

A Scala library providing types and JSON format typeclass instances for
[JSON-RPC 2.0](http://www.jsonrpc.org/specification) messages along with support for marshalling application level
commands, responses and notifications via JSON-RPC 2.0.

Note that prior to version 1.5.0 scala-json-rpc was named play-json-rpc.


Status
------

scala-json-rpc is actively maintained and used in multiple production systems. Both the server and client side of
[Liquidity](https://play.google.com/store/apps/details?id=com.dhpcs.liquidity) use it. Liquidity is the system that
originally motivated development.


Versioning
----------

All versions greater than 1.0.0 implement the JSON-RPC 2.0 specification.

Patch version increments between versions 1.x.y to 1.5.0 are source compatible with the preceding minor version (e.g.
1.4.1 is source compatible with 1.4.0). However, minor version increments are not generally source compatible with the
preceding minor version (i.e. 1.5.0 is not source compatible with 1.4.0).


Resolution and library dependency
---------------------------------

```scala
resolvers += Resolver.bintrayRepo("dhpcs", "maven")

libraryDependencies += "com.dhpcs" %% "scala-json-rpc" % "1.5.0"
```


Marshalling application level types
-----------------------------------

The `CommandCompanion`, `ResponseCompanion` and `NotificationCompanion` bases defined in
[MessageCompanions.scala](scala-json-rpc/src/main/scala/com/dhpcs/jsonrpc/MessageCompanions.scala) provide readers and writers
for hierarchies of application level types. Example use can be seen in
[MessageCompanionsSpec.scala](scala-json-rpc/src/test/scala/com/dhpcs/jsonrpc/MessageCompanionsSpec.scala). 


JSON-RPC message types
----------------------

The JSON-RPC message types are represented by an ADT defined in 
[JsonRpcMessage.scala](scala-json-rpc/src/main/scala/com/dhpcs/jsonrpc/JsonRpcMessage.scala). The JsonRpcMessage
[JsonRpcMessageSpec.scala](scala-json-rpc/src/test/scala/com/dhpcs/jsonrpc/JsonRpcMessageSpec.scala) shows how they appear when
marshalled to and from JSON.

Note that typical usage does _not_ involve the direct construction of the low level JSON-RPC message types. The
recommended approach is to make use of the writers provided by the companion bases as demonstrated in the previously
linked [marshalling specification](scala-json-rpc/src/test/scala/com/dhpcs/jsonrpc/MessageCompanionsSpec.scala).

The companion object for the `JsonRpcMessage` trait provides a play-json Format typeclass instance that can read and
write "raw" `JsonRpcMessage` types. If you are reading messages received over e.g. a websocket connection and thus
don't know what message type to expect at a given point, you can simply match on the subtype of the result and then use
the appropriate application specific companion to unmarshall the content to one of your application protocol's types.
Similarly, when marshalling your application protocol's types, you'll first pass them to the appropriate application
specific companion and then format the result of that as JSON, implicitly making use of the relevant provided typeclass
instance.


play-json-testkit
-----------------

The `JsResultUniformity` and `FormatBehaviors` types in the `com.dhpcs.json` package of the testkit module may be
useful when writing tests in dependent projects. Example use can be seen in the above linked
[JsonRpcMessage specification](scala-json-rpc/src/test/scala/com/dhpcs/jsonrpc/JsonRpcMessageSpec.scala).


### Testkit resolution and library dependency

```scala
libraryDependencies += "com.dhpcs" %% "play-json-testkit" % "1.5.0" % Test
```


License
-------

scala-json-rpc is licensed under the Apache 2 License.
