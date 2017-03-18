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

  sealed abstract class Command                           extends Message
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
    implicit final val AddTransactionCommandFormat: Format[AddTransactionCommand] = (
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
      "updateAccount"  -> Json.format[UpdateAccountCommand],
      "addTransaction" -> Json.format[AddTransactionCommand]
    )
  }

  sealed abstract class Response                         extends Message
  case object UpdateAccountResponse                      extends Response
  final case class AddTransactionResponse(created: Long) extends Response

  object Response extends ResponseCompanion[Response] {
    override final val ResponseFormats = MessageFormats(
      "updateAccount"  -> Message.objectFormat(UpdateAccountResponse),
      "addTransaction" -> Json.format[AddTransactionResponse]
    )
  }

  sealed abstract class Notification                                      extends Message
  final case class AccountUpdatedNotification(account: Account)           extends Notification
  final case class TransactionAddedNotification(transaction: Transaction) extends Notification

  object Notification extends NotificationCompanion[Notification] {
    override final val NotificationFormats = MessageFormats(
      "accountUpdated"   -> Json.format[AccountUpdatedNotification],
      "transactionAdded" -> Json.format[TransactionAddedNotification]
    )
  }

  final case class Account(id: Int, name: Option[String] = None, metadata: Option[JsObject] = None)

  object Account {
    implicit final val AccountFormat: Format[Account] = Json.format[Account]
  }

  final case class Transaction(from: Int,
                               to: Int,
                               value: BigDecimal,
                               created: Long,
                               description: Option[String] = None,
                               metadata: Option[JsObject] = None) {
    require(value >= 0)
    require(created >= 0)
  }

  object Transaction {
    implicit final val TransactionFormat: Format[Transaction] = (
      (JsPath \ "from").format[Int] and
        (JsPath \ "to").format[Int] and
        (JsPath \ "value").format(min[BigDecimal](0)) and
        (JsPath \ "created").format(min[Long](0)) and
        (JsPath \ "description").formatNullable[String] and
        (JsPath \ "metadata").formatNullable[JsObject]
    )(
      (from, to, value, created, description, metadata) =>
        Transaction(
          from,
          to,
          value,
          created,
          description,
          metadata
      ),
      transaction =>
        (transaction.from,
         transaction.to,
         transaction.value,
         transaction.created,
         transaction.description,
         transaction.metadata)
    )
  }
}

class MessageCompanionsSpec extends FreeSpec with Matchers {

  "A Command" - {
    "with an invalid method" - {
      val jsonRpcRequestMessage = JsonRpcRequestMessage(
        method = "invalidMethod",
        Json.obj(),
        NumericCorrelationId(1)
      )
      val jsError = JsError("unknown method invalidMethod")
      s"should fail to decode with error $jsError" in (
        Command.read(jsonRpcRequestMessage) shouldBe jsError
      )
    }
    "of type AddTransactionCommand" - {
      "with params of the wrong type" - {
        val jsonRpcRequestMessage = JsonRpcRequestMessage(
          method = "addTransaction",
          Json.arr(),
          NumericCorrelationId(1)
        )
        val jsError = JsError(__, "command parameters must be named")
        s"should fail to decode with error $jsError" in (
          Command.read(jsonRpcRequestMessage) shouldBe jsError
        )
      }
      "with empty params" - {
        val jsonRpcRequestMessage = JsonRpcRequestMessage(
          method = "addTransaction",
          Json.obj(),
          NumericCorrelationId(1)
        )
        val jsError = JsError(
          Seq(
            (__ \ "to", Seq(JsonValidationError("error.path.missing"))),
            (__ \ "from", Seq(JsonValidationError("error.path.missing"))),
            (__ \ "value", Seq(JsonValidationError("error.path.missing")))
          )
        )
        s"should fail to decode with error $jsError" in (
          Command.read(jsonRpcRequestMessage) shouldBe jsError
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
          "from"        -> 0,
          "to"          -> 1,
          "value"       -> BigDecimal(1000000),
          "description" -> "Property purchase",
          "metadata" -> Json.obj(
            "property" -> "The TARDIS"
          )
        ),
        NumericCorrelationId(1)
      )
      s"should decode to $addTransactionCommand" in (
        Command.read(jsonRpcRequestMessage) shouldBe JsSuccess(addTransactionCommand)
      )
      s"should encode to $jsonRpcRequestMessage" in (
        Command.write(addTransactionCommand, id) shouldBe jsonRpcRequestMessage
      )
    }
  }

  "A Response of type AddTransactionResponse" - {
    val addTransactionResponse = AddTransactionResponse(
      created = 1434115187612L
    )
    val id = NumericCorrelationId(1)
    val jsonRpcResponseMessage = JsonRpcResponseSuccessMessage(
      Json.obj(
        "created" -> 1434115187612L
      ),
      NumericCorrelationId(1)
    )
    val method = "addTransaction"
    s"should decode to $addTransactionResponse" in (
      Response.read(jsonRpcResponseMessage, method) shouldBe JsSuccess(addTransactionResponse)
    )
    s"should encode to $jsonRpcResponseMessage" in (
      Response.write(addTransactionResponse, id) shouldBe jsonRpcResponseMessage
    )
  }

  "A Notification" - {
    "with an invalid method" - {
      val jsonRpcNotificationMessage = JsonRpcNotificationMessage(
        method = "invalidMethod",
        Json.obj()
      )
      val jsError = JsError("unknown method invalidMethod")
      s"should fail to decode with error $jsError" in (
        Notification.read(jsonRpcNotificationMessage) shouldBe jsError
      )
    }
    "of type TransactionAddedNotification" - {
      "with params of the wrong type" - {
        val jsonRpcNotificationMessage = JsonRpcNotificationMessage(
          method = "transactionAdded",
          Json.arr()
        )
        val jsError = JsError(__, "notification parameters must be named")
        s"should fail to decode with error $jsError" in (
          Notification.read(jsonRpcNotificationMessage) shouldBe jsError
        )
      }
      "with empty params" - {
        val jsonRpcNotificationMessage = JsonRpcNotificationMessage(
          method = "transactionAdded",
          Json.obj()
        )
        val jsError = JsError(__ \ "transaction", "error.path.missing")
        s"should fail to decode with error $jsError" in (
          Notification.read(jsonRpcNotificationMessage) shouldBe jsError
        )
      }
      val clientJoinedZoneNotification = TransactionAddedNotification(
        Transaction(
          from = 0,
          to = 1,
          value = BigDecimal(1000000),
          created = 1434115187612L
        )
      )
      val jsonRpcNotificationMessage = JsonRpcNotificationMessage(
        method = "transactionAdded",
        params = Json.obj(
          "transaction" -> Json.parse("""{"from":0,"to":1,"value":1000000,"created":1434115187612}""")
        )
      )
      s"should decode to $clientJoinedZoneNotification" in (
        Notification.read(jsonRpcNotificationMessage) shouldBe JsSuccess(clientJoinedZoneNotification)
      )
      s"should encode to $jsonRpcNotificationMessage" in (
        Notification.write(clientJoinedZoneNotification) shouldBe jsonRpcNotificationMessage
      )
    }
  }
}
