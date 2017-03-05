package com.dhpcs.jsonrpc

import com.dhpcs.json.FormatBehaviors
import org.scalatest.{FunSpec, Matchers}
import play.api.data.validation.ValidationError
import play.api.libs.json._

class JsonRpcMessageSpec extends FunSpec with FormatBehaviors[JsonRpcMessage] with Matchers {

  describe("An arbitrary JsValue")(
    it should behave like readError[JsonRpcMessage](
      Json.parse(
        """
          |{
          |}""".stripMargin
      ),
      JsError(
        List(
          (__,
           List(ValidationError(
             "not a valid request, request batch, response, response batch or notification message"
           )))))
    )
  )

  describe("A JsonRpcRequestMessage") {
    describe("with an incorrect version")(
      it should behave like readError[JsonRpcRequestMessage](
        Json.parse(
          """
            |{
            |  "jsonrpc":"3.0",
            |  "method":"testMethod",
            |  "params":{"param1":"param1","param2":"param2"},
            |  "id":0
            |}""".stripMargin
        ),
        JsError(List((__ \ "jsonrpc", List(ValidationError("error.invalid")))))
      )
    )
    describe("with version of the wrong type")(
      it should behave like readError[JsonRpcRequestMessage](
        Json.parse("""
            |{
            |  "jsonrpc":2.0,
            |  "method":"testMethod",
            |  "params":{"param1":"param1","param2":"param2"},
            |  "id":0
            |}""".stripMargin),
        JsError(List((__ \ "jsonrpc", List(ValidationError("error.expected.jsstring")))))
      )
    )
    describe("without a version")(
      it should behave like readError[JsonRpcRequestMessage](
        Json.parse(
          """
            |{
            |  "method":"testMethod",
            |  "params":{"param1":"param1","param2":"param2"},
            |  "id":0
            |}""".stripMargin
        ),
        JsError(List((__ \ "jsonrpc", List(ValidationError("error.path.missing")))))
      )
    )
    describe("with method of the wrong type")(
      it should behave like readError[JsonRpcRequestMessage](
        Json.parse(
          """
            |{
            |  "jsonrpc":"2.0",
            |  "method":3.0,
            |  "params":{"param1":"param1","param2":"param2"},
            |  "id":0
            |}""".stripMargin
        ),
        JsError(List((__ \ "method", List(ValidationError("error.expected.jsstring")))))
      )
    )
    describe("without a method")(
      it should behave like readError[JsonRpcRequestMessage](
        Json.parse(
          """
            |{
            |  "jsonrpc":"2.0",
            |  "params":{"param1":"param1","param2":"param2"},
            |  "id":0
            |}""".stripMargin
        ),
        JsError(List((__ \ "method", List(ValidationError("error.path.missing")))))
      )
    )
    describe("with params of the wrong type")(
      it should behave like readError[JsonRpcRequestMessage](
        Json.parse(
          """
            |{
            |  "jsonrpc":"2.0",
            |  "method":"testMethod",
            |  "params":"params",
            |  "id":0
            |}""".stripMargin
        ),
        JsError(List((__ \ "params", List(ValidationError("error.expected.jsarray")))))
      )
    )
    describe("without params") {
      implicit val jsonRpcRequestMessage = JsonRpcRequestMessage(
        "testMethod",
        None,
        Some(Right(1))
      )
      implicit val jsonRpcRequestMessageJson = Json.parse(
        """
        |{
        |  "jsonrpc":"2.0",
        |  "method":"testMethod",
        |  "id":1
        |}""".stripMargin
      )
      it should behave like read
      it should behave like write
    }
    describe("without an id")(
      it should behave like readError[JsonRpcRequestMessage](
        Json.parse(
          """
            |{
            |  "jsonrpc":"2.0",
            |  "method":"testMethod",
            |  "params":{"param1":"param1","param2":"param2"}
            |}""".stripMargin
        ),
        JsError(List((__ \ "id", List(ValidationError("error.path.missing")))))
      )
    )
    describe("with a params array") {
      describe("and a null id") {
        implicit val jsonRpcRequestMessage = JsonRpcRequestMessage(
          method = "testMethod",
          params = Some(
            Left(
              JsArray(
                Seq(
                  JsString("param1"),
                  JsString("param2")
                )
              ))),
          id = None
        )
        implicit val jsonRpcRequestMessageJson = Json.parse(
          """
            |{
            |  "jsonrpc":"2.0",
            |  "method":"testMethod",
            |  "params":["param1","param2"],
            |  "id":null
            |}""".stripMargin
        )
        it should behave like read
        it should behave like write
      }
      describe("and a string id") {
        implicit val jsonRpcRequestMessage = JsonRpcRequestMessage(
          method = "testMethod",
          params = Some(
            Left(
              JsArray(
                Seq(
                  JsString("param1"),
                  JsString("param2")
                )
              ))),
          id = Some(Left("one"))
        )
        implicit val jsonRpcRequestMessageJson = Json.parse(
          """
            |{
            |  "jsonrpc":"2.0",
            |  "method":"testMethod",
            |  "params":["param1","param2"],
            |  "id":"one"
            |}""".stripMargin
        )
        it should behave like read
        it should behave like write
      }
      describe("and a numeric id") {
        implicit val jsonRpcRequestMessage = JsonRpcRequestMessage(
          method = "testMethod",
          params = Some(
            Left(
              JsArray(
                Seq(
                  JsString("param1"),
                  JsString("param2")
                )
              ))),
          id = Some(Right(1))
        )
        implicit val jsonRpcRequestMessageJson = Json.parse(
          """
            |{
            |  "jsonrpc":"2.0",
            |  "method":"testMethod",
            |  "params":["param1","param2"],
            |  "id":1
            |}""".stripMargin
        )
        it should behave like read
        it should behave like write
        describe("with a fractional part") {
          implicit val jsonRpcRequestMessage = JsonRpcRequestMessage(
            method = "testMethod",
            params = Some(
              Left(
                JsArray(
                  Seq(
                    JsString("param1"),
                    JsString("param2")
                  )
                ))),
            id = Some(Right(1.1))
          )
          implicit val jsonRpcRequestMessageJson = Json.parse(
            """{
              |  "jsonrpc":"2.0",
              |  "method":"testMethod",
              |  "params":["param1","param2"],
              |  "id":1.1
              |}""".stripMargin
          )
          it should behave like read
          it should behave like write
        }
      }
    }
    describe("with a params object") {
      describe("and a null id") {
        implicit val jsonRpcRequestMessage = JsonRpcRequestMessage(
          method = "testMethod",
          params = Some(
            Right(
              JsObject(
                Seq(
                  "param1" -> JsString("param1"),
                  "param2" -> JsString("param2")
                )
              ))),
          id = None
        )
        implicit val jsonRpcRequestMessageJson = Json.parse(
          """
            |{
            |  "jsonrpc":"2.0",
            |  "method":"testMethod",
            |  "params":{"param1":"param1","param2":"param2"},
            |  "id":null
            |}""".stripMargin
        )
        it should behave like read
        it should behave like write
      }
      describe("and a string id") {
        implicit val jsonRpcRequestMessage = JsonRpcRequestMessage(
          method = "testMethod",
          params = Some(
            Right(
              JsObject(
                Seq(
                  "param1" -> JsString("param1"),
                  "param2" -> JsString("param2")
                )
              ))),
          id = Some(Left("one"))
        )
        implicit val jsonRpcRequestMessageJson = Json.parse(
          """
            |{
            |  "jsonrpc":"2.0",
            |  "method":"testMethod",
            |  "params":{"param1":"param1","param2":"param2"},
            |  "id":"one"
            |}""".stripMargin
        )
        it should behave like read
        it should behave like write
      }
      describe("and a numeric id") {
        implicit val jsonRpcRequestMessage = JsonRpcRequestMessage(
          method = "testMethod",
          params = Some(
            Right(
              JsObject(
                Seq(
                  "param1" -> JsString("param1"),
                  "param2" -> JsString("param2")
                )
              ))),
          id = Some(Right(1))
        )
        implicit val jsonRpcRequestMessageJson = Json.parse(
          """
            |{
            |  "jsonrpc":"2.0",
            |  "method":"testMethod",
            |  "params":{"param1":"param1","param2":"param2"},
            |  "id":1
            |}""".stripMargin
        )
        it should behave like read
        it should behave like write
        describe("with a fractional part") {
          implicit val jsonRpcRequestMessage = JsonRpcRequestMessage(
            method = "testMethod",
            params = Some(
              Right(
                JsObject(
                  Seq(
                    "param1" -> JsString("param1"),
                    "param2" -> JsString("param2")
                  )
                ))),
            id = Some(Right(1.1))
          )
          implicit val jsonRpcRequestMessageJson = Json.parse(
            """
              |{
              |  "jsonrpc":"2.0",
              |  "method":"testMethod",
              |  "params":{"param1":"param1","param2":"param2"},
              |  "id":1.1
              |}""".stripMargin
          )
          it should behave like read
          it should behave like write
        }
      }
    }
  }

