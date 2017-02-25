play-json-rpc
=============

[![Build status](https://travis-ci.org/dhpcs/play-json-rpc.svg?branch=master)](https://travis-ci.org/dhpcs/play-json-rpc)
[![codecov](https://codecov.io/gh/dhpcs/play-json-rpc/branch/master/graph/badge.svg)](https://codecov.io/gh/dhpcs/play-json-rpc)
[![Dependencies](https://app.updateimpact.com/badge/835521161172488192/play-json-rpc-root.svg?config=compile)](https://app.updateimpact.com/latest/835521161172488192/play-json-rpc-root)
[![Download](https://api.bintray.com/packages/dhpcs/maven/play-json-rpc/images/download.svg)](https://bintray.com/dhpcs/maven/play-json-rpc/_latestVersion)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

A Scala library providing implicit [play-json Formats](https://www.playframework.com/documentation/2.5.x/ScalaJson) for
[JSON-RPC 2.0](http://www.jsonrpc.org/specification) messages, built on top of the Play Framework's standalone
`play-json` library.


SBT dependency
--------------

```scala
resolvers += Resolver.bintrayRepo("dhpcs", "maven")

libraryDependencies += "com.dhpcs" %% "play-json-rpc" % "1.4.1"
```


Structure
---------

`play-json-rpc` essentially provides two things:

1. Types and formats for JSON-RPC 2.0 messages. The types are as follows:

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
      case class JsonRpcResponseMessage(eitherErrorOrResult: Either[JsonRpcResponseError, JsValue],
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

 The companion object for the `JsonRpcMessage` trait has an implicitly available `JsonRpcMessageFormat` that can read
 and write all of the above types – you can then match on the type of the result.

2. A set of abstract classes as bases for companion objects providing read/write functions for a hierarchy of
   application level types (see example usage).


Example usage
-------------

This is best demonstrated by the example project (located in the `example` directory).

The best way to get started with `play-json-rpc` in your project is to copy the example project and then
add/change/remove domain and message types as necessary. You might want to copy and modify the example tests too.

The example project includes:

1. Two domain types with JSON Formats, `Account` and `Transaction` (definitions omitted for brevity here because they
   are fairly typical `play-json` usage).

2. Two command message types (`UpdateAccountCommand` and `AddTransactionCommand`), two matching response types
   (`UpdateAccountResponse` and `AddTransactionResponse`) and two notification message types
   (`AccountUpdatedNotification` and `TransactionAddedNotification`).

   Note that `play-json-rpc` leaves the definition of the `Message` type hierarchy to your code in order that `Message`
   and all its subtypes can be defined as sealed to let you benefit from the [compiler checks](
   http://www.scala-lang.org/old/node/123) that sealed enables.

   Thus most users should copy and preserve the definitions of `Message`, `Command`, `Response`, `ResultResponse` and
   `Notification`, and simply replace the implementations of `Command`, `ResultResponse` and `Notfication` with their own.

3. Partial code for a server that receives messages of type `JsonRpcRequestMessage` and sends messages of type
   `JsonRpcResponseMessage` and `JsonRpcNotificationMessage`.

4. Partial code for a client that sends messages of type `JsonRpcRequestMessage` and receives messages of type
   `JsonRpcResponseMessage` and `JsonRpcNotificationMessage`.


### Example message types

The types that you should define as a user of `play-json-rpc` will look something like the following.

The full definition with imports is at [example/src/main/scala/com/dhpcs/jsonrpc/example/models/Message.scala](
example/src/main/scala/com/dhpcs/jsonrpc/example/models/Message.scala).

```scala
sealed trait Message

sealed trait Command extends Message

case class UpdateAccountCommand(account: Account) extends Command

case class AddTransactionCommand(from: Int,
                                 to: Int,
                                 value: BigDecimal,
                                 description: Option[String] = None,
                                 metadata: Option[JsObject] = None) extends Command {
  require(value >= 0)
}

object AddTransactionCommand {
  implicit val AddTransactionCommandFormat: Format[AddTransactionCommand] = (
    (JsPath \ "from").format[Int] and
      (JsPath \ "to").format[Int] and
      (JsPath \ "value").format(min[BigDecimal](0)) and
      (JsPath \ "description").formatNullable[String] and
      (JsPath \ "metadata").formatNullable[JsObject]
    )((from, to, value, description, metadata) =>
    AddTransactionCommand(
      from,
      to,
      value,
      description,
      metadata
    ), addTransactionCommand =>
    (addTransactionCommand.from,
      addTransactionCommand.to,
      addTransactionCommand.value,
      addTransactionCommand.description,
      addTransactionCommand.metadata)
    )
}

object Command extends CommandCompanion[Command] {
  override val CommandFormats = MessageFormats(
    "updateAccount" -> Json.format[UpdateAccountCommand],
    "addTransaction" -> Json.format[AddTransactionCommand]
  )
}

sealed trait Response extends Message

sealed trait ResultResponse extends Response

case object UpdateAccountResponse extends ResultResponse

case class AddTransactionResponse(created: Long) extends ResultResponse

object Response extends ResponseCompanion[ResultResponse] {
  override val ResponseFormats = MessageFormats(
    "updateAccount" -> Message.objectFormat(UpdateAccountResponse),
    "addTransaction" -> Json.format[AddTransactionResponse]
  )
}

sealed trait Notification extends Message

case class AccountUpdatedNotification(account: Account) extends Notification

case class TransactionAddedNotification(transaction: Transaction) extends Notification

object Notification extends NotificationCompanion[Notification] {
  override val NotificationFormats = MessageFormats(
    "accountUpdated" -> Json.format[AccountUpdatedNotification],
    "transactionAdded" -> Json.format[TransactionAddedNotification]
  )
}
```


### Example server code

Note that `readCommand` is not included in `play-json-rpc` itself because different use cases may have different
directionality. The following example receives commands and sends responses and notifications; other use cases might
receive commands and notifications but only send responses.

The full definition with imports is at [example/src/main/scala/com/dhpcs/jsonrpc/example/Server.scala](
example/src/main/scala/com/dhpcs/jsonrpc/example/Server.scala).

```scala
object Server {
  private def readCommand(jsonString: String):
  (Option[Either[String, BigDecimal]], Either[JsonRpcResponseError, Command]) =
    Try(Json.parse(jsonString)) match {
      case Failure(exception) =>
        None -> Left(
          JsonRpcResponseError.parseError(exception)
        )
      case Success(json) =>
        Json.fromJson[JsonRpcRequestMessage](json).fold(
          errors => None -> Left(
            JsonRpcResponseError.invalidRequest(errors)
          ),
          jsonRpcRequestMessage =>
            Command.read(jsonRpcRequestMessage)
              .fold[(Option[Either[String, BigDecimal]], Either[JsonRpcResponseError, Command])](
              jsonRpcRequestMessage.id -> Left(
                JsonRpcResponseError.methodNotFound(jsonRpcRequestMessage.method)
              )
            )(commandJsResult => commandJsResult.fold(
              errors => jsonRpcRequestMessage.id -> Left(
                JsonRpcResponseError.invalidParams(errors)
              ),
              command => jsonRpcRequestMessage.id -> Right(
                command
              )
            ))
        )
    }
}

class Server {
  private[this] def deliverToSender(jsonString: String) = ???

  private[this] def deliverToSubscribers(jsonString: String) = ???

  def yourMessageHandler(jsonString: String) = readCommand(jsonString) match {
    case (id, Left(jsonRpcResponseError)) =>
      val jsonResponseString = Json.stringify(Json.toJson(
        JsonRpcResponseMessage(Left(jsonRpcResponseError), id)
      ))
      deliverToSender(jsonResponseString)
    case (id, Right(command)) =>
      command match {
        case UpdateAccountCommand(account) =>
          // Omitted: validate and handle the command
          // ...
          // For demonstration purposes we'll toss a coin to decide whether to reject the command
          if (Random.nextBoolean()) {
            // Rejecting a command:
            val jsonResponseString = Json.stringify(Json.toJson(
              Response.write(
                Left(
                  ErrorResponse(
                    JsonRpcResponseError.ReservedErrorCodeFloor - 1,
                    "Fate does not favour you today"
                  )
                ),
                id
              )
            ))
            deliverToSender(jsonResponseString)
          } else {
            val jsonResponseString = Json.stringify(Json.toJson(
              Response.write(
                Right(
                  UpdateAccountResponse
                ),
                id
              )
            ))
            deliverToSender(jsonResponseString)
            val jsonNotificationString = Json.stringify(Json.toJson(
              Notification.write(
                AccountUpdatedNotification(
                  account
                )
              )
            ))
            deliverToSubscribers(jsonNotificationString)
          }
        case AddTransactionCommand(from, to, value, description, metadata) =>
          // Omitted: validate and handle the command
          // ...
          // For demonstration purposes we'll toss a coin to decide whether to reject the command
          if (Random.nextBoolean()) {
            // Rejecting a command:
            val jsonResponseString = Json.stringify(Json.toJson(
              Response.write(
                Left(
                  ErrorResponse(
                    JsonRpcResponseError.ReservedErrorCodeFloor - 1,
                    "Fate does not favour you today"
                  )
                ),
                id
              )
            ))
            deliverToSender(jsonResponseString)
          } else {
            val transaction = Transaction(from, to, value, System.currentTimeMillis, description, metadata)
            val jsonResponseString = Json.stringify(Json.toJson(
              Response.write(
                Right(
                  AddTransactionResponse(
                    transaction.created
                  )
                ),
                id
              )
            ))
            deliverToSender(jsonResponseString)
            val jsonNotificationString = Json.stringify(Json.toJson(
              Notification.write(
                TransactionAddedNotification(
                  transaction
                )
              )
            ))
            deliverToSubscribers(jsonNotificationString)
          }
      }
  }
}
```


### Example client code

Note that `readJsonRpcMessage` is not included in `play-json-rpc` itself, for the same reason that the server example's
`readCommand` is not.

Note also that in the real project this example is based on, there is extra code that ensures `pendingRequests` etc. is
only accessed by one thread. That code was removed to simplify this example.

The full definition with imports is at [example/src/main/scala/com/dhpcs/jsonrpc/example/Client.scala](
example/src/main/scala/com/dhpcs/jsonrpc/example/Client.scala).

```scala
object Client {

  trait ResponseCallback {
    def onErrorReceived(errorResponse: ErrorResponse)

    def onResultReceived(resultResponse: ResultResponse) = ()
  }

  private case class PendingRequest(requestMessage: JsonRpcRequestMessage,
                                    callback: ResponseCallback)

  private def readJsonRpcMessage(jsonString: String): Either[String, JsonRpcMessage] =
    Try(Json.parse(jsonString)) match {
      case Failure(exception) =>
        Left(s"Invalid JSON: $exception")
      case Success(json) =>
        Json.fromJson[JsonRpcMessage](json).fold(
          errors => Left(s"Invalid JSON-RPC message: $errors"),
          jsonRpcMessage => Right(jsonRpcMessage)
        )
    }
}

class Client {
  private[this] var pendingRequests = Map.empty[BigDecimal, PendingRequest]
  private[this] var commandIdentifier = BigDecimal(0)

  private[this] def deliverToServer(jsonString: String) = ???

  private[this] def notifySubscribers(notification: Notification) = ???

  def sendCommand(command: Command, responseCallback: ResponseCallback) {
    val jsonRpcRequestMessage = Command.write(command, Some(Right(commandIdentifier)))
    commandIdentifier = commandIdentifier + 1
    val jsonCommandString = Json.stringify(
      Json.toJson(jsonRpcRequestMessage)
    )
    deliverToServer(jsonCommandString)
    pendingRequests = pendingRequests +
      (jsonRpcRequestMessage.id.get.right.get ->
        PendingRequest(jsonRpcRequestMessage, responseCallback))
  }

  def yourMessageHandler(jsonString: String) {
    readJsonRpcMessage(jsonString) match {
      case Left(error) =>
        sys.error(error)
      case Right(jsonRpcMessage) => jsonRpcMessage match {
        case jsonRpcRequestMessage: JsonRpcRequestMessage =>
          sys.error(s"Received $jsonRpcRequestMessage")
        case jsonRpcRequestMessageBatch: JsonRpcRequestMessageBatch =>
          sys.error(s"Received $jsonRpcRequestMessageBatch")
        case jsonRpcResponseMessage: JsonRpcResponseMessage =>
          jsonRpcResponseMessage.id.fold {
            sys.error(s"JSON-RPC message ID missing, jsonRpcResponseMessage" +
              s".eitherErrorOrResult=${jsonRpcResponseMessage.eitherErrorOrResult}")
          } { id =>
            id.right.toOption.fold {
              sys.error(s"JSON-RPC message ID was not a number, id=$id")
            } { commandIdentifier =>
              pendingRequests.get(commandIdentifier).fold {
                sys.error(s"No pending request exists with commandIdentifier" +
                  s"=$commandIdentifier")
              } { pendingRequest =>
                pendingRequests = pendingRequests - commandIdentifier
                Response.read(
                  jsonRpcResponseMessage,
                  pendingRequest.requestMessage.method
                ).fold({ errors =>
                  sys.error(s"Invalid Response: $errors")
                }, {
                  case Left(errorResponse) =>
                    pendingRequest.callback.onErrorReceived(errorResponse)
                  case Right(resultResponse) =>
                    pendingRequest.callback.onResultReceived(resultResponse)
                })
              }
            }
          }
        case jsonRpcResponseMessageBatch: JsonRpcResponseMessageBatch =>
          sys.error(s"Received $jsonRpcResponseMessageBatch")
        case jsonRpcNotificationMessage: JsonRpcNotificationMessage =>
          Notification.read(jsonRpcNotificationMessage).fold {
            sys.error(s"No notification type exists with method" +
              s"=${jsonRpcNotificationMessage.method}")
          }(_.fold({ errors =>
            sys.error(s"Invalid Notification: $errors")
          }, notifySubscribers
          ))
      }
    }
  }
}
```


Error responses
---------------

A `JsonRpcResponseError` looks like this:

```scala
sealed trait JsonRpcResponseError {

  def code: Int

  def message: String

  def data: Option[JsValue]

}
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


Testkit
-------

There are two types in the `com.dhpcs.json` package of the testkit module that may be useful when writing tests in
dependent projects (example use can be seen in `JsonRpcMessageSpec.scala`):
1. `JsResultUniformity`
2. `FormatBehaviors`

They are published as a separate artifact, so to make use of them in your dependent project's tests, set your
dependencies to be like this:

```scala
libraryDependencies ++= Seq(
  "com.dhpcs" %% "play-json-rpc" % "1.4.1",
  "org.scalatest" %% "scalatest" % "3.0.1" % Test,
  "com.dhpcs" %% "play-json-rpc-testkit" % "1.4.1" % Test
)
```


Contributing
------------

Contributions – both code and documentation – are welcome.

The tests are in `JsonRpcMessageSpec.scala` and can be run with `sbt test`.


License
-------

play-json-rpc is licensed under the Apache 2 License.
