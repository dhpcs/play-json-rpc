package com.dhpcs.jsonrpc

import play.api.data.validation.ValidationError
import play.api.libs.functional.syntax._
import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json.Reads.verifying
import play.api.libs.json._
import com.dhpcs.jsonrpc.JsonRpcMessage._

import scala.collection.Seq

sealed trait JsonRpcMessage

object JsonRpcMessage {

  final val Version = "2.0"

  sealed trait CorrelationId
  object CorrelationId {
    implicit final val CorrelationIdFormat: Format[CorrelationId] =
      new Format[CorrelationId] {
        override def reads(json: JsValue): JsResult[CorrelationId] = json match {
          case JsNull          => JsSuccess(NoCorrelationId)
          case JsString(value) => JsSuccess(StringCorrelationId(value))
          case JsNumber(value) => JsSuccess(NumericCorrelationId(value))
          case JsBoolean(_)    => JsError()
          case JsArray(_)      => JsError()
          case JsObject(_)     => JsError()
        }

        override def writes(correlationId: CorrelationId): JsValue = correlationId match {
          case NoCorrelationId             => JsNull
          case StringCorrelationId(value)  => JsString(value)
          case NumericCorrelationId(value) => JsNumber(value)
        }
      }
  }

  case object NoCorrelationId                        extends CorrelationId
  case class StringCorrelationId(value: String)      extends CorrelationId
  case class NumericCorrelationId(value: BigDecimal) extends CorrelationId

  implicit final val JsonRpcMessageFormat: Format[JsonRpcMessage] = new Format[JsonRpcMessage] {
    override def reads(json: JsValue): JsResult[JsonRpcMessage] =
      (
        __.read(JsonRpcRequestMessage.JsonRpcRequestMessageFormat).map(m => m: JsonRpcMessage) orElse
          __.read(JsonRpcRequestMessageBatch.JsonRpcRequestMessageBatchFormat).map(m => m: JsonRpcMessage) orElse
          __.read(JsonRpcResponseMessage.JsonRpcResponseMessageFormat).map(m => m: JsonRpcMessage) orElse
          __.read(JsonRpcResponseMessageBatch.JsonRpcResponseMessageBatchFormat).map(m => m: JsonRpcMessage) orElse
          __.read(JsonRpcNotificationMessage.JsonRpcNotificationMessageFormat).map(m => m: JsonRpcMessage)
      ).reads(json)
        .orElse(
          JsError("not a valid request, request batch, response, response batch or notification message")
        )

    override def writes(jsonRpcMessage: JsonRpcMessage): JsValue = jsonRpcMessage match {
      case jsonRpcRequestMessage: JsonRpcRequestMessage =>
        Json.toJson(jsonRpcRequestMessage)(JsonRpcRequestMessage.JsonRpcRequestMessageFormat)
      case jsonRpcRequestMessageBatch: JsonRpcRequestMessageBatch =>
        Json.toJson(jsonRpcRequestMessageBatch)(JsonRpcRequestMessageBatch.JsonRpcRequestMessageBatchFormat)
      case jsonRpcResponseMessage: JsonRpcResponseMessage =>
        Json.toJson(jsonRpcResponseMessage)(JsonRpcResponseMessage.JsonRpcResponseMessageFormat)
      case jsonRpcResponseMessageBatch: JsonRpcResponseMessageBatch =>
        Json.toJson(jsonRpcResponseMessageBatch)(JsonRpcResponseMessageBatch.JsonRpcResponseMessageBatchFormat)
      case jsonRpcNotificationMessage: JsonRpcNotificationMessage =>
        Json.toJson(jsonRpcNotificationMessage)(JsonRpcNotificationMessage.JsonRpcNotificationMessageFormat)
    }
  }

  implicit final val ParamsFormat: Format[Either[JsArray, JsObject]] = eitherValueFormat[JsArray, JsObject]

  def eitherObjectFormat[A: Format, B: Format](leftKey: String, rightKey: String): Format[Either[A, B]] =
    OFormat(
      (__ \ rightKey).read[B].map(b => Right(b): Either[A, B]) orElse
        (__ \ leftKey).read[A].map(a => Left(a): Either[A, B]),
      OWrites[Either[A, B]] {
        case Right(rightValue) => Json.obj(rightKey -> Json.toJson(rightValue))
        case Left(leftValue)   => Json.obj(leftKey  -> Json.toJson[A](leftValue))
      }
    )