  describe("A JsonRpcRequestMessageBatch") {
    describe("with no content")(
      it should behave like readError[JsonRpcRequestMessageBatch](
        Json.parse(
          """
            |[
            |]""".stripMargin
        ),
        JsError(List((__, List(ValidationError("error.invalid")))))
      )
    )
    describe("with an invalid request")(
      it should behave like readError[JsonRpcRequestMessageBatch](
        Json.parse(
          """
            |[
            |  {
            |    "jsonrpc":"2.0",
            |    "params":{"param1":"param1","param2":"param2"},
            |    "id":1
            |  }
            |]""".stripMargin
        ),
        JsError(List((__(0) \ "method", List(ValidationError("error.path.missing")))))
      )
    )
    describe("with a single request") {
      implicit val jsonRpcRequestMessageBatch = JsonRpcRequestMessageBatch(
        Seq(
          Right(
            JsonRpcRequestMessage(
              method = "testMethod",
              params = Some(
                Right(
                  JsObject(
                    Seq(
                      "param1" -> JsString("param1"),
                      "param2" -> JsString("param2")
                    )
                  ))),
              id = Some(Right(1))
            ))
        )
      )
      implicit val jsonRpcRequestMessageBatchJson = Json.parse(
        """[
          |  {
          |    "jsonrpc":"2.0",
          |    "method":"testMethod",
          |    "params":{"param1":"param1","param2":"param2"},
          |    "id":1
          |  }
          |]""".stripMargin
      )
      it should behave like read
      it should behave like write
    }
    describe("with an invalid notification")(
      it should behave like readError[JsonRpcRequestMessageBatch](
        Json.parse(
          """
            |[
            |  {
            |    "jsonrpc":"2.0"
            |  }
            |]""".stripMargin
        ),
        JsError(List((__(0) \ "method", List(ValidationError("error.path.missing")))))
      )
    )
    describe("with a single notification") {
      implicit val jsonRpcRequestMessageBatch = JsonRpcRequestMessageBatch(
        Seq(
          Left(
            JsonRpcNotificationMessage(method = "testMethod",
                                       params = Some(
                                         Right(
                                           JsObject(
                                             Seq(
                                               "param1" -> JsString("param1"),
                                               "param2" -> JsString("param2")
                                             )
                                           )
                                         )
                                       )))
        )
      )
      implicit val jsonRpcRequestMessageBatchJson = Json.parse(
        """
          |[
          |  {
          |    "jsonrpc":"2.0",
          |    "method":"testMethod",
          |    "params":{"param1":"param1","param2":"param2"}
          |  }
          |]""".stripMargin
      )
      it should behave like read
      it should behave like write
    }
  }

