package com.dhpcs.jsonrpc

import com.dhpcs.jsonrpc.JsonRpcMessage.{ArrayParams, CorrelationId, NoParams, ObjectParams}
import com.dhpcs.jsonrpc.ResponseCompanion.ErrorResponse
import play.api.libs.json._

import scala.language.existentials
import scala.reflect.ClassTag

trait CommandCompanion[A] {

  private[this] lazy val (methodReads, classWrites) = CommandFormats

  protected[this] val CommandFormats: (Map[String, Reads[_ <: A]], Map[Class[_], (String, OWrites[_ <: A])])

  def read(jsonRpcRequestMessage: JsonRpcRequestMessage): Option[JsResult[_ <: A]] =
    for (reads <- methodReads.get(jsonRpcRequestMessage.method))
      yield
        jsonRpcRequestMessage.params match {
          case NoParams       => JsError("command parameters must be given")
          case ArrayParams(_) => JsError("command parameters must be named")
          case ObjectParams(value) =>
            reads.reads(value) match {
              // We just do this to reset the path in the success case.
              case JsError(invalid)    => JsError(invalid)
              case JsSuccess(valid, _) => JsSuccess(valid)
            }
        }

  def write[B <: A](command: B, id: CorrelationId): JsonRpcRequestMessage = {
    val (method, writes) =
      classWrites.getOrElse(command.getClass, sys.error(s"No format found for ${command.getClass}"))
    val bWrites = writes.asInstanceOf[OWrites[B]]
    JsonRpcRequestMessage(method, ObjectParams(bWrites.writes(command)), id)
  }
}

trait ResponseCompanion[A] {

  private[this] lazy val (methodReads, classWrites) = ResponseFormats

  protected[this] val ResponseFormats: (Map[String, Reads[_ <: A]], Map[Class[_], (String, OWrites[_ <: A])])

  def read(jsonRpcResponseMessage: JsonRpcResponseMessage, method: String): JsResult[Either[ErrorResponse, _ <: A]] =
    jsonRpcResponseMessage.errorOrResult match {
      case Left(error) => JsSuccess(Left(ErrorResponse(error.code, error.message, error.data)))
      case Right(result) =>
        methodReads(method).reads(result).map(Right(_)) match {
          // We just do this to reset the path in the success case.
          case JsError(invalid)    => JsError(invalid)
          case JsSuccess(valid, _) => JsSuccess(valid)
        }
    }

  def write[B <: A](response: Either[ErrorResponse, B], id: CorrelationId): JsonRpcResponseMessage = {
    val errorOrResult = response match {
      case Left(ErrorResponse(code, message, data)) =>
        Left(
          JsonRpcResponseError.applicationError(code, message, data)
        )
      case Right(resultResponse) =>
        val (_, writes) =
          classWrites.getOrElse(resultResponse.getClass, sys.error(s"No format found for ${response.getClass}"))
        val bWrites = writes.asInstanceOf[OWrites[B]]
        Right(bWrites.writes(resultResponse))
    }
    JsonRpcResponseMessage(errorOrResult, id)
  }
}

object ResponseCompanion {

  case class ErrorResponse(code: Int, message: String, data: Option[JsValue] = None)

}

trait NotificationCompanion[A] {

  private[this] lazy val (methodReads, classWrites) = NotificationFormats

  protected[this] val NotificationFormats: (Map[String, Reads[_ <: A]], Map[Class[_], (String, OWrites[_ <: A])])

  def read(jsonRpcNotificationMessage: JsonRpcNotificationMessage): Option[JsResult[_ <: A]] =
    for (reads <- methodReads.get(jsonRpcNotificationMessage.method))
      yield
        jsonRpcNotificationMessage.params match {
          case NoParams       => JsError("command parameters must be given")
          case ArrayParams(_) => JsError("notification parameters must be named")
          case ObjectParams(value) =>
            reads.reads(value) match {
              // We just do this to reset the path in the success case.
              case JsError(invalid)    => JsError(invalid)
              case JsSuccess(valid, _) => JsSuccess(valid)
            }
        }

  def write[B <: A](notification: B): JsonRpcNotificationMessage = {
    val (method, writes) =
      classWrites.getOrElse(notification.getClass, sys.error(s"No format found for ${notification.getClass}"))
    val bWrites = writes.asInstanceOf[OWrites[B]]
    JsonRpcNotificationMessage(method, ObjectParams(bWrites.writes(notification)))
  }
}

object Message {

  implicit class MessageFormat[A: ClassTag](methodAndFormat: (String, Format[A])) {
    private[Message] val classTag         = implicitly[ClassTag[A]]
    private[Message] val (method, format) = methodAndFormat
  }

  object MessageFormats {
    def apply[A](messageFormats: MessageFormat[_ <: A]*)
      : (Map[String, Reads[_ <: A]], Map[Class[_], (String, OWrites[_ <: A])]) = {
      val methods = messageFormats.map(_.method)
      require(
        methods == methods.distinct,
        "Duplicate methods: " + methods.mkString(", ")
      )
      val classes = messageFormats.map(_.classTag.runtimeClass)
      require(
        classes == classes.distinct,
        "Duplicate classes: " + classes.mkString(", ")
      )
      val reads = messageFormats.map(
        messageFormat =>
          messageFormat.method ->
            messageFormat.format)
      val writes = messageFormats.map(
        messageFormat =>
          messageFormat.classTag.runtimeClass ->
            (messageFormat.method             -> messageFormat.format.asInstanceOf[OWrites[_ <: A]]))
      (reads.toMap, writes.toMap)
    }
  }

  def objectFormat[A](o: A): OFormat[A] = OFormat(
    _.validate[JsObject].map(_ => o),
    (_: A) => Json.obj()
  )

}
