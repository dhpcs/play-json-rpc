package com.dhpcs.jsonrpc

import com.dhpcs.jsonrpc.JsonRpcMessage._
import org.scalatest.{FreeSpec, Matchers}
import play.api.libs.json._

class JsonRpcMessageSpec extends FreeSpec with Matchers {

  "An arbitrary JsValue" - {
    val json = Json.parse(
      """
          |{
          |}""".stripMargin
    )
    val jsError = JsError("not a valid request, request batch, response, response batch or notification message")
    s"should fail to decode with error $jsError" in (
      Json.fromJson[JsonRpcMessage](json) shouldBe jsError
    )
  }

  "A JsonRpcRequestMessage" - {
    "with an incorrect version" - {
      val json = Json.parse(
        """
            |{
            |  "jsonrpc":"3.0",
            |  "method":"testMethod",
            |  "params":{"param1":"param1","param2":"param2"},
            |  "id":0
            |}""".stripMargin
      )
      val jsError = JsError(__ \ "jsonrpc", "error.invalid")
      s"should fail to decode with error $jsError" in (
        Json.fromJson[JsonRpcRequestMessage](json) shouldBe
          jsError
      )
    }
    "with version of the wrong type" - {
      val json    = Json.parse("""
            |{
            |  "jsonrpc":2.0,
            |  "method":"testMethod",
            |  "params":{"param1":"param1","param2":"param2"},
            |  "id":0
            |}""".stripMargin)
      val jsError = JsError(__ \ "jsonrpc", "error.expected.jsstring")
      s"should fail to decode with error $jsError" in (
        Json.fromJson[JsonRpcRequestMessage](json) shouldBe jsError
      )
    }
    "without a version" - {
      val json = Json.parse(
        """
            |{
            |  "method":"testMethod",
            |  "params":{"param1":"param1","param2":"param2"},
            |  "id":0
            |}""".stripMargin
      )
      val jsError = JsError(__ \ "jsonrpc", "error.path.missing")
      s"should fail to decode with error $jsError" in (
        Json.fromJson[JsonRpcRequestMessage](json) shouldBe jsError
      )
    }
    "with method of the wrong type" - {
      val json =
        Json.parse(
          """
            |{
            |  "jsonrpc":"2.0",
            |  "method":3.0,
            |  "params":{"param1":"param1","param2":"param2"},
            |  "id":0
            |}""".stripMargin
        )
      val jsError = JsError(__ \ "method", "error.expected.jsstring")
      s"should fail to decode with error $jsError" in (
        Json.fromJson[JsonRpcRequestMessage](json) shouldBe jsError
      )
    }
    "without a method" - {
      val json =
        Json.parse(
          """
            |{
            |  "jsonrpc":"2.0",
            |  "params":{"param1":"param1","param2":"param2"},
            |  "id":0
            |}""".stripMargin
        )
      val jsError = JsError(__ \ "method", "error.path.missing")
      s"should fail to decode with error $jsError" in (
        Json.fromJson[JsonRpcRequestMessage](json) shouldBe jsError
      )
    }
    "with params of the wrong type" - {
      val json = Json.parse(
        """
          |{
          |  "jsonrpc":"2.0",
          |  "method":"testMethod",
          |  "params":"params",
          |  "id":0
          |}""".stripMargin
      )
      val jsError = JsError(__ \ "params", "error.expected.jsobjectorjsarray")
      s"should fail to decode with error $jsError" in (
        Json.fromJson[JsonRpcRequestMessage](json) shouldBe jsError
      )
    }
    "without params" - {
      val jsonRpcRequestMessage = JsonRpcRequestMessage(
        "testMethod",
        NoParams,
        NumericCorrelationId(1)
      )
      val jsonRpcRequestMessageJson = Json.parse(
        """
          |{
          |  "jsonrpc":"2.0",
          |  "method":"testMethod",
          |  "id":1
          |}""".stripMargin
      )
      s"should decode to $jsonRpcRequestMessage" in (
        Json.fromJson(jsonRpcRequestMessageJson) shouldBe JsSuccess(jsonRpcRequestMessage)
      )
      s"should encode to $jsonRpcRequestMessageJson" in (
        Json.toJson(jsonRpcRequestMessage) shouldBe jsonRpcRequestMessageJson
      )
    }
    "without an id" - {
      val json = Json.parse(
        """
            |{
            |  "jsonrpc":"2.0",
            |  "method":"testMethod",
            |  "params":{"param1":"param1","param2":"param2"}
            |}""".stripMargin
      )
      val jsError = JsError(__ \ "id", "error.path.missing")
      s"should fail to decode with error $jsError" in (
        Json.fromJson[JsonRpcRequestMessage](json) shouldBe jsError
      )
    }
    "with a params array" - {
      "and a null id" - {
        val jsonRpcRequestMessage = JsonRpcRequestMessage(
          method = "testMethod",
          Json.arr(
            "param1",
            "param2"
          ),
          NoCorrelationId
        )
        val jsonRpcRequestMessageJson = Json.parse(
          """
            |{
            |  "jsonrpc":"2.0",
            |  "method":"testMethod",
            |  "params":["param1","param2"],
            |  "id":null
            |}""".stripMargin
        )
        s"should decode to $jsonRpcRequestMessage" in (
          Json.fromJson(jsonRpcRequestMessageJson) shouldBe JsSuccess(jsonRpcRequestMessage)
        )
        s"should encode to $jsonRpcRequestMessageJson" in (
          Json.toJson(jsonRpcRequestMessage) shouldBe jsonRpcRequestMessageJson
        )
      }
      "and a string id" - {
        val jsonRpcRequestMessage = JsonRpcRequestMessage(
          method = "testMethod",
          Json.arr(
            "param1",
            "param2"
          ),
          StringCorrelationId("one")
        )
        val jsonRpcRequestMessageJson = Json.parse(
          """
            |{
            |  "jsonrpc":"2.0",
            |  "method":"testMethod",
            |  "params":["param1","param2"],
            |  "id":"one"
            |}""".stripMargin
        )
        s"should decode to $jsonRpcRequestMessage" in (
          Json.fromJson(jsonRpcRequestMessageJson) shouldBe JsSuccess(jsonRpcRequestMessage)
        )
        s"should encode to $jsonRpcRequestMessageJson" in (
          Json.toJson(jsonRpcRequestMessage) shouldBe jsonRpcRequestMessageJson
        )
      }
      "and a numeric id" - {
        val jsonRpcRequestMessage = JsonRpcRequestMessage(
          method = "testMethod",
          Json.arr(
            "param1",
            "param2"
          ),
          NumericCorrelationId(1)
        )
        val jsonRpcRequestMessageJson = Json.parse(
          """
            |{
            |  "jsonrpc":"2.0",
            |  "method":"testMethod",
            |  "params":["param1","param2"],
            |  "id":1
            |}""".stripMargin
        )
        s"should decode to $jsonRpcRequestMessage" in (
          Json.fromJson(jsonRpcRequestMessageJson) shouldBe JsSuccess(jsonRpcRequestMessage)
        )
        s"should encode to $jsonRpcRequestMessageJson" in (
          Json.toJson(jsonRpcRequestMessage) shouldBe jsonRpcRequestMessageJson
        )
        "with a fractional part" - {
          val jsonRpcRequestMessage = JsonRpcRequestMessage(
            method = "testMethod",
            Json.arr(
              "param1",
              "param2"
            ),
            NumericCorrelationId(1.1)
          )
          val jsonRpcRequestMessageJson = Json.parse(
            """{
              |  "jsonrpc":"2.0",
              |  "method":"testMethod",
              |  "params":["param1","param2"],
              |  "id":1.1
              |}""".stripMargin
          )
          s"should decode to $jsonRpcRequestMessage" in (
            Json.fromJson(jsonRpcRequestMessageJson) shouldBe JsSuccess(jsonRpcRequestMessage)
          )
          s"should encode to $jsonRpcRequestMessageJson" in (
            Json.toJson(jsonRpcRequestMessage) shouldBe jsonRpcRequestMessageJson
          )
        }
      }
    }
    "with a params object" - {
      "and a null id" - {
        val jsonRpcRequestMessage = JsonRpcRequestMessage(
          method = "testMethod",
          Json.obj(
            "param1" -> "param1",
            "param2" -> "param2"
          ),
          NoCorrelationId
        )
        val jsonRpcRequestMessageJson = Json.parse(
          """
            |{
            |  "jsonrpc":"2.0",
            |  "method":"testMethod",
            |  "params":{"param1":"param1","param2":"param2"},
            |  "id":null
            |}""".stripMargin
        )
        s"should decode to $jsonRpcRequestMessage" in (
          Json.fromJson(jsonRpcRequestMessageJson) shouldBe JsSuccess(jsonRpcRequestMessage)
        )
        s"should encode to $jsonRpcRequestMessageJson" in (
          Json.toJson(jsonRpcRequestMessage) shouldBe jsonRpcRequestMessageJson
        )
      }
      "and a string id" - {
        val jsonRpcRequestMessage = JsonRpcRequestMessage(
          method = "testMethod",
          Json.obj(
            "param1" -> "param1",
            "param2" -> "param2"
          ),
          StringCorrelationId("one")
        )
        val jsonRpcRequestMessageJson = Json.parse(
          """
            |{
            |  "jsonrpc":"2.0",
            |  "method":"testMethod",
            |  "params":{"param1":"param1","param2":"param2"},
            |  "id":"one"
            |}""".stripMargin
        )
        s"should decode to $jsonRpcRequestMessage" in (
          Json.fromJson(jsonRpcRequestMessageJson) shouldBe JsSuccess(jsonRpcRequestMessage)
        )
        s"should encode to $jsonRpcRequestMessageJson" in (
          Json.toJson(jsonRpcRequestMessage) shouldBe jsonRpcRequestMessageJson
        )
      }
      "and a numeric id" - {
        val jsonRpcRequestMessage = JsonRpcRequestMessage(
          method = "testMethod",
          Json.obj(
            "param1" -> "param1",
            "param2" -> "param2"
          ),
          NumericCorrelationId(1)
        )
        val jsonRpcRequestMessageJson = Json.parse(
          """
            |{
            |  "jsonrpc":"2.0",
            |  "method":"testMethod",
            |  "params":{"param1":"param1","param2":"param2"},
            |  "id":1
            |}""".stripMargin
        )
        s"should decode to $jsonRpcRequestMessage" in (
          Json.fromJson(jsonRpcRequestMessageJson) shouldBe JsSuccess(jsonRpcRequestMessage)
        )
        s"should encode to $jsonRpcRequestMessageJson" in (
          Json.toJson(jsonRpcRequestMessage) shouldBe jsonRpcRequestMessageJson
        )
        "with a fractional part" - {
          val jsonRpcRequestMessage = JsonRpcRequestMessage(
            method = "testMethod",
            Json.obj(
              "param1" -> "param1",
              "param2" -> "param2"
            ),
            NumericCorrelationId(1.1)
          )
          val jsonRpcRequestMessageJson = Json.parse(
            """
              |{
              |  "jsonrpc":"2.0",
              |  "method":"testMethod",
              |  "params":{"param1":"param1","param2":"param2"},
              |  "id":1.1
              |}""".stripMargin
          )
          s"should decode to $jsonRpcRequestMessage" in (
            Json.fromJson(jsonRpcRequestMessageJson) shouldBe JsSuccess(jsonRpcRequestMessage)
          )
          s"should encode to $jsonRpcRequestMessageJson" in (
            Json.toJson(jsonRpcRequestMessage) shouldBe jsonRpcRequestMessageJson
          )
        }
      }
    }
  }