  describe("A JsonRpcResponseMessage") {
    describe("with an incorrect version") {
      it should behave like readError[JsonRpcResponseMessage](
        Json.parse(
          """
            |{
            |  "jsonrpc":"3.0",
            |  "result":{"param1":"param1","param2":"param2"},
            |  "id":0
            |}""".stripMargin
        ),
        JsError(List((__ \ "jsonrpc", List(ValidationError("error.invalid")))))
      )
    }
    describe("with version of the wrong type")(
      it should behave like readError[JsonRpcResponseMessage](
        Json.parse(
          """
            |{
            |  "jsonrpc":2.0,
            |  "result":{"param1":"param1","param2":"param2"},
            |  "id":0
            |}""".stripMargin
        ),
        JsError(List((__ \ "jsonrpc", List(ValidationError("error.expected.jsstring")))))
      )
    )
    describe("without a version")(
      it should behave like readError[JsonRpcResponseMessage](
        Json.parse(
          """
            |{
            |  "result":{"param1":"param1","param2":"param2"},
            |  "id":0
            |}""".stripMargin
        ),
        JsError(List((__ \ "jsonrpc", List(ValidationError("error.path.missing")))))
      )
    )
    describe("with an error of the wrong type")(
      it should behave like readError[JsonRpcResponseMessage](
        Json.parse(
          """
            |{
            |  "jsonrpc":"2.0",
            |  "error":"error",
            |  "id":0
            |}""".stripMargin
        ),
        JsError(
          List(
            (__ \ "error" \ "code", List(ValidationError("error.path.missing"))),
            (__ \ "error" \ "message", List(ValidationError("error.path.missing")))
          ))
      )
    )
    describe("without an error or a result")(
      it should behave like readError[JsonRpcResponseMessage](
        Json.parse(
          """
            |{
            |  "jsonrpc":"2.0",
            |  "id":0
            |}""".stripMargin
        ),
        JsError(List((__ \ "error", List(ValidationError("error.path.missing")))))
      )
    )
    describe("without an id") {
      it should behave like readError[JsonRpcResponseMessage](
        Json.parse(
          """
            |{
            |  "jsonrpc":"2.0",
            |  "result":{"param1":"param1","param2":"param2"}
            |}""".stripMargin
        ),
        JsError(List((__ \ "id", List(ValidationError("error.path.missing")))))
      )
    }
    describe("with an error") {
      describe("and a null id") {
        implicit val jsonRpcResponseMessage = JsonRpcResponseMessage(
          errorOrResult = Left(JsonRpcResponseError.internalError(None)),
          id = None
        )
        implicit val jsonRpcResponseMessageJson = Json.parse(
          """
            |{
            |  "jsonrpc":"2.0",
            |  "error":{"code":-32603,"message":"Invalid params","data":{"meaning":"Internal JSON-RPC error."}},
            |  "id":null
            |}""".stripMargin
        )
        it should behave like read
        it should behave like write
      }
      describe("and a string id") {
        implicit val jsonRpcResponseMessage = JsonRpcResponseMessage(
          errorOrResult = Left(JsonRpcResponseError.internalError(None)),
          id = Some(Left("one"))
        )
        implicit val jsonRpcResponseMessageJson = Json.parse(
          """
            |{
            |  "jsonrpc":"2.0",
            |  "error":{"code":-32603,"message":"Invalid params","data":{"meaning":"Internal JSON-RPC error."}},
            |  "id":"one"
            |}""".stripMargin
        )
        it should behave like read
        it should behave like write
      }
      describe("and a numeric id") {
        implicit val jsonRpcResponseMessage = JsonRpcResponseMessage(
          errorOrResult = Left(JsonRpcResponseError.internalError(None)),
          id = Some(Right(1))
        )
        implicit val jsonRpcResponseMessageJson = Json.parse(
          """
            |{
            |  "jsonrpc":"2.0",
            |  "error":{"code":-32603,"message":"Invalid params","data":{"meaning":"Internal JSON-RPC error."}},
            |  "id":1
            |}""".stripMargin
        )
        it should behave like read
        it should behave like write
        describe("with a fractional part") {
          implicit val jsonRpcResponseMessage = JsonRpcResponseMessage(
            errorOrResult = Left(JsonRpcResponseError.internalError(None)),
            id = Some(Right(1.1))
          )
          implicit val jsonRpcResponseMessageJson = Json.parse(
            """
              |{
              |  "jsonrpc":"2.0",
              |  "error":{"code":-32603,"message":"Invalid params","data":{"meaning":"Internal JSON-RPC error."}},
              |  "id":1.1
              |}""".stripMargin
          )
          it should behave like read
          it should behave like write
        }
      }
    }
    describe("with a result") {
      describe("and a null id") {
        implicit val jsonRpcResponseMessage = JsonRpcResponseMessage(
          errorOrResult = Right(
            JsObject(
              Seq(
                "param1" -> JsString("param1"),
                "param2" -> JsString("param2")
              )
            )),
          id = None
        )
        implicit val jsonRpcResponseMessageJson = Json.parse(
          """
            |{
            |  "jsonrpc":"2.0",
            |  "result":{"param1":"param1","param2":"param2"},
            |  "id":null
            |}""".stripMargin
        )
        it should behave like read
        it should behave like write
      }
      describe("and a string id") {
        implicit val jsonRpcResponseMessage = JsonRpcResponseMessage(
          errorOrResult = Right(
            JsObject(
              Seq(
                "param1" -> JsString("param1"),
                "param2" -> JsString("param2")
              )
            )),
          id = Some(Left("one"))
        )
        implicit val jsonRpcResponseMessageJson = Json.parse(
          """
            |{
            |  "jsonrpc":"2.0",
            |  "result":{"param1":"param1","param2":"param2"},
            |  "id":"one"
            |}""".stripMargin
        )
        it should behave like read
        it should behave like write
      }
      describe("and a numeric id") {
        implicit val jsonRpcResponseMessage = JsonRpcResponseMessage(
          errorOrResult = Right(
            JsObject(
              Seq(
                "param1" -> JsString("param1"),
                "param2" -> JsString("param2")
              )
            )),
          id = Some(Right(1))
        )
        implicit val jsonRpcResponseMessageJson = Json.parse(
          """
            |{
            |  "jsonrpc":"2.0",
            |  "result":{"param1":"param1","param2":"param2"},
            |  "id":1
            |}""".stripMargin
        )
        it should behave like read
        it should behave like write
        describe("with a fractional part") {
          implicit val jsonRpcResponseMessage = JsonRpcResponseMessage(
            errorOrResult = Right(
              JsObject(
                Seq(
                  "param1" -> JsString("param1"),
                  "param2" -> JsString("param2")
                )
              )),
            id = Some(Right(1.1))
          )
          implicit val jsonRpcResponseMessageJson = Json.parse(
            """
              |{
              |  "jsonrpc":"2.0",
              |  "result":{"param1":"param1","param2":"param2"},
              |  "id":1.1
              |}""".stripMargin
          )
          it should behave like read
          it should behave like write
        }
      }
    }
  }

