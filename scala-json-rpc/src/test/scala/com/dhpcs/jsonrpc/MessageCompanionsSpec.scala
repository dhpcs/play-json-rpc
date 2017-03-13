package com.dhpcs.jsonrpc

import com.dhpcs.json.JsResultUniformity
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
  sealed abstract class ResultResponse                   extends Response
  case object UpdateAccountResponse                      extends ResultResponse
  final case class AddTransactionResponse(created: Long) extends ResultResponse

  object Response extends ResponseCompanion[ResultResponse] {
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

class MessageCompanionsSpec extends FunSpec with Matchers {

  describe("A Command") {
    describe("with an invalid method")(
      it should behave like commandReadError(
        JsonRpcRequestMessage(
          method = "invalidMethod",
          Json.obj(),
          NumericCorrelationId(1)
        ),
        JsError("unknown method invalidMethod")
      )
    )
    describe("of type AddTransactionCommand") {
      describe("with params of the wrong type")(
        it should behave like commandReadError(
          JsonRpcRequestMessage(
            method = "addTransaction",
            Json.arr(),
            NumericCorrelationId(1)
          ),
          JsError(__, "command parameters must be named")
        )
      )
      describe("with empty params")(
        it should behave like commandReadError(
          JsonRpcRequestMessage(
            method = "addTransaction",
            Json.obj(),
            NumericCorrelationId(1)
          ),
          JsError(
            Seq(
              (__ \ "from", Seq(JsonValidationError("error.path.missing"))),
              (__ \ "to", Seq(JsonValidationError("error.path.missing"))),
              (__ \ "value", Seq(JsonValidationError("error.path.missing")))
            )
          )
        )
      )
      implicit val addTransactionCommand = AddTransactionCommand(
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
      implicit val id = NumericCorrelationId(1)
      implicit val jsonRpcRequestMessage = JsonRpcRequestMessage(
        "addTransaction",
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
      it should behave like commandRead
      it should behave like commandWrite
    }
  }

  describe("A Response")(
    describe("of type AddTransactionResponse") {
      implicit val addTransactionResponse = AddTransactionResponse(
        created = 1434115187612L
      )
      implicit val id = NumericCorrelationId(1)
      implicit val jsonRpcResponseMessage = JsonRpcResponseSuccessMessage(
        Json.obj(
          "created" -> 1434115187612L
        ),
        NumericCorrelationId(1)
      )
      implicit val method = "addTransaction"
      it should behave like responseRead
      it should behave like responseWrite
    }
  )

  describe("A Notification") {
    describe("with an invalid method")(
      it should behave like notificationReadError(
        JsonRpcNotificationMessage(
          method = "invalidMethod",
          Json.obj()
        ),
        JsError("unknown method invalidMethod")
      )
    )
    describe("of type TransactionAddedNotification") {
      describe("with params of the wrong type")(
        it should behave like notificationReadError(
          JsonRpcNotificationMessage(
            method = "transactionAdded",
            Json.arr()
          ),
          JsError(__, "notification parameters must be named")
        )
      )
      describe("with empty params") {
        it should behave like notificationReadError(
          JsonRpcNotificationMessage(
            method = "transactionAdded",
            Json.obj()
          ),
          JsError(__ \ "transaction", "error.path.missing")
        )
      }
      implicit val clientJoinedZoneNotification = TransactionAddedNotification(
        Transaction(
          from = 0,
          to = 1,
          value = BigDecimal(1000000),
          created = 1434115187612L
        )
      )
      implicit val jsonRpcNotificationMessage = JsonRpcNotificationMessage(
        method = "transactionAdded",
        params = Json.obj(
          "transaction" -> Json.parse("""{"from":0,"to":1,"value":1000000,"created":1434115187612}""")
        )
      )
      it should behave like notificationRead
      it should behave like notificationWrite
    }
  }

  private[this] def commandReadError(jsonRpcRequestMessage: JsonRpcRequestMessage, jsError: JsError) =
    it(s"should fail to decode with error $jsError") {
      val jsResult = Command.read(jsonRpcRequestMessage)
      jsResult should equal(jsError)(after being ordered[Command])
    }

  private[this] def commandRead(implicit jsonRpcRequestMessage: JsonRpcRequestMessage, command: Command) =
    it(s"should decode to $command")(
      Command.read(jsonRpcRequestMessage) should be(JsSuccess(command))
    )

  private[this] def commandWrite(implicit command: Command,
                                 id: CorrelationId,
                                 jsonRpcRequestMessage: JsonRpcRequestMessage) =
    it(s"should encode to $jsonRpcRequestMessage")(
      Command.write(command, id) should be(jsonRpcRequestMessage)
    )

  private[this] def responseRead(implicit jsonRpcResponseMessage: JsonRpcResponseSuccessMessage,
                                 method: String,
                                 response: ResultResponse) =
    it(s"should decode to $response")(
      Response.read(jsonRpcResponseMessage, method) should be(JsSuccess(response))
    )

  private[this] def responseWrite(implicit response: ResultResponse,
                                  id: CorrelationId,
                                  jsonRpcResponseMessage: JsonRpcResponseSuccessMessage) =
    it(s"should encode to $jsonRpcResponseMessage")(
      Response.write(response, id) should be(jsonRpcResponseMessage)
    )

  private[this] def notificationReadError(jsonRpcNotificationMessage: JsonRpcNotificationMessage, jsError: JsError) =
    it(s"should fail to decode with error $jsError") {
      val notificationJsResult = Notification.read(jsonRpcNotificationMessage)
      notificationJsResult should equal(jsError)(after being ordered[Notification])
    }

  private[this] def notificationRead(implicit jsonRpcNotificationMessage: JsonRpcNotificationMessage,
                                     notification: Notification) =
    it(s"should decode to $notification")(
      Notification.read(jsonRpcNotificationMessage) should be(JsSuccess(notification))
    )

  private[this] def notificationWrite(implicit notification: Notification,
                                      jsonRpcNotificationMessage: JsonRpcNotificationMessage) =
    it(s"should encode to $jsonRpcNotificationMessage")(
      Notification.write(notification) should be(jsonRpcNotificationMessage)
    )

  private[this] def ordered[A] = new JsResultUniformity[A]

}