  "A JsonRpcRequestMessageBatch" - {
    "with no content" - {
      val json = Json.parse(
        """
            |[
            |]""".stripMargin
      )
      val jsError = JsError(__, "error.invalid")
      s"should fail to decode with error $jsError" in (
        Json.fromJson[JsonRpcRequestMessageBatch](json) shouldBe jsError
      )
    }
    "with an invalid request" - {
      val json = Json.parse(
        """
            |[
            |  {
            |    "jsonrpc":"2.0",
            |    "params":{"param1":"param1","param2":"param2"},
            |    "id":1
            |  }
            |]""".stripMargin
      )
      val jsError = JsError(__(0) \ "method", "error.path.missing")
      s"should fail to decode with error $jsError" in (
        Json.fromJson[JsonRpcRequestMessageBatch](json) shouldBe jsError
      )
    }
    "with a single request" - {
      val jsonRpcRequestMessageBatch = JsonRpcRequestMessageBatch(
        Seq(
          Right(
            JsonRpcRequestMessage(
              method = "testMethod",
              Json.obj(
                "param1" -> "param1",
                "param2" -> "param2"
              ),
              NumericCorrelationId(1)
            ))
        )
      )
      val jsonRpcRequestMessageBatchJson = Json.parse(
        """[
          |  {
          |    "jsonrpc":"2.0",
          |    "method":"testMethod",
          |    "params":{"param1":"param1","param2":"param2"},
          |    "id":1
          |  }
          |]""".stripMargin
      )
      s"should decode to $jsonRpcRequestMessageBatch" in (
        Json.fromJson(jsonRpcRequestMessageBatchJson) shouldBe JsSuccess(jsonRpcRequestMessageBatch)
      )
      s"should encode to $jsonRpcRequestMessageBatchJson" in (
        Json.toJson(jsonRpcRequestMessageBatch) shouldBe jsonRpcRequestMessageBatchJson
      )
    }
    "with an invalid notification" - {
      val json = Json.parse(
        """
            |[
            |  {
            |    "jsonrpc":"2.0"
            |  }
            |]""".stripMargin
      )
      val jsError = JsError(__(0) \ "method", "error.path.missing")
      s"should fail to decode with error $jsError" in (
        Json.fromJson[JsonRpcRequestMessageBatch](json) shouldBe jsError
      )
    }
    "with a single notification" - {
      val jsonRpcRequestMessageBatch = JsonRpcRequestMessageBatch(
        Seq(
          Left(
            JsonRpcNotificationMessage(
              method = "testMethod",
              Json.obj(
                "param1" -> "param1",
                "param2" -> "param2"
              )
            )
          )
        )
      )
      val jsonRpcRequestMessageBatchJson = Json.parse(
        """
          |[
          |  {
          |    "jsonrpc":"2.0",
          |    "method":"testMethod",
          |    "params":{"param1":"param1","param2":"param2"}
          |  }
          |]""".stripMargin
      )
      s"should decode to $jsonRpcRequestMessageBatch" in (
        Json.fromJson(jsonRpcRequestMessageBatchJson) shouldBe JsSuccess(jsonRpcRequestMessageBatch)
      )
      s"should encode to $jsonRpcRequestMessageBatchJson" in (
        Json.toJson(jsonRpcRequestMessageBatch) shouldBe jsonRpcRequestMessageBatchJson
      )
    }
  }