  describe("A JsonRpcResponseMessageBatch") {
    describe("with no content")(
      it should behave like readError[JsonRpcResponseMessageBatch](
        Json.parse(
          """
            |[
            |]""".stripMargin
        ),
        JsError(List((__, List(ValidationError("error.invalid")))))
      )
    )
    describe("with an invalid response")(
      it should behave like readError[JsonRpcResponseMessageBatch](
        Json.parse(
          """
            |[
            |  {
            |    "jsonrpc":"2.0",
            |    "id":1
            |  }
            |]""".stripMargin
        ),
        JsError(List((__(0) \ "error", List(ValidationError("error.path.missing")))))
      )
    )
    describe("with a single response") {
      implicit val jsonRpcResponseMessageBatch = JsonRpcResponseMessageBatch(
        Seq(
          JsonRpcResponseMessage(
            errorOrResult = Right(
              JsObject(
                Seq(
                  "param1" -> JsString("param1"),
                  "param2" -> JsString("param2")
                )
              )),
            id = Some(Right(1))
          )
        )
      )
      implicit val jsonRpcResponseMessageBatchJson = Json.parse(
        """
          |[
          |  {
          |    "jsonrpc":"2.0",
          |    "result":{"param1":"param1","param2":"param2"},
          |    "id":1
          |  }
          |]""".stripMargin
      )
      it should behave like read
      it should behave like write
    }
  }