  def eitherValueFormat[A: Format, B: Format]: Format[Either[A, B]] =
    Format(
      __.read[B].map(b => Right(b): Either[A, B]) orElse
        __.read[A].map(a => Left(a): Either[A, B]),
      Writes[Either[A, B]] {
        case Right(rightValue) => Json.toJson(rightValue)
        case Left(leftValue)   => Json.toJson(leftValue)
      }
    )

}

case class JsonRpcRequestMessage(method: String, params: Option[Either[JsArray, JsObject]], id: CorrelationId)
    extends JsonRpcMessage

object JsonRpcRequestMessage {
  implicit final val JsonRpcRequestMessageFormat: Format[JsonRpcRequestMessage] = (
    (__ \ "jsonrpc").format(verifying[String](_ == JsonRpcMessage.Version)) and
      (__ \ "method").format[String] and
      // formatNullable allows the key and value to be completely absent
      (__ \ "params").formatNullable[Either[JsArray, JsObject]] and
      // optionWithNull requires that the key is present but permits the value to be null
      (__ \ "id").format[CorrelationId]
  )(
    (_, method, params, id) => JsonRpcRequestMessage(method, params, id),
    jsonRpcRequestMessage =>
      (JsonRpcMessage.Version, jsonRpcRequestMessage.method, jsonRpcRequestMessage.params, jsonRpcRequestMessage.id)
  )
}

case class JsonRpcRequestMessageBatch(messages: Seq[Either[JsonRpcNotificationMessage, JsonRpcRequestMessage]])
    extends JsonRpcMessage {
  require(messages.nonEmpty)
}

object JsonRpcRequestMessageBatch {

  implicit final val RequestOrNotificationFormat: Format[Either[JsonRpcNotificationMessage, JsonRpcRequestMessage]] =
    eitherValueFormat[JsonRpcNotificationMessage, JsonRpcRequestMessage]

  implicit final val JsonRpcRequestMessageBatchFormat: Format[JsonRpcRequestMessageBatch] = Format(
    Reads(
      json =>
        Reads
          .of[Seq[Either[JsonRpcNotificationMessage, JsonRpcRequestMessage]]](verifying(_.nonEmpty))
          .reads(json)
          .map(JsonRpcRequestMessageBatch(_))
    ),
    Writes(
      a => Writes.of[Seq[Either[JsonRpcNotificationMessage, JsonRpcRequestMessage]]].writes(a.messages)
    )
  )
}

case class JsonRpcResponseMessage(errorOrResult: Either[JsonRpcResponseError, JsValue], id: CorrelationId)
    extends JsonRpcMessage

object JsonRpcResponseMessage {
  implicit final val JsonRpcResponseMessageFormat: Format[JsonRpcResponseMessage] = (
    (__ \ "jsonrpc").format(verifying[String](_ == JsonRpcMessage.Version)) and
      __.format(eitherObjectFormat[JsonRpcResponseError, JsValue]("error", "result")) and
      // optionWithNull requires that the key is present but permits the value to be null
      (__ \ "id").format[CorrelationId]
  )(
    (_, errorOrResult, id) => JsonRpcResponseMessage(errorOrResult, id),
    jsonRpcResponseMessage => (JsonRpcMessage.Version, jsonRpcResponseMessage.errorOrResult, jsonRpcResponseMessage.id)
  )
}

case class JsonRpcResponseMessageBatch(messages: Seq[JsonRpcResponseMessage]) extends JsonRpcMessage {
  require(messages.nonEmpty)
}

object JsonRpcResponseMessageBatch {
  implicit final val JsonRpcResponseMessageBatchFormat: Format[JsonRpcResponseMessageBatch] = Format(
    Reads(
      json =>
        Reads.of[Seq[JsonRpcResponseMessage]](verifying(_.nonEmpty)).reads(json).map(JsonRpcResponseMessageBatch(_))
    ),
    Writes(
      a => Writes.of[Seq[JsonRpcResponseMessage]].writes(a.messages)
    )
  )
}