  "A JsonRpcResponseMessage" - {
    "with an incorrect version" - {
      val json = Json.parse(
        """
            |{
            |  "jsonrpc":"3.0",
            |  "result":{"param1":"param1","param2":"param2"},
            |  "id":0
            |}""".stripMargin
      )
      val jsError = JsError(__ \ "jsonrpc", "error.invalid")
      s"should fail to decode with error $jsError" in (
        Json.fromJson[JsonRpcResponseMessage](json) shouldBe jsError
      )
    }
    "with version of the wrong type" - {
      val json = Json.parse(
        """
            |{
            |  "jsonrpc":2.0,
            |  "result":{"param1":"param1","param2":"param2"},
            |  "id":0
            |}""".stripMargin
      )
      val jsError = JsError(__ \ "jsonrpc", "error.expected.jsstring")
      s"should fail to decode with error $jsError" in (
        Json.fromJson[JsonRpcResponseMessage](json) shouldBe jsError
      )
    }
    "without a version" - {
      val json = Json.parse(
        """
            |{
            |  "result":{"param1":"param1","param2":"param2"},
            |  "id":0
            |}""".stripMargin
      )
      val jsError = JsError(__ \ "jsonrpc", "error.path.missing")
      s"should fail to decode with error $jsError" in (
        Json.fromJson[JsonRpcResponseMessage](json) shouldBe jsError
      )
    }
    "with an error of the wrong type" - {
      val json = Json.parse(
        """
            |{
            |  "jsonrpc":"2.0",
            |  "error":"error",
            |  "id":0
            |}""".stripMargin
      )
      val jsError = JsError(
        Seq(
          (__ \ "error" \ "code", Seq(JsonValidationError("error.path.missing"))),
          (__ \ "error" \ "message", Seq(JsonValidationError("error.path.missing")))
        ))
      s"should fail to decode with error $jsError" in (
        Json.fromJson[JsonRpcResponseMessage](json) shouldBe jsError
      )
    }
    "without an error or a result" - {
      val json = Json.parse(
        """
            |{
            |  "jsonrpc":"2.0",
            |  "id":0
            |}""".stripMargin
      )
      val jsError = JsError(__ \ "result", "error.path.missing")
      s"should fail to decode with error $jsError" in (
        Json.fromJson[JsonRpcResponseMessage](json) shouldBe jsError
      )
    }
    "without an id" - {
      val json = Json.parse(
        """
            |{
            |  "jsonrpc":"2.0",
            |  "result":{"param1":"param1","param2":"param2"}
            |}""".stripMargin
      )
      val jsError = JsError(__ \ "id", "error.path.missing")
      s"should fail to decode with error $jsError" in (
        Json.fromJson[JsonRpcResponseMessage](json) shouldBe jsError
      )
    }
    "with a parse error" - {
      val jsonRpcResponseMessage = JsonRpcResponseErrorMessage.parseError(
        new Throwable("Boom"),
        NumericCorrelationId(1)
      )
      val jsonRpcResponseMessageJson = Json.parse(
        """
          |{
          |  "jsonrpc":"2.0",
          |  "error":{"code":-32700,"message":"Parse error","data":{"meaning":"Invalid JSON was received by the server.\nAn error occurred on the server while parsing the JSON text.","error":"Boom"}},
          |  "id":1
          |}""".stripMargin
      )
      s"should decode to $jsonRpcResponseMessage" in (
        Json.fromJson(jsonRpcResponseMessageJson) shouldBe JsSuccess(jsonRpcResponseMessage)
      )
      s"should encode to $jsonRpcResponseMessageJson" in (
        Json.toJson(jsonRpcResponseMessage) shouldBe jsonRpcResponseMessageJson
      )
    }
    "with an invalid request error" - {
      val jsonRpcResponseMessage = JsonRpcResponseErrorMessage.invalidRequest(
        error = JsError(__ \ "method", "error.path.missing"),
        NumericCorrelationId(1)
      )
      val jsonRpcResponseMessageJson = Json.parse(
        """
          |{
          |  "jsonrpc":"2.0",
          |  "error":{"code":-32600,"message":"Invalid Request","data":{"meaning":"The JSON sent is not a valid Request object.","error":{"obj.method":[{"msg":["error.path.missing"],"args":[]}]}}},
          |  "id":1
          |}""".stripMargin
      )
      s"should decode to $jsonRpcResponseMessage" in (
        Json.fromJson(jsonRpcResponseMessageJson) shouldBe JsSuccess(jsonRpcResponseMessage)
      )
      s"should encode to $jsonRpcResponseMessageJson" in (
        Json.toJson(jsonRpcResponseMessage) shouldBe jsonRpcResponseMessageJson
      )
    }
    "with a method not found error" - {
      val jsonRpcResponseMessage = JsonRpcResponseErrorMessage.methodNotFound(
        "foo",
        NumericCorrelationId(1)
      )
      val jsonRpcResponseMessageJson = Json.parse(
        """
          |{
          |  "jsonrpc":"2.0",
          |  "error":{"code":-32601,"message":"Method not found","data":{"meaning":"The method does not exist / is not available.","error":"The method \"foo\" is not implemented."}},
          |  "id":1
          |}""".stripMargin
      )
      s"should decode to $jsonRpcResponseMessage" in (
        Json.fromJson(jsonRpcResponseMessageJson) shouldBe JsSuccess(jsonRpcResponseMessage)
      )
      s"should encode to $jsonRpcResponseMessageJson" in (
        Json.toJson(jsonRpcResponseMessage) shouldBe jsonRpcResponseMessageJson
      )
    }
    "with an invalid params error" - {
      val jsonRpcResponseMessage = JsonRpcResponseErrorMessage.invalidParams(
        error = JsError(__ \ "arg1", "error.path.missing"),
        NumericCorrelationId(1)
      )
      val jsonRpcResponseMessageJson = Json.parse(
        """
          |{
          |  "jsonrpc":"2.0",
          |  "error":{"code":-32602,"message":"Invalid params","data":{"meaning":"Invalid method parameter(s).","error":{"obj.arg1":[{"msg":["error.path.missing"],"args":[]}]}}},
          |  "id":1
          |}""".stripMargin
      )
      s"should decode to $jsonRpcResponseMessage" in (
        Json.fromJson(jsonRpcResponseMessageJson) shouldBe JsSuccess(jsonRpcResponseMessage)
      )
      s"should encode to $jsonRpcResponseMessageJson" in (
        Json.toJson(jsonRpcResponseMessage) shouldBe jsonRpcResponseMessageJson
      )
    }
    "with an internal error" - {
      val jsonRpcResponseMessage = JsonRpcResponseErrorMessage.internalError(
        error = None,
        NumericCorrelationId(1)
      )
      val jsonRpcResponseMessageJson = Json.parse(
        """
          |{
          |  "jsonrpc":"2.0",
          |  "error":{"code":-32603,"message":"Internal error","data":{"meaning":"Internal JSON-RPC error."}},
          |  "id":1
          |}""".stripMargin
      )
      s"should decode to $jsonRpcResponseMessage" in (
        Json.fromJson(jsonRpcResponseMessageJson) shouldBe JsSuccess(jsonRpcResponseMessage)
      )
      s"should encode to $jsonRpcResponseMessageJson" in (
        Json.toJson(jsonRpcResponseMessage) shouldBe jsonRpcResponseMessageJson
      )
    }
    "with a server error" - {
      val jsonRpcResponseMessage = JsonRpcResponseErrorMessage.serverError(
        code = JsonRpcResponseErrorMessage.ServerErrorCodeFloor,
        error = None,
        NumericCorrelationId(1)
      )
      val jsonRpcResponseMessageJson = Json.parse(
        """
          |{
          |  "jsonrpc":"2.0",
          |  "error":{"code":-32099,"message":"Server error","data":{"meaning":"Something went wrong in the receiving application."}},
          |  "id":1
          |}""".stripMargin
      )
      s"should decode to $jsonRpcResponseMessage" in (
        Json.fromJson(jsonRpcResponseMessageJson) shouldBe JsSuccess(jsonRpcResponseMessage)
      )
      s"should encode to $jsonRpcResponseMessageJson" in (
        Json.toJson(jsonRpcResponseMessage) shouldBe jsonRpcResponseMessageJson
      )
    }
    "with an application error" - {
      val jsonRpcResponseMessage = JsonRpcResponseErrorMessage.applicationError(
        code = -31999,
        message = "Boom",
        data = None,
        NumericCorrelationId(1)
      )
      val jsonRpcResponseMessageJson = Json.parse(
        """
          |{
          |  "jsonrpc":"2.0",
          |  "error":{"code":-31999,"message":"Boom"},
          |  "id":1
          |}""".stripMargin
      )
      s"should decode to $jsonRpcResponseMessage" in (
        Json.fromJson(jsonRpcResponseMessageJson) shouldBe JsSuccess(jsonRpcResponseMessage)
      )
      s"should encode to $jsonRpcResponseMessageJson" in (
        Json.toJson(jsonRpcResponseMessage) shouldBe jsonRpcResponseMessageJson
      )
    }
    "with a result" - {
      "and a null id" - {
        val jsonRpcResponseMessage = JsonRpcResponseSuccessMessage(
          Json.obj(
            "param1" -> "param1",
            "param2" -> "param2"
          ),
          NoCorrelationId
        )
        val jsonRpcResponseMessageJson = Json.parse(
          """
            |{
            |  "jsonrpc":"2.0",
            |  "result":{"param1":"param1","param2":"param2"},
            |  "id":null
            |}""".stripMargin
        )
        s"should decode to $jsonRpcResponseMessage" in (
          Json.fromJson(jsonRpcResponseMessageJson) shouldBe JsSuccess(jsonRpcResponseMessage)
        )
        s"should encode to $jsonRpcResponseMessageJson" in (
          Json.toJson(jsonRpcResponseMessage) shouldBe jsonRpcResponseMessageJson
        )
      }
      "and a string id" - {
        val jsonRpcResponseMessage = JsonRpcResponseSuccessMessage(
          Json.obj(
            "param1" -> "param1",
            "param2" -> "param2"
          ),
          StringCorrelationId("one")
        )
        val jsonRpcResponseMessageJson = Json.parse(
          """
            |{
            |  "jsonrpc":"2.0",
            |  "result":{"param1":"param1","param2":"param2"},
            |  "id":"one"
            |}""".stripMargin
        )
        s"should decode to $jsonRpcResponseMessage" in (
          Json.fromJson(jsonRpcResponseMessageJson) shouldBe JsSuccess(jsonRpcResponseMessage)
        )
        s"should encode to $jsonRpcResponseMessageJson" in (
          Json.toJson(jsonRpcResponseMessage) shouldBe jsonRpcResponseMessageJson
        )
      }
      "and a numeric id" - {
        val jsonRpcResponseMessage = JsonRpcResponseSuccessMessage(
          Json.obj(
            "param1" -> "param1",
            "param2" -> "param2"
          ),
          NumericCorrelationId(1)
        )
        val jsonRpcResponseMessageJson = Json.parse(
          """
            |{
            |  "jsonrpc":"2.0",
            |  "result":{"param1":"param1","param2":"param2"},
            |  "id":1
            |}""".stripMargin
        )
        s"should decode to $jsonRpcResponseMessage" in (
          Json.fromJson(jsonRpcResponseMessageJson) shouldBe JsSuccess(jsonRpcResponseMessage)
        )
        s"should encode to $jsonRpcResponseMessageJson" in (
          Json.toJson(jsonRpcResponseMessage) shouldBe jsonRpcResponseMessageJson
        )
        "with a fractional part" - {
          val jsonRpcResponseMessage = JsonRpcResponseSuccessMessage(
            Json.obj(
              "param1" -> "param1",
              "param2" -> "param2"
            ),
            NumericCorrelationId(1.1)
          )
          val jsonRpcResponseMessageJson = Json.parse(
            """
              |{
              |  "jsonrpc":"2.0",
              |  "result":{"param1":"param1","param2":"param2"},
              |  "id":1.1
              |}""".stripMargin
          )
          s"should decode to $jsonRpcResponseMessage" in (
            Json.fromJson(jsonRpcResponseMessageJson) shouldBe JsSuccess(jsonRpcResponseMessage)
          )
          s"should encode to $jsonRpcResponseMessageJson" in (
            Json.toJson(jsonRpcResponseMessage) shouldBe jsonRpcResponseMessageJson
          )
        }
      }
    }
  }