  describe("A JsonRpcNotificationMessage") {
    describe("with an incorrect version")(
      it should behave like readError[JsonRpcNotificationMessage](
        Json.parse(
          """
            |{
            |  "jsonrpc":"3.0",
            |  "method":"testMethod",
            |  "params":{"param1":"param1","param2":"param2"}
            |}""".stripMargin
        ),
        JsError(List((__ \ "jsonrpc", List(ValidationError("error.invalid")))))
      )
    )
    describe("with version of the wrong type")(
      it should behave like readError[JsonRpcNotificationMessage](
        Json.parse(
          """
            |{
            |  "jsonrpc":2.0,
            |  "method":"testMethod",
            |  "params":{"param1":"param1","param2":"param2"}
            |}""".stripMargin
        ),
        JsError(List((__ \ "jsonrpc", List(ValidationError("error.expected.jsstring")))))
      )
    )
    describe("without a version")(
      it should behave like readError[JsonRpcNotificationMessage](
        Json.parse(
          """
            |{
            |  "method":"testMethod",
            |  "params":{"param1":"param1","param2":"param2"}
            |}""".stripMargin
        ),
        JsError(List((__ \ "jsonrpc", List(ValidationError("error.path.missing")))))
      )
    )
    describe("with method of the wrong type")(
      it should behave like readError[JsonRpcNotificationMessage](
        Json.parse(
          """
            |{
            |  "jsonrpc":"2.0",
            |  "method":3.0,
            |  "params":{"param1":"param1","param2":"param2"}
            |}""".stripMargin
        ),
        JsError(List((__ \ "method", List(ValidationError("error.expected.jsstring")))))
      )
    )
    describe("without a method")(
      it should behave like readError[JsonRpcNotificationMessage](
        Json.parse(
          """
            |{
            |  "jsonrpc":"2.0",
            |  "params":{"param1":"param1","param2":"param2"}
            |}""".stripMargin
        ),
        JsError(List((__ \ "method", List(ValidationError("error.path.missing")))))
      )
    )
    describe("with params of the wrong type")(
      it should behave like readError[JsonRpcNotificationMessage](
        Json.parse(
          """
            |{
            |  "jsonrpc":"2.0",
            |  "method":"testMethod",
            |  "params":"params"
            |}""".stripMargin
        ),
        JsError(List((__ \ "params", List(ValidationError("error.expected.jsarray")))))
      )
    )
    describe("without params") {
      implicit val jsonRpcNotificationMessage = JsonRpcNotificationMessage(
        method = "testMethod",
        params = None
      )
      implicit val jsonRpcNotificationMessageJson = Json.parse(
        """
          |{
          |  "jsonrpc":"2.0",
          |  "method":"testMethod"
          |}""".stripMargin
      )
      it should behave like read
      it should behave like write
    }
    describe("with a params array") {
      implicit val jsonRpcNotificationMessage = JsonRpcNotificationMessage(
        method = "testMethod",
        params = Some(
          Left(
            JsArray(
              Seq(
                JsString("param1"),
                JsString("param2")
              )
            )))
      )
      implicit val jsonRpcNotificationMessageJson = Json.parse(
        """
          |{
          |  "jsonrpc":"2.0",
          |  "method":"testMethod",
          |  "params":["param1","param2"]
          |}""".stripMargin
      )
      it should behave like read
      it should behave like write
    }
    describe("with a params object") {
      implicit val jsonRpcNotificationMessage = JsonRpcNotificationMessage(
        method = "testMethod",
        params = Some(
          Right(
            JsObject(
              Seq(
                "param1" -> JsString("param1"),
                "param2" -> JsString("param2")
              )
            )))
      )
      implicit val jsonRpcNotificationMessageJson = Json.parse(
        """
          |{
          |  "jsonrpc":"2.0",
          |  "method":"testMethod",
          |  "params":{"param1":"param1","param2":"param2"}
          |}""".stripMargin
      )
      it should behave like read
      it should behave like write
    }
  }
}
