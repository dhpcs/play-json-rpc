package com.dhpcs.json

import org.scalactic.Uniformity
import play.api.libs.json.{JsError, JsResult}

class JsResultUniformity[A] extends Uniformity[JsResult[A]] {

  override def normalized(jsResult: JsResult[A]): JsResult[A] = jsResult match {
    case JsError(errors) =>
      JsError(
        errors.sortBy { case (jsPath, _) => jsPath.toJsonString }
      )
    case _ => jsResult
  }

  override def normalizedCanHandle(o: Any): Boolean = o.isInstanceOf[JsError]

  override def normalizedOrSame(o: Any): Any = o match {
    case jsError: JsError => normalized(jsError)
    case _                => o
  }
}
