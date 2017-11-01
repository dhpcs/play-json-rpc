# scala-json-rpc

[![Build Status](https://travis-ci.org/dhpcs/scala-json-rpc.svg?branch=master)](https://travis-ci.org/dhpcs/scala-json-rpc)
[![codecov](https://codecov.io/gh/dhpcs/scala-json-rpc/branch/master/graph/badge.svg)](https://codecov.io/gh/dhpcs/scala-json-rpc)
[![Latest Version](https://index.scala-lang.org/dhpcs/scala-json-rpc/scala-json-rpc/latest.svg)](https://index.scala-lang.org/dhpcs/scala-json-rpc/scala-json-rpc)

A Scala library providing types and JSON format typeclass instances for [JSON-RPC
2.0](http://www.jsonrpc.org/specification) messages along with support for marshalling application level commands,
responses and notifications via JSON-RPC 2.0.


Note that up until version 1.5.0 scala-json-rpc was called play-json-rpc. It was originally developed for use in both
the server and client side of [Liquidity](https://play.google.com/store/apps/details?id=com.dhpcs.liquidity).


## Resolution and library dependency

```scala
resolvers += Resolver.bintrayRepo("dhpcs", "maven")

libraryDependencies += "com.dhpcs" %% "scala-json-rpc" % "x.y.z"
```


## Marshalling application level types

The `CommandCompanion`, `ResponseCompanion` and `NotificationCompanion` bases defined in
[MessageCompanions.scala](scala-json-rpc/src/main/scala/com/dhpcs/jsonrpc/MessageCompanions.scala) provide readers and
writers for hierarchies of application level types. Example use can be seen in
[MessageCompanionsSpec.scala](scala-json-rpc/src/test/scala/com/dhpcs/jsonrpc/MessageCompanionsSpec.scala).


## JSON-RPC message types

The JSON-RPC message types are represented by an ADT defined in
[JsonRpcMessage.scala](scala-json-rpc/src/main/scala/com/dhpcs/jsonrpc/JsonRpcMessage.scala). The JsonRpcMessage
[JsonRpcMessageSpec.scala](scala-json-rpc/src/test/scala/com/dhpcs/jsonrpc/JsonRpcMessageSpec.scala) shows how they
appear when marshalled to and from JSON.

Note that typical usage does _not_ involve the direct construction of the low level JSON-RPC message types. The
recommended approach is to make use of the writers provided by the companion bases as demonstrated in the previously
linked marshalling specification.

The companion object for the `JsonRpcMessage` trait provides a play-json Format typeclass instance that can read and
write "raw" `JsonRpcMessage` types. If you are reading messages received over e.g. a websocket connection and thus don't
know what message type to expect at a given point, you can simply match on the subtype of the result and then use the
appropriate application specific companion to unmarshall the content to one of your application protocol's types.
Similarly, when marshalling your application protocol's types, you'll first pass them to the appropriate application
specific companion and then format the result of that as JSON, implicitly making use of the relevant provided typeclass
instance.


## License

scala-json-rpc is licensed under the Apache 2 License.
