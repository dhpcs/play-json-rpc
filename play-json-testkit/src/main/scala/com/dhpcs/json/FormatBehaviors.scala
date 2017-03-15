package com.dhpcs.json

import org.scalatest.{FunSpecLike, Matchers}
import play.api.libs.json._

trait FormatBehaviors[A] { this: FunSpecLike with Matchers =>

  def readError[B <: A](json: JsValue, jsError: JsError)(implicit format: Format[B]): Unit =
    it(s"should fail to decode with error $jsError")(
      Json.fromJson(json)(format) should equal(jsError)
    )

  def read(implicit json: JsValue, a: A, format: Format[A]): Unit =
    it(s"should decode to $a")(
      Json.fromJson(json)(format) should be(JsSuccess(a))
    )

  def write(implicit a: A, json: JsValue, format: Format[A]): Unit =
    it(s"should encode to $json")(
      Json.toJson(a)(format) should be(json)
    )

}
