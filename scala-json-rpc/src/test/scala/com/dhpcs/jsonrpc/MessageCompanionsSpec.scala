package com.dhpcs.jsonrpc

import com.dhpcs.jsonrpc.JsonRpcMessage._
import com.dhpcs.jsonrpc.Message.MessageFormats
import com.dhpcs.jsonrpc.MessageCompanionsSpec._
import org.scalatest._
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads.min
import play.api.libs.json._

object MessageCompanionsSpec {

  sealed abstract class Message

  sealed abstract class Command extends Message
  final case class UpdateAccountCommand(account: Account) extends Command
  final case class AddTransactionCommand(from: Int,
                                         to: Int,
                                         value: BigDecimal,
                                         description: Option[String] = None,
                                         metadata: Option[JsObject] = None)
      extends Command {
    require(value >= 0)
  }

  object AddTransactionCommand {
    implicit final val AddTransactionCommandFormat
      : Format[AddTransactionCommand] = (
      (JsPath \ "from").format[Int] and
        (JsPath \ "to").format[Int] and
        (JsPath \ "value").format(min[BigDecimal](0)) and
        (JsPath \ "description").formatNullable[String] and
        (JsPath \ "metadata").formatNullable[JsObject]
    )(
      (from, to, value, description, metadata) =>
        AddTransactionCommand(
          from,
          to,
          value,
          description,
          metadata
      ),
      addTransactionCommand =>
        (addTransactionCommand.from,
         addTransactionCommand.to,
         addTransactionCommand.value,
         addTransactionCommand.description,
         addTransactionCommand.metadata)
    )
  }

  object Command extends CommandCompanion[Command] {
    override final val CommandFormats = MessageFormats(
      "updateAccount" -> Json.format[UpdateAccountCommand],
      "addTransaction" -> Json.format[AddTransactionCommand]
    )
  }

  sealed abstract class Response extends Message
  case object UpdateAccountResponse extends Response
  final case class AddTransactionResponse(id: Int) extends Response

  object Response extends ResponseCompanion[Response] {
    override final val ResponseFormats = MessageFormats(
      "updateAccount" -> Message.objectFormat(UpdateAccountResponse),
      "addTransaction" -> Format[AddTransactionResponse](
        Reads.of[Int].map(AddTransactionResponse),
        Writes.of[Int].contramap(_.id)
      )
    )
  }

  sealed abstract class Notification extends Message
  final case class AccountUpdatedNotification(account: Account)
      extends Notification
  final case class TransactionAddedNotification(transaction: Transaction)
      extends Notification

  object Notification extends NotificationCompanion[Notification] {
    override final val NotificationFormats = MessageFormats(
      "accountUpdated" -> Json.format[AccountUpdatedNotification],
      "transactionAdded" -> Json.format[TransactionAddedNotification]
    )
  }

  final case class Account(id: Int,
                           name: Option[String] = None,
                           metadata: Option[JsObject] = None)

  object Account {
    implicit final val AccountFormat: Format[Account] = Json.format[Account]
  }

  final case class Transaction(id: Int,
                               from: Int,
                               to: Int,
                               value: BigDecimal,
                               description: Option[String] = None,
                               metadata: Option[JsObject] = None) {
    require(value >= 0)
  }

  object Transaction {
    implicit final val TransactionFormat: Format[Transaction] = (
      (JsPath \ "id").format[Int] and
        (JsPath \ "from").format[Int] and
        (JsPath \ "to").format[Int] and
        (JsPath \ "value").format(min[BigDecimal](0)) and
        (JsPath \ "description").formatNullable[String] and
        (JsPath \ "metadata").formatNullable[JsObject]
    )(
      (id, from, to, value, description, metadata) =>
        Transaction(
          id,
          from,
          to,
          value,
          description,
          metadata
      ),
      transaction =>
        (transaction.id,
         transaction.from,
         transaction.to,
         transaction.value,
         transaction.description,
         transaction.metadata)
    )
  }
}

class MessageCompanionsSpec extends FreeSpec {