  "A JsonRpcResponseMessageBatch" - {
    "with no content" - {
      val json = Json.parse(
        """
            |[
            |]""".stripMargin
      )
      val jsError = JsError(__, "error.invalid")
      s"should fail to decode with error $jsError" in (
        Json.fromJson[JsonRpcResponseMessageBatch](json) shouldBe jsError
      )
    }
    "with an invalid response" - {
      val json = Json.parse(
        """
            |[
            |  {
            |    "jsonrpc":"2.0",
            |    "id":1
            |  }
            |]""".stripMargin
      )
      val jsError = JsError(__(0) \ "result", "error.path.missing")
      s"should fail to decode with error $jsError" in (
        Json.fromJson[JsonRpcResponseMessageBatch](json) shouldBe jsError
      )
    }
    "with a single response" - {
      val jsonRpcResponseMessageBatch = JsonRpcResponseMessageBatch(
        Seq(
          JsonRpcResponseSuccessMessage(
            Json.obj(
              "param1" -> "param1",
              "param2" -> "param2"
            ),
            NumericCorrelationId(1)
          )
        )
      )
      val jsonRpcResponseMessageBatchJson = Json.parse(
        """
          |[
          |  {
          |    "jsonrpc":"2.0",
          |    "result":{"param1":"param1","param2":"param2"},
          |    "id":1
          |  }
          |]""".stripMargin
      )
      s"should decode to $jsonRpcResponseMessageBatch" in (
        Json.fromJson(jsonRpcResponseMessageBatchJson) shouldBe JsSuccess(jsonRpcResponseMessageBatch)
      )
      s"should encode to $jsonRpcResponseMessageBatchJson" in (
        Json.toJson(jsonRpcResponseMessageBatch) shouldBe jsonRpcResponseMessageBatchJson
      )
    }
  }

