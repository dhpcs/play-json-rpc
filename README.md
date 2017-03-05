play-json-rpc
=============

[![Build status](https://travis-ci.org/dhpcs/play-json-rpc.svg?branch=master)](https://travis-ci.org/dhpcs/play-json-rpc)
[![codecov](https://codecov.io/gh/dhpcs/play-json-rpc/branch/master/graph/badge.svg)](https://codecov.io/gh/dhpcs/play-json-rpc)
[![Dependencies](https://app.updateimpact.com/badge/835521161172488192/play-json-rpc-root.svg?config=compile)](https://app.updateimpact.com/latest/835521161172488192/play-json-rpc-root)
[![Download](https://api.bintray.com/packages/dhpcs/maven/play-json-rpc/images/download.svg)](https://bintray.com/dhpcs/maven/play-json-rpc/_latestVersion)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

A Scala library providing types and JSON format typeclass instances for
[JSON-RPC 2.0](http://www.jsonrpc.org/specification) messages along with support for marshalling application level
commands, responses and notifications over JSON-RPC.


Status
------

play-json-rpc is actively maintained and used in multiple production systems. Both the server and client side of
[Liquidity](https://play.google.com/store/apps/details?id=com.dhpcs.liquidity) use it. Liquidity is the system that
originally motivated development.


Resolution
----------

```scala
resolvers += Resolver.bintrayRepo("dhpcs", "maven")
```


play-json-rpc
-------------

play-json-rpc provides types and formats for JSON-RPC 2.0 messages and a set of abstract classes for use as companion
object bases. The provided functions facilitate the marshalling of application level types into and out of JSON-RPC
messages. 

### Library dependency

```scala
libraryDependencies += "com.dhpcs" %% "play-json-rpc" % "1.4.1"
```

### Marshalling application level types

The provided `CommandCompanion`, `ResponseCompanion` and `NotificationCompanion` bases provide readers and writers for
hierarchies of application level types. Example use can be seen in
[`MessageCompanionsSpec.scala`](rpc/src/main/scala/com/dhpcs/jsonrpc/MessageCompanionsSpec.scala).

Typical usage does not involve the manual construction of any of the following low level JSON-RPC message types. The
recommended approach is to make use of the writers provided by the companion bases as demonstrated in the above linked
[specification](rpc/src/main/scala/com/dhpcs/jsonrpc/MessageCompanionsSpec.scala).

The companion object for the `JsonRpcMessage` trait provides a play-json Format typeclass instance that can read and
write all of the above types. If you are reading messages received over a websocket connection and thus don't know what
message type to expect at a given point, you can simply match on the type of the result and then use the appropriate
application specific companion to unmarshall the content to one of your application protocol's types. Similarly, when
marshalling your application protocol's types, you'll first pass them to the appropriate application specific companion
and then format the result of that as JSON, implicitly making use of the relevant provided typeclass instance. 

### JSON-RPC message types

1. `JsonRpcRequestMessage`:

  ```scala
  case class JsonRpcRequestMessage(method: String,
                                   params: Option[Either[JsArray, JsObject]],
                                   id: Option[Either[String, BigDecimal]]) extends JsonRpcMessage
  ```

2. `JsonRpcRequestMessageBatch`:

  ```scala
  case class JsonRpcRequestMessageBatch(messages: Seq[Either[JsonRpcNotificationMessage, JsonRpcRequestMessage]])
    extends JsonRpcMessage {
    require(messages.nonEmpty)
  }
  ```

3. `JsonRpcResponseMessage`:

  ```scala
  case class JsonRpcResponseMessage(errorOrResult: Either[JsonRpcResponseError, JsValue],
                                    id: Option[Either[String, BigDecimal]]) extends JsonRpcMessage
  ```

4. `JsonRpcResponseMessageBatch`:

  ```scala
  case class JsonRpcResponseMessageBatch(messages: Seq[JsonRpcResponseMessage]) extends JsonRpcMessage {
    require(messages.nonEmpty)
  }
  ```

5. `JsonRpcNotificationMessage`:

  ```scala
  case class JsonRpcNotificationMessage(method: String,
                                        params: Either[JsArray, JsObject]) extends JsonRpcMessage
  ```


#### Response error types

A `JsonRpcResponseError` looks like this:

```scala
sealed abstract case class JsonRpcResponseError(code: Int, message: String, data: Option[JsValue])
```

And has the following functions available on its companion object with which to create instances:

```scala
def parseError(exception: Throwable): JsonRpcResponseError

def invalidRequest(errors: Seq[(JsPath, Seq[ValidationError])]): JsonRpcResponseError

def methodNotFound(method: String): JsonRpcResponseError

def invalidParams(errors: Seq[(JsPath, Seq[ValidationError])]): JsonRpcResponseError

def internalError(error: Option[JsValue] = None): JsonRpcResponseError

def serverError(code: Int, error: Option[JsValue] = None): JsonRpcResponseError

def applicationError(code: Int,
                     message: String,
                     data: Option[JsValue] = None): JsonRpcResponseError
```

Note that `applicationError(...)` and `serverError(...)` validate the integer code passed in and will fail if it is not
legal for the error type as per the JSON-RPC 2.0 spec.


play-json-testkit
-----------------

The `JsResultUniformity` and `FormatBehaviors` types in the `com.dhpcs.json` package of the testkit module may be
useful when writing tests in dependent projects. Example use can be seen in
[`JsonRpcMessageSpec.scala`](rpc/src/main/scala/com/dhpcs/jsonrpc/JsonRpcMessageSpec.scala).


### Library dependency

```scala
libraryDependencies += "com.dhpcs" %% "play-json-testkit" % "1.4.1" % Test
```


License
-------

play-json-rpc is licensed under the Apache 2 License.
