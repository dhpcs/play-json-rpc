package com.dhpcs.jsonrpc

import com.dhpcs.json.JsResultUniformity
import com.dhpcs.jsonrpc.JsonRpcMessage.{CorrelationId, NumericCorrelationId}
import com.dhpcs.jsonrpc.Message.MessageFormats
import com.dhpcs.jsonrpc.MessageCompanionsSpec._
import com.dhpcs.jsonrpc.ResponseCompanion.ErrorResponse
import org.scalatest.OptionValues._
import org.scalatest._
import play.api.data.validation.ValidationError
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads.min
import play.api.libs.json._

object MessageCompanionsSpec {

  sealed trait Message

  sealed trait Command                              extends Message
  case class UpdateAccountCommand(account: Account) extends Command
  case class AddTransactionCommand(from: Int,
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

  sealed trait Response                            extends Message
  sealed trait ResultResponse                      extends Response
  case object UpdateAccountResponse                extends ResultResponse
  case class AddTransactionResponse(created: Long) extends ResultResponse

  object Response extends ResponseCompanion[ResultResponse] {
    override final val ResponseFormats = MessageFormats(
      "updateAccount"  -> Message.objectFormat(UpdateAccountResponse),
      "addTransaction" -> Json.format[AddTransactionResponse]
    )
  }

  sealed trait Notification                                         extends Message
  case class AccountUpdatedNotification(account: Account)           extends Notification
  case class TransactionAddedNotification(transaction: Transaction) extends Notification

  object Notification extends NotificationCompanion[Notification] {
    override final val NotificationFormats = MessageFormats(
      "accountUpdated"   -> Json.format[AccountUpdatedNotification],
      "transactionAdded" -> Json.format[TransactionAddedNotification]
    )
  }

  case class Account(id: Int, name: Option[String] = None, metadata: Option[JsObject] = None)

  object Account {
    implicit final val AccountFormat: Format[Account] = Json.format[Account]
  }

  case class Transaction(from: Int,
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
          params = Some(Right(Json.obj())),
          NumericCorrelationId(1)
        ),
        None
      )
    )
    describe("of type AddTransactionCommand") {
      describe("with params of the wrong type")(
        it should behave like commandReadError(
          JsonRpcRequestMessage(
            method = "addTransaction",
            params = Some(Left(Json.arr())),
            NumericCorrelationId(1)
          ),
          Some(
            JsError(
              List(
                (__, List(ValidationError("command parameters must be named")))
              )
            ))
        )
      )
      describe("with empty params")(
        it should behave like commandReadError(
          JsonRpcRequestMessage(
            method = "addTransaction",
            params = Some(Right(Json.obj())),
            NumericCorrelationId(1)
          ),
          Some(
            JsError(
              List(
                (__ \ "from", List(ValidationError("error.path.missing"))),
                (__ \ "to", List(ValidationError("error.path.missing"))),
                (__ \ "value", List(ValidationError("error.path.missing")))
              )
            ))
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
        Some(
          Right(
            Json.obj(
              "from"        -> 0,
              "to"          -> 1,
              "value"       -> BigDecimal(1000000),
              "description" -> "Property purchase",
              "metadata" -> Json.obj(
                "property" -> "The TARDIS"
              )
            )
          )),
        NumericCorrelationId(1)
      )
      it should behave like commandRead
      it should behave like commandWrite
    }
  }

  describe("A Response") {
    describe("of type AddTransactionResponse")(
      describe("with empty params")(
        it should behave like responseReadError(
          JsonRpcResponseMessage(
            errorOrResult = Right(Json.obj()),
            NumericCorrelationId(1)
          ),
          "addTransaction",
          JsError(
            List(
              (__ \ "created", List(ValidationError("error.path.missing")))
            )
          )
        )
      )
    )
    implicit val addTransactionResponse = Right(
      AddTransactionResponse(
        created = 1434115187612L
      ))
    implicit val id = NumericCorrelationId(1)
    implicit val jsonRpcResponseMessage = JsonRpcResponseMessage(
      Right(
        Json.obj(
          "created" -> 1434115187612L
        )
      ),
      NumericCorrelationId(1)
    )
    implicit val method = "addTransaction"
    it should behave like responseRead
    it should behave like responseWrite
  }

  describe("A Notification") {
    describe("with an invalid method")(
      it should behave like notificationReadError(
        JsonRpcNotificationMessage(
          method = "invalidMethod",
          params = Some(Right(Json.obj()))
        ),
        jsError = None
      )
    )
    describe("of type TransactionAddedNotification") {
      describe("with params of the wrong type")(
        it should behave like notificationReadError(
          JsonRpcNotificationMessage(
            method = "transactionAdded",
            params = Some(Left(Json.arr()))
          ),
          Some(
            JsError(
              List(
                (__, List(ValidationError("notification parameters must be named")))
              )
            ))
        )
      )
      describe("with empty params") {
        it should behave like notificationReadError(
          JsonRpcNotificationMessage(
            method = "transactionAdded",
            params = Some(Right(Json.obj()))
          ),
          Some(
            JsError(
              List(
                (__ \ "transaction", List(ValidationError("error.path.missing")))
              )
            ))
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
        params = Some(
          Right(
            Json.obj(
              "transaction" -> Json.parse("""{"from":0,"to":1,"value":1000000,"created":1434115187612}""")
            )
          )
        )
      )
      it should behave like notificationRead
      it should behave like notificationWrite
    }
  }

  private[this] def commandReadError(jsonRpcRequestMessage: JsonRpcRequestMessage, jsError: Option[JsError]) =
    it(s"should fail to decode with error $jsError") {
      val jsResult = Command.read(jsonRpcRequestMessage)
      jsError.fold(jsResult shouldBe empty)(
        jsResult.value should equal(_)(after being ordered[Command])
      )
    }

  private[this] def commandRead(implicit jsonRpcRequestMessage: JsonRpcRequestMessage, command: Command) =
    it(s"should decode to $command")(
      Command.read(jsonRpcRequestMessage) should be(Some(JsSuccess(command)))
    )

  private[this] def commandWrite(implicit command: Command,
                                 id: CorrelationId,
                                 jsonRpcRequestMessage: JsonRpcRequestMessage) =
    it(s"should encode to $jsonRpcRequestMessage")(
      Command.write(command, id) should be(jsonRpcRequestMessage)
    )

  private[this] def responseReadError(jsonRpcResponseMessage: JsonRpcResponseMessage,
                                      method: String,
                                      jsError: JsError) =
    it(s"should fail to decode with error $jsError")(
      (Response.read(jsonRpcResponseMessage, method)
        should equal(jsError))(after being ordered[Either[ErrorResponse, ResultResponse]])
    )

  private[this] def responseRead(implicit jsonRpcResponseMessage: JsonRpcResponseMessage,
                                 method: String,
                                 errorOrResponse: Either[ErrorResponse, ResultResponse]) =
    it(s"should decode to $errorOrResponse")(
      Response.read(jsonRpcResponseMessage, method) should be(JsSuccess(errorOrResponse))
    )

  private[this] def responseWrite(implicit errorOrResponse: Either[ErrorResponse, ResultResponse],
                                  id: CorrelationId,
                                  jsonRpcResponseMessage: JsonRpcResponseMessage) =
    it(s"should encode to $jsonRpcResponseMessage")(
      Response.write(errorOrResponse, id) should be(jsonRpcResponseMessage)
    )

  private[this] def notificationReadError(jsonRpcNotificationMessage: JsonRpcNotificationMessage,
                                          jsError: Option[JsError]) =
    it(s"should fail to decode with error $jsError") {
      val notificationJsResult = Notification.read(jsonRpcNotificationMessage)
      jsError.fold(notificationJsResult shouldBe empty)(
        notificationJsResult.value should equal(_)(after being ordered[Notification])
      )
    }

  private[this] def notificationRead(implicit jsonRpcNotificationMessage: JsonRpcNotificationMessage,
                                     notification: Notification) =
    it(s"should decode to $notification")(
      Notification.read(jsonRpcNotificationMessage) should be(Some(JsSuccess(notification)))
    )

  private[this] def notificationWrite(implicit notification: Notification,
                                      jsonRpcNotificationMessage: JsonRpcNotificationMessage) =
    it(s"should encode to $jsonRpcNotificationMessage")(
      Notification.write(notification) should be(jsonRpcNotificationMessage)
    )

  private[this] def ordered[A] = new JsResultUniformity[A]

}