  "A JsonRpcNotificationMessage" - {
    "with an incorrect version" - {
      val json = Json.parse(
        """
            |{
            |  "jsonrpc":"3.0",
            |  "method":"testMethod",
            |  "params":{"param1":"param1","param2":"param2"}
            |}""".stripMargin
      )
      val jsError = JsError(__ \ "jsonrpc", "error.invalid")
      s"should fail to decode with error $jsError" in (
        Json.fromJson[JsonRpcNotificationMessage](json) shouldBe jsError
      )
    }
    "with version of the wrong type" - {
      val json = Json.parse(
        """
            |{
            |  "jsonrpc":2.0,
            |  "method":"testMethod",
            |  "params":{"param1":"param1","param2":"param2"}
            |}""".stripMargin
      )
      val jsError = JsError(__ \ "jsonrpc", "error.expected.jsstring")
      s"should fail to decode with error $jsError" in (
        Json.fromJson[JsonRpcNotificationMessage](json) shouldBe jsError
      )
    }
    "without a version" - {
      val json = Json.parse(
        """
            |{
            |  "method":"testMethod",
            |  "params":{"param1":"param1","param2":"param2"}
            |}""".stripMargin
      )
      val jsError = JsError(__ \ "jsonrpc", "error.path.missing")
      s"should fail to decode with error $jsError" in (
        Json.fromJson[JsonRpcNotificationMessage](json) shouldBe jsError
      )
    }
    "with method of the wrong type" - {
      val json = Json.parse(
        """
            |{
            |  "jsonrpc":"2.0",
            |  "method":3.0,
            |  "params":{"param1":"param1","param2":"param2"}
            |}""".stripMargin
      )
      val jsError = JsError(__ \ "method", "error.expected.jsstring")
      s"should fail to decode with error $jsError" in (
        Json.fromJson[JsonRpcNotificationMessage](json) shouldBe jsError
      )
    }
    "without a method" - {
      val json = Json.parse(
        """
            |{
            |  "jsonrpc":"2.0",
            |  "params":{"param1":"param1","param2":"param2"}
            |}""".stripMargin
      )
      val jsError = JsError(__ \ "method", "error.path.missing")
      s"should fail to decode with error $jsError" in (
        Json.fromJson[JsonRpcNotificationMessage](json) shouldBe jsError
      )
    }
    "with params of the wrong type" - {
      val json = Json.parse(
        """
            |{
            |  "jsonrpc":"2.0",
            |  "method":"testMethod",
            |  "params":"params"
            |}""".stripMargin
      )
      val jsError = JsError(__ \ "params", "error.expected.jsobjectorjsarray")
      s"should fail to decode with error $jsError" in (
        Json.fromJson[JsonRpcNotificationMessage](json) shouldBe jsError
      )
    }
    "without params" - {
      val jsonRpcNotificationMessage = JsonRpcNotificationMessage(
        method = "testMethod",
        NoParams
      )
      val jsonRpcNotificationMessageJson = Json.parse(
        """
          |{
          |  "jsonrpc":"2.0",
          |  "method":"testMethod"
          |}""".stripMargin
      )
      s"should decode to $jsonRpcNotificationMessage" in (
        Json.fromJson(jsonRpcNotificationMessageJson) shouldBe JsSuccess(jsonRpcNotificationMessage)
      )
      s"should encode to $jsonRpcNotificationMessageJson" in (
        Json.toJson(jsonRpcNotificationMessage) shouldBe jsonRpcNotificationMessageJson
      )
    }
    "with a params array" - {
      val jsonRpcNotificationMessage = JsonRpcNotificationMessage(
        method = "testMethod",
        Json.arr(
          "param1",
          "param2"
        )
      )
      val jsonRpcNotificationMessageJson = Json.parse(
        """
          |{
          |  "jsonrpc":"2.0",
          |  "method":"testMethod",
          |  "params":["param1","param2"]
          |}""".stripMargin
      )
      s"should decode to $jsonRpcNotificationMessage" in (
        Json.fromJson(jsonRpcNotificationMessageJson) shouldBe JsSuccess(jsonRpcNotificationMessage)
      )
      s"should encode to $jsonRpcNotificationMessageJson" in (
        Json.toJson(jsonRpcNotificationMessage) shouldBe jsonRpcNotificationMessageJson
      )
    }
    "with a params object" - {
      val jsonRpcNotificationMessage = JsonRpcNotificationMessage(
        method = "testMethod",
        Json.obj(
          "param1" -> "param1",
          "param2" -> "param2"
        )
      )
      val jsonRpcNotificationMessageJson = Json.parse(
        """
          |{
          |  "jsonrpc":"2.0",
          |  "method":"testMethod",
          |  "params":{"param1":"param1","param2":"param2"}
          |}""".stripMargin
      )
      s"should decode to $jsonRpcNotificationMessage" in (
        Json.fromJson(jsonRpcNotificationMessageJson) shouldBe JsSuccess(jsonRpcNotificationMessage)
      )
      s"should encode to $jsonRpcNotificationMessageJson" in (
        Json.toJson(jsonRpcNotificationMessage) shouldBe jsonRpcNotificationMessageJson
      )
    }
  }
}