  "A Command" - {
    "with an invalid method" - {
      val jsonRpcRequestMessage = JsonRpcRequestMessage(
        method = "invalidMethod",
        JsObject.empty,
        NumericCorrelationId(1)
      )
      val jsError = JsError("unknown method invalidMethod")
      s"fails to decode with error $jsError" in assert(
        Command.read(jsonRpcRequestMessage) === jsError
      )
    }
    "of type AddTransactionCommand" - {
      "with params of the wrong type" - {
        val jsonRpcRequestMessage = JsonRpcRequestMessage(
          method = "addTransaction",
          JsArray.empty,
          NumericCorrelationId(1)
        )
        val jsError = JsError(__, "command parameters must be named")
        s"fails to decode with error $jsError" in assert(
          Command.read(jsonRpcRequestMessage) === jsError
        )
      }
      "with empty params" - {
        val jsonRpcRequestMessage = JsonRpcRequestMessage(
          method = "addTransaction",
          JsObject.empty,
          NumericCorrelationId(1)
        )
        val jsError = JsError(
          Seq(
            (__ \ "to", Seq(JsonValidationError("error.path.missing"))),
            (__ \ "from", Seq(JsonValidationError("error.path.missing"))),
            (__ \ "value", Seq(JsonValidationError("error.path.missing")))
          )
        )
        s"fails to decode with error $jsError" in assert(
          Command.read(jsonRpcRequestMessage) === jsError
        )
      }
      val addTransactionCommand = AddTransactionCommand(
        from = 0,
        to = 1,
        value = BigDecimal(1000000),
        description = Some("Property purchase"),
        metadata = Some(
          Json.obj(
            "property" -> "The TARDIS"
          )
        )
      )
      val id = NumericCorrelationId(1)
      val jsonRpcRequestMessage = JsonRpcRequestMessage(
        method = "addTransaction",
        Json.obj(
          "from" -> 0,
          "to" -> 1,
          "value" -> BigDecimal(1000000),
          "description" -> "Property purchase",
          "metadata" -> Json.obj(
            "property" -> "The TARDIS"
          )
        ),
        NumericCorrelationId(1)
      )
      s"decodes to $addTransactionCommand" in assert(
        Command.read(jsonRpcRequestMessage) ===
          JsSuccess(addTransactionCommand)
      )
      s"encodes to $jsonRpcRequestMessage" in assert(
        Command.write(addTransactionCommand, id) === jsonRpcRequestMessage
      )
    }
  }

  "A Response of type AddTransactionResponse" - {
    val addTransactionResponse = AddTransactionResponse(id = 0)
    val id = NumericCorrelationId(1)
    val jsonRpcResponseMessage = JsonRpcResponseSuccessMessage(
      JsNumber(0),
      NumericCorrelationId(1)
    )
    val method = "addTransaction"
    s"decodes to $addTransactionResponse" in assert(
      Response.read(jsonRpcResponseMessage, method) === JsSuccess(
        addTransactionResponse)
    )
    s"encodes to $jsonRpcResponseMessage" in assert(
      Response.write(addTransactionResponse, id) === jsonRpcResponseMessage
    )
  }

  "A Notification" - {
    "with an invalid method" - {
      val jsonRpcNotificationMessage = JsonRpcNotificationMessage(
        method = "invalidMethod",
        JsObject.empty
      )
      val jsError = JsError("unknown method invalidMethod")
      s"fails to decode with error $jsError" in assert(
        Notification.read(jsonRpcNotificationMessage) === jsError
      )
    }
    "of type TransactionAddedNotification" - {
      "with params of the wrong type" - {
        val jsonRpcNotificationMessage = JsonRpcNotificationMessage(
          method = "transactionAdded",
          JsArray.empty
        )
        val jsError = JsError(__, "notification parameters must be named")
        s"fails to decode with error $jsError" in assert(
          Notification.read(jsonRpcNotificationMessage) === jsError
        )
      }
      "with empty params" - {
        val jsonRpcNotificationMessage = JsonRpcNotificationMessage(
          method = "transactionAdded",
          JsObject.empty
        )
        val jsError = JsError(__ \ "transaction", "error.path.missing")
        s"fails to decode with error $jsError" in assert(
          Notification.read(jsonRpcNotificationMessage) === jsError
        )
      }
      val clientJoinedZoneNotification = TransactionAddedNotification(
        Transaction(
          id = 0,
          from = 0,
          to = 1,
          value = BigDecimal(1000000)
        )
      )
      val jsonRpcNotificationMessage = JsonRpcNotificationMessage(
        method = "transactionAdded",
        params = Json.obj(
          "transaction" -> Json.parse(
            """{ "id" : 0, "from" : 0, "to" : 1, "value" : 1000000 }""")
        )
      )
      s"decodes to $clientJoinedZoneNotification" in assert(
        Notification.read(jsonRpcNotificationMessage) === JsSuccess(
          clientJoinedZoneNotification)
      )
      s"encodes to $jsonRpcNotificationMessage" in assert(
        Notification
          .write(clientJoinedZoneNotification) === jsonRpcNotificationMessage
      )
    }
  }
}