case class JsonRpcNotificationMessage(method: String, params: Option[Either[JsArray, JsObject]]) extends JsonRpcMessage

object JsonRpcNotificationMessage {
  implicit final val JsonRpcNotificationMessageFormat: Format[JsonRpcNotificationMessage] = (
    (__ \ "jsonrpc").format(verifying[String](_ == JsonRpcMessage.Version)) and
      (__ \ "method").format[String] and
      (__ \ "params").formatNullable[Either[JsArray, JsObject]]
  )(
    (_, method, params) => JsonRpcNotificationMessage(method, params),
    jsonRpcNotificationMessage =>
      (JsonRpcMessage.Version, jsonRpcNotificationMessage.method, jsonRpcNotificationMessage.params)
  )
}

sealed abstract case class JsonRpcResponseError(code: Int, message: String, data: Option[JsValue])

object JsonRpcResponseError {

  implicit final val JsonRpcResponseErrorFormat: Format[JsonRpcResponseError] = (
    (__ \ "code").format[Int] and
      (__ \ "message").format[String] and
      // formatNullable allows the key and value to be completely absent
      (__ \ "data").formatNullable[JsValue]
  )(
    (code, message, data) => new JsonRpcResponseError(code, message, data) {},
    jsonRpcResponseError => (jsonRpcResponseError.code, jsonRpcResponseError.message, jsonRpcResponseError.data)
  )

  final val ReservedErrorCodeFloor: Int   = -32768
  final val ReservedErrorCodeCeiling: Int = -32000

  final val ParseErrorCode: Int         = -32700
  final val InvalidRequestCode: Int     = -32600
  final val MethodNotFoundCode: Int     = -32601
  final val InvalidParamsCode: Int      = -32602
  final val InternalErrorCode: Int      = -32603
  final val ServerErrorCodeFloor: Int   = -32099
  final val ServerErrorCodeCeiling: Int = -32000

  def parseError(exception: Throwable): JsonRpcResponseError = rpcError(
    ParseErrorCode,
    message = "Parse error",
    meaning = "Invalid JSON was received by the server.\nAn error occurred on the server while parsing the JSON text.",
    error = Some(JsString(exception.getMessage))
  )

  def invalidRequest(errors: Seq[(JsPath, Seq[ValidationError])]): JsonRpcResponseError = rpcError(
    InvalidRequestCode,
    message = "Invalid Request",
    meaning = "The JSON sent is not a valid Request object.",
    error = Some(JsError.toJson(errors))
  )

  def methodNotFound(method: String): JsonRpcResponseError = rpcError(
    MethodNotFoundCode,
    message = "Method not found",
    meaning = "The method does not exist / is not available.",
    error = Some(JsString(s"""The method "$method" is not implemented."""))
  )

  def invalidParams(errors: Seq[(JsPath, Seq[ValidationError])]): JsonRpcResponseError = rpcError(
    InvalidParamsCode,
    message = "Invalid params",
    meaning = "Invalid method parameter(s).",
    error = Some(JsError.toJson(errors))
  )

  def internalError(error: Option[JsValue] = None): JsonRpcResponseError = rpcError(
    InternalErrorCode,
    message = "Internal error",
    meaning = "Internal JSON-RPC error.",
    error
  )

  def serverError(code: Int, error: Option[JsValue] = None): JsonRpcResponseError = {
    require(code >= ServerErrorCodeFloor && code <= ServerErrorCodeCeiling)
    rpcError(
      code,
      message = "Server error",
      meaning = "Something went wrong in the receiving application.",
      error
    )
  }

  private def rpcError(code: Int, message: String, meaning: String, error: Option[JsValue]): JsonRpcResponseError =
    new JsonRpcResponseError(
      code,
      message,
      data = Some(
        Json.obj(
          ("meaning" -> meaning: (String, JsValueWrapper)) +:
            error.fold[Seq[(String, JsValueWrapper)]](ifEmpty = Nil)(
              error => Seq("error" -> error)
            ): _*
        )
      )
    ) {}

  def applicationError(code: Int, message: String, data: Option[JsValue] = None): JsonRpcResponseError = {
    require(code > ReservedErrorCodeCeiling || code < ReservedErrorCodeFloor)
    new JsonRpcResponseError(
      code,
      message,
      data
    ) {}
  }
}
