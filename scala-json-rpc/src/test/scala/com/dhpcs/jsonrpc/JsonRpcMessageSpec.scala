package com.dhpcs.jsonrpc

import com.dhpcs.json.FormatBehaviors
import com.dhpcs.jsonrpc.JsonRpcMessage._
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
      JsError("not a valid request, request batch, response, response batch or notification message")
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
        JsError(__ \ "jsonrpc", "error.invalid")
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
        JsError(__ \ "jsonrpc", "error.expected.jsstring")
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
        JsError(__ \ "jsonrpc", "error.path.missing")
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
        JsError(__ \ "method", "error.expected.jsstring")
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
        JsError(__ \ "method", "error.path.missing")
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
        JsError(__ \ "params", "error.expected.jsobjectorjsarray")
      )
    )
    describe("without params") {
      implicit val jsonRpcRequestMessage = JsonRpcRequestMessage(
        "testMethod",
        NoParams,
        NumericCorrelationId(1)
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
        JsError(__ \ "id", "error.path.missing")
      )
    )
    describe("with a params array") {
      describe("and a null id") {
        implicit val jsonRpcRequestMessage = JsonRpcRequestMessage(
          method = "testMethod",
          ArrayParams(
            Json.arr(
              JsString("param1"),
              JsString("param2")
            )),
          NoCorrelationId
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
          ArrayParams(
            Json.arr(
              JsString("param1"),
              JsString("param2")
            )),
          StringCorrelationId("one")
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
          ArrayParams(
            Json.arr(
              JsString("param1"),
              JsString("param2")
            )),
          NumericCorrelationId(1)
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
            ArrayParams(
              Json.arr(
                JsString("param1"),
                JsString("param2")
              )),
            NumericCorrelationId(1.1)
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
          ObjectParams(
            Json.obj(
              "param1" -> JsString("param1"),
              "param2" -> JsString("param2")
            )),
          NoCorrelationId
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
          ObjectParams(
            Json.obj(
              "param1" -> JsString("param1"),
              "param2" -> JsString("param2")
            )),
          StringCorrelationId("one")
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
          ObjectParams(
            Json.obj(
              "param1" -> JsString("param1"),
              "param2" -> JsString("param2")
            )
          ),
          NumericCorrelationId(1)
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
            ObjectParams(
              Json.obj(
                "param1" -> JsString("param1"),
                "param2" -> JsString("param2")
              )
            ),
            NumericCorrelationId(1.1)
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
        JsError(__, "error.invalid")
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
        JsError(__(0) \ "method", "error.path.missing")
      )
    )
    describe("with a single request") {
      implicit val jsonRpcRequestMessageBatch = JsonRpcRequestMessageBatch(
        Seq(
          Right(
            JsonRpcRequestMessage(
              method = "testMethod",
              ObjectParams(
                Json.obj(
                  "param1" -> JsString("param1"),
                  "param2" -> JsString("param2")
                )
              ),
              NumericCorrelationId(1)
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
        JsError(__(0) \ "method", "error.path.missing")
      )
    )
    describe("with a single notification") {
      implicit val jsonRpcRequestMessageBatch = JsonRpcRequestMessageBatch(
        Seq(
          Left(
            JsonRpcNotificationMessage(method = "testMethod",
                                       ObjectParams(
                                         Json.obj(
                                           "param1" -> JsString("param1"),
                                           "param2" -> JsString("param2")
                                         )
                                       ))
          )
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
        JsError(__ \ "jsonrpc", "error.invalid")
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
        JsError(__ \ "jsonrpc", "error.expected.jsstring")
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
        JsError(__ \ "jsonrpc", "error.path.missing")
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
          Seq(
            (__ \ "error" \ "code", Seq(ValidationError("error.path.missing"))),
            (__ \ "error" \ "message", Seq(ValidationError("error.path.missing")))
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
        JsError(__ \ "result", "error.path.missing")
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
        JsError(__ \ "id", "error.path.missing")
      )
    }
    describe("with a parse error") {
      implicit val jsonRpcResponseMessage = JsonRpcResponseErrorMessage.parseError(
        new Throwable("Boom"),
        NumericCorrelationId(1)
      )
      implicit val jsonRpcResponseMessageJson = Json.parse(
        """
          |{
          |  "jsonrpc":"2.0",
          |  "error":{"code":-32700,"message":"Parse error","data":{"meaning":"Invalid JSON was received by the server.\nAn error occurred on the server while parsing the JSON text.","error":"Boom"}},
          |  "id":1
          |}""".stripMargin
      )
      it should behave like read
      it should behave like write
    }
    describe("with an invalid request error") {
      implicit val jsonRpcResponseMessage = JsonRpcResponseErrorMessage.invalidRequest(
        error = JsError(__ \ "method", "error.path.missing"),
        NumericCorrelationId(1)
      )
      implicit val jsonRpcResponseMessageJson = Json.parse(
        """
          |{
          |  "jsonrpc":"2.0",
          |  "error":{"code":-32600,"message":"Invalid Request","data":{"meaning":"The JSON sent is not a valid Request object.","error":{"obj.method":[{"msg":["error.path.missing"],"args":[]}]}}},
          |  "id":1
          |}""".stripMargin
      )
      it should behave like read
      it should behave like write
    }
    describe("with a method not found error") {
      implicit val jsonRpcResponseMessage = JsonRpcResponseErrorMessage.methodNotFound(
        "foo",
        NumericCorrelationId(1)
      )
      implicit val jsonRpcResponseMessageJson = Json.parse(
        """
          |{
          |  "jsonrpc":"2.0",
          |  "error":{"code":-32601,"message":"Method not found","data":{"meaning":"The method does not exist / is not available.","error":"The method \"foo\" is not implemented."}},
          |  "id":1
          |}""".stripMargin
      )
      it should behave like read
      it should behave like write
    }
    describe("with an invalid params error") {
      implicit val jsonRpcResponseMessage = JsonRpcResponseErrorMessage.invalidParams(
        error = JsError(__ \ "arg1", "error.path.missing"),
        NumericCorrelationId(1)
      )
      implicit val jsonRpcResponseMessageJson = Json.parse(
        """
          |{
          |  "jsonrpc":"2.0",
          |  "error":{"code":-32602,"message":"Invalid params","data":{"meaning":"Invalid method parameter(s).","error":{"obj.arg1":[{"msg":["error.path.missing"],"args":[]}]}}},
          |  "id":1
          |}""".stripMargin
      )
      it should behave like read
      it should behave like write
    }
    describe("with an internal error") {
      implicit val jsonRpcResponseMessage = JsonRpcResponseErrorMessage.internalError(
        error = None,
        NumericCorrelationId(1)
      )
      implicit val jsonRpcResponseMessageJson = Json.parse(
        """
          |{
          |  "jsonrpc":"2.0",
          |  "error":{"code":-32603,"message":"Internal error","data":{"meaning":"Internal JSON-RPC error."}},
          |  "id":1
          |}""".stripMargin
      )
      it should behave like read
      it should behave like write
    }
    describe("with a server error") {
      implicit val jsonRpcResponseMessage = JsonRpcResponseErrorMessage.serverError(
        code = JsonRpcResponseErrorMessage.ServerErrorCodeFloor,
        error = None,
        NumericCorrelationId(1)
      )
      implicit val jsonRpcResponseMessageJson = Json.parse(
        """
          |{
          |  "jsonrpc":"2.0",
          |  "error":{"code":-32099,"message":"Server error","data":{"meaning":"Something went wrong in the receiving application."}},
          |  "id":1
          |}""".stripMargin
      )
      it should behave like read
      it should behave like write
    }
    describe("with an application error") {
      implicit val jsonRpcResponseMessage = JsonRpcResponseErrorMessage.applicationError(
        code = -31999,
        message = "Boom",
        data = None,
        NumericCorrelationId(1)
      )
      implicit val jsonRpcResponseMessageJson = Json.parse(
        """
          |{
          |  "jsonrpc":"2.0",
          |  "error":{"code":-31999,"message":"Boom"},
          |  "id":1
          |}""".stripMargin
      )
      it should behave like read
      it should behave like write
    }
    describe("with a result") {
      describe("and a null id") {
        implicit val jsonRpcResponseMessage = JsonRpcResponseSuccessMessage(
          JsObject(
            Seq(
              "param1" -> JsString("param1"),
              "param2" -> JsString("param2")
            )
          ),
          NoCorrelationId
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
        implicit val jsonRpcResponseMessage = JsonRpcResponseSuccessMessage(
          JsObject(
            Seq(
              "param1" -> JsString("param1"),
              "param2" -> JsString("param2")
            )
          ),
          StringCorrelationId("one")
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
        implicit val jsonRpcResponseMessage = JsonRpcResponseSuccessMessage(
          JsObject(
            Seq(
              "param1" -> JsString("param1"),
              "param2" -> JsString("param2")
            )
          ),
          NumericCorrelationId(1)
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
          implicit val jsonRpcResponseMessage = JsonRpcResponseSuccessMessage(
            JsObject(
              Seq(
                "param1" -> JsString("param1"),
                "param2" -> JsString("param2")
              )
            ),
            NumericCorrelationId(1.1)
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
        JsError(__, "error.invalid")
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
        JsError(__(0) \ "result", "error.path.missing")
      )
    )
    describe("with a single response") {
      implicit val jsonRpcResponseMessageBatch = JsonRpcResponseMessageBatch(
        Seq(
          JsonRpcResponseSuccessMessage(
            JsObject(
              Seq(
                "param1" -> JsString("param1"),
                "param2" -> JsString("param2")
              )
            ),
            NumericCorrelationId(1)
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
        JsError(__ \ "jsonrpc", "error.invalid")
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
        JsError(__ \ "jsonrpc", "error.expected.jsstring")
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
        JsError(__ \ "jsonrpc", "error.path.missing")
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
        JsError(__ \ "method", "error.expected.jsstring")
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
        JsError(__ \ "method", "error.path.missing")
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
        JsError(__ \ "params", "error.expected.jsobjectorjsarray")
      )
    )
    describe("without params") {
      implicit val jsonRpcNotificationMessage = JsonRpcNotificationMessage(
        method = "testMethod",
        NoParams
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
        ArrayParams(
          Json.arr(
            JsString("param1"),
            JsString("param2")
          )
        )
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
        ObjectParams(
          Json.obj(
            "param1" -> JsString("param1"),
            "param2" -> JsString("param2")
          )
        )
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
