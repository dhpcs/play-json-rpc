package com.dhpcs.jsonrpc

import com.dhpcs.jsonrpc.JsonRpcMessage._
import org.scalatest.FreeSpec
import play.api.libs.json._

import scala.collection.immutable.Seq

class JsonRpcMessageSpec extends FreeSpec {

  "An invalid JsValue" - {
    val json = Json.parse("{}")
    val jsError = JsError(
      "not a valid request, request batch, response, response batch or " +
        "notification message")
    s"fails to decode with error $jsError" in assert(
      Json.fromJson[JsonRpcMessage](json) === jsError
    )
  }

  "A JsonRpcRequestMessage" - {
    "with an incorrect version" - {
      val json = Json.parse(
        """
            |{
            |  "jsonrpc" : "3.0",
            |  "method" : "testMethod",
            |  "params" : { "param1" : "param1", "param2" : "param2" },
            |  "id" : 0
            |}""".stripMargin
      )
      val jsError = JsError(__ \ "jsonrpc", "error.invalid")
      s"fails to decode with error $jsError" in assert(
        Json.fromJson[JsonRpcRequestMessage](json) === jsError
      )
    }
    "with version of the wrong type" - {
      val json =
        Json.parse("""
            |{
            |  "jsonrpc" : 2.0,
            |  "method" : "testMethod",
            |  "params" : { "param1" : "param1", "param2" : "param2" },
            |  "id" : 0
            |}""".stripMargin)
      val jsError = JsError(__ \ "jsonrpc", "error.expected.jsstring")
      s"fails to decode with error $jsError" in assert(
        Json.fromJson[JsonRpcRequestMessage](json) === jsError
      )
    }
    "without a version" - {
      val json = Json.parse(
        """
            |{
            |  "method" : "testMethod",
            |  "params" : { "param1" : "param1", "param2" : "param2" },
            |  "id" : 0
            |}""".stripMargin
      )
      val jsError = JsError(__ \ "jsonrpc", "error.path.missing")
      s"fails to decode with error $jsError" in assert(
        Json.fromJson[JsonRpcRequestMessage](json) === jsError
      )
    }
    "with method of the wrong type" - {
      val json =
        Json.parse(
          """
            |{
            |  "jsonrpc" : "2.0",
            |  "method" : 3.0,
            |  "params" : { "param1" : "param1", "param2" : "param2" },
            |  "id" : 0
            |}""".stripMargin
        )
      val jsError = JsError(__ \ "method", "error.expected.jsstring")
      s"fails to decode with error $jsError" in assert(
        Json.fromJson[JsonRpcRequestMessage](json) === jsError
      )
    }
    "without a method" - {
      val json =
        Json.parse(
          """
            |{
            |  "jsonrpc" : "2.0",
            |  "params" : { "param1" : "param1", "param2" : "param2" },
            |  "id" : 0
            |}""".stripMargin
        )
      val jsError = JsError(__ \ "method", "error.path.missing")
      s"fails to decode with error $jsError" in assert(
        Json.fromJson[JsonRpcRequestMessage](json) === jsError
      )
    }
    "with params of the wrong type" - {
      val json = Json.parse(
        """
          |{
          |  "jsonrpc" : "2.0",
          |  "method" : "testMethod",
          |  "params" : "params",
          |  "id" : 0
          |}""".stripMargin
      )
      val jsError = JsError(__ \ "params", "error.expected.jsobjectorjsarray")
      s"fails to decode with error $jsError" in assert(
        Json.fromJson[JsonRpcRequestMessage](json) === jsError
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
          |  "jsonrpc" : "2.0",
          |  "method" : "testMethod",
          |  "id" : 1
          |}""".stripMargin
      )
      s"decodes to $jsonRpcRequestMessage" in assert(
        Json.fromJson(jsonRpcRequestMessageJson) === JsSuccess(
          jsonRpcRequestMessage)
      )
      s"encodes to $jsonRpcRequestMessageJson" in assert(
        Json.toJson(jsonRpcRequestMessage) === jsonRpcRequestMessageJson
      )
    }
    "without an id" - {
      val json = Json.parse(
        """
            |{
            |  "jsonrpc" : "2.0",
            |  "method" : "testMethod",
            |  "params" : { "param1" : "param1", "param2" : "param2" }
            |}""".stripMargin
      )
      val jsError = JsError(__ \ "id", "error.path.missing")
      s"fails to decode with error $jsError" in assert(
        Json.fromJson[JsonRpcRequestMessage](json) === jsError
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
            |  "jsonrpc" : "2.0",
            |  "method" : "testMethod",
            |  "params" : [ "param1", "param2" ],
            |  "id" : null
            |}""".stripMargin
        )
        s"decodes to $jsonRpcRequestMessage" in assert(
          Json.fromJson(jsonRpcRequestMessageJson) === JsSuccess(
            jsonRpcRequestMessage)
        )
        s"encodes to $jsonRpcRequestMessageJson" in assert(
          Json.toJson(jsonRpcRequestMessage) === jsonRpcRequestMessageJson
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
            |  "jsonrpc" : "2.0",
            |  "method" : "testMethod",
            |  "params" : [ "param1", "param2" ],
            |  "id" : "one"
            |}""".stripMargin
        )
        s"decodes to $jsonRpcRequestMessage" in assert(
          Json.fromJson(jsonRpcRequestMessageJson) === JsSuccess(
            jsonRpcRequestMessage)
        )
        s"encodes to $jsonRpcRequestMessageJson" in assert(
          Json.toJson(jsonRpcRequestMessage) === jsonRpcRequestMessageJson
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
            |  "jsonrpc" : "2.0",
            |  "method" : "testMethod",
            |  "params" : [ "param1", "param2" ],
            |  "id" : 1
            |}""".stripMargin
        )
        s"decodes to $jsonRpcRequestMessage" in assert(
          Json.fromJson(jsonRpcRequestMessageJson) === JsSuccess(
            jsonRpcRequestMessage)
        )
        s"encodes to $jsonRpcRequestMessageJson" in assert(
          Json.toJson(jsonRpcRequestMessage) === jsonRpcRequestMessageJson
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
              |  "jsonrpc" : "2.0",
              |  "method" : "testMethod",
              |  "params" : [ "param1", "param2" ],
              |  "id" : 1.1
              |}""".stripMargin
          )
          s"decodes to $jsonRpcRequestMessage" in assert(
            Json.fromJson(jsonRpcRequestMessageJson) === JsSuccess(
              jsonRpcRequestMessage)
          )
          s"encodes to $jsonRpcRequestMessageJson" in assert(
            Json.toJson(jsonRpcRequestMessage) === jsonRpcRequestMessageJson
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
            |  "jsonrpc" : "2.0",
            |  "method" : "testMethod",
            |  "params" : { "param1" : "param1", "param2" : "param2" },
            |  "id" : null
            |}""".stripMargin
        )
        s"decodes to $jsonRpcRequestMessage" in assert(
          Json.fromJson(jsonRpcRequestMessageJson) === JsSuccess(
            jsonRpcRequestMessage)
        )
        s"encodes to $jsonRpcRequestMessageJson" in assert(
          Json.toJson(jsonRpcRequestMessage) === jsonRpcRequestMessageJson
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
            |  "jsonrpc" : "2.0",
            |  "method" : "testMethod",
            |  "params" : { "param1" : "param1", "param2" : "param2" },
            |  "id" : "one"
            |}""".stripMargin
        )
        s"decodes to $jsonRpcRequestMessage" in assert(
          Json.fromJson(jsonRpcRequestMessageJson) === JsSuccess(
            jsonRpcRequestMessage)
        )
        s"encodes to $jsonRpcRequestMessageJson" in assert(
          Json.toJson(jsonRpcRequestMessage) === jsonRpcRequestMessageJson
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
            |  "jsonrpc" : "2.0",
            |  "method" : "testMethod",
            |  "params" : { "param1" : "param1", "param2" : "param2" },
            |  "id" : 1
            |}""".stripMargin
        )
        s"decodes to $jsonRpcRequestMessage" in assert(
          Json.fromJson(jsonRpcRequestMessageJson) === JsSuccess(
            jsonRpcRequestMessage)
        )
        s"encodes to $jsonRpcRequestMessageJson" in assert(
          Json.toJson(jsonRpcRequestMessage) === jsonRpcRequestMessageJson
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
              |  "jsonrpc" : "2.0",
              |  "method" : "testMethod",
              |  "params" : { "param1" : "param1", "param2" : "param2" },
              |  "id" : 1.1
              |}""".stripMargin
          )
          s"decodes to $jsonRpcRequestMessage" in assert(
            Json.fromJson(jsonRpcRequestMessageJson) === JsSuccess(
              jsonRpcRequestMessage)
          )
          s"encodes to $jsonRpcRequestMessageJson" in assert(
            Json.toJson(jsonRpcRequestMessage) === jsonRpcRequestMessageJson
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
      s"fails to decode with error $jsError" in assert(
        Json.fromJson[JsonRpcRequestMessageBatch](json) === jsError
      )
    }
    "with an invalid request" - {
      val json = Json.parse(
        """
            |[
            |  {
            |    "jsonrpc" : "2.0",
            |    "params" : { "param1" : "param1", "param2" : "param2" },
            |    "id" : 1
            |  }
            |]""".stripMargin
      )
      val jsError = JsError(__(0) \ "method", "error.path.missing")
      s"fails to decode with error $jsError" in assert(
        Json.fromJson[JsonRpcRequestMessageBatch](json) === jsError
      )
    }
    "with a single request" - {
      val jsonRpcRequestMessageBatch = JsonRpcRequestMessageBatch(
        Seq(
          JsonRpcRequestMessage(
            method = "testMethod",
            Json.obj(
              "param1" -> "param1",
              "param2" -> "param2"
            ),
            NumericCorrelationId(1)
          ))
      )
      val jsonRpcRequestMessageBatchJson = Json.parse(
        """[
          |  {
          |    "jsonrpc" : "2.0",
          |    "method" : "testMethod",
          |    "params" : { "param1" : "param1", "param2" : "param2" },
          |    "id" : 1
          |  }
          |]""".stripMargin
      )
      s"decodes to $jsonRpcRequestMessageBatch" in assert(
        Json.fromJson(jsonRpcRequestMessageBatchJson) === JsSuccess(
          jsonRpcRequestMessageBatch)
      )
      s"encodes to $jsonRpcRequestMessageBatchJson" in assert(
        Json
          .toJson(jsonRpcRequestMessageBatch) === jsonRpcRequestMessageBatchJson
      )
    }
    "with an invalid notification" - {
      val json = Json.parse(
        """
            |[
            |  {
            |    "jsonrpc" : "2.0"
            |  }
            |]""".stripMargin
      )
      val jsError = JsError(__(0) \ "method", "error.path.missing")
      s"fails to decode with error $jsError" in assert(
        Json.fromJson[JsonRpcRequestMessageBatch](json) === jsError
      )
    }
    "with a single notification" - {
      val jsonRpcRequestMessageBatch = JsonRpcRequestMessageBatch(
        Seq(
          JsonRpcNotificationMessage(
            method = "testMethod",
            Json.obj(
              "param1" -> "param1",
              "param2" -> "param2"
            )
          )
        )
      )
      val jsonRpcRequestMessageBatchJson = Json.parse(
        """
          |[
          |  {
          |    "jsonrpc" : "2.0",
          |    "method" : "testMethod",
          |    "params" : { "param1" : "param1", "param2" : "param2" }
          |  }
          |]""".stripMargin
      )
      s"decodes to $jsonRpcRequestMessageBatch" in assert(
        Json.fromJson(jsonRpcRequestMessageBatchJson) === JsSuccess(
          jsonRpcRequestMessageBatch)
      )
      s"encodes to $jsonRpcRequestMessageBatchJson" in assert(
        Json.toJson(jsonRpcRequestMessageBatch) ===
          jsonRpcRequestMessageBatchJson
      )
    }
  }

  "A JsonRpcResponseMessage" - {
    "with an incorrect version" - {
      val json = Json.parse(
        """
            |{
            |  "jsonrpc" : "3.0",
            |  "result" : { "param1" : "param1", "param2" : "param2" },
            |  "id" : 0
            |}""".stripMargin
      )
      val jsError = JsError(__ \ "jsonrpc", "error.invalid")
      s"fails to decode with error $jsError" in assert(
        Json.fromJson[JsonRpcResponseMessage](json) === jsError
      )
    }
    "with version of the wrong type" - {
      val json = Json.parse(
        """
            |{
            |  "jsonrpc" :2.0,
            |  "result" : { "param1" : "param1", "param2" : "param2" },
            |  "id" : 0
            |}""".stripMargin
      )
      val jsError = JsError(__ \ "jsonrpc", "error.expected.jsstring")
      s"fails to decode with error $jsError" in assert(
        Json.fromJson[JsonRpcResponseMessage](json) === jsError
      )
    }
    "without a version" - {
      val json = Json.parse(
        """
            |{
            |  "result" : { "param1" : "param1", "param2" : "param2" },
            |  "id" : 0
            |}""".stripMargin
      )
      val jsError = JsError(__ \ "jsonrpc", "error.path.missing")
      s"fails to decode with error $jsError" in assert(
        Json.fromJson[JsonRpcResponseMessage](json) === jsError
      )
    }
    "with an error of the wrong type" - {
      val json = Json.parse(
        """
            |{
            |  "jsonrpc" : "2.0",
            |  "error" : "error",
            |  "id" : 0
            |}""".stripMargin
      )
      val jsError = JsError(
        Seq(
          (__ \ "error" \ "code",
           Seq(JsonValidationError("error.path.missing"))),
          (__ \ "error" \ "message",
           Seq(JsonValidationError("error.path.missing")))
        ))
      s"fails to decode with error $jsError" in assert(
        Json.fromJson[JsonRpcResponseMessage](json) === jsError
      )
    }
    "without an error or a result" - {
      val json = Json.parse(
        """
            |{
            |  "jsonrpc" : "2.0",
            |  "id" : 0
            |}""".stripMargin
      )
      val jsError = JsError(__ \ "result", "error.path.missing")
      s"fails to decode with error $jsError" in assert(
        Json.fromJson[JsonRpcResponseMessage](json) === jsError
      )
    }
    "without an id" - {
      val json = Json.parse(
        """
            |{
            |  "jsonrpc" : "2.0",
            |  "result" : { "param1" : "param1", "param2" : "param2" }
            |}""".stripMargin
      )
      val jsError = JsError(__ \ "id", "error.path.missing")
      s"fails to decode with error $jsError" in assert(
        Json.fromJson[JsonRpcResponseMessage](json) === jsError
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
          |  "jsonrpc" : "2.0",
          |  "error" : {
          |    "code" : -32700,
          |    "message" : "Parse error",
          |    "data" : {
          |      "meaning" : "Invalid JSON was received by the server.\nAn error occurred on the server while parsing the JSON text.",
          |      "error" : "Boom"
          |    }
          |  },
          |  "id" : 1
          |}""".stripMargin
      )
      s"decodes to $jsonRpcResponseMessage" in assert(
        Json.fromJson(jsonRpcResponseMessageJson) === JsSuccess(
          jsonRpcResponseMessage)
      )
      s"encodes to $jsonRpcResponseMessageJson" in assert(
        Json.toJson(jsonRpcResponseMessage) === jsonRpcResponseMessageJson
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
          |  "jsonrpc" : "2.0",
          |  "error" : {
          |    "code" : -32600,
          |    "message" : "Invalid Request",
          |    "data" : {
          |      "meaning" : "The JSON sent is not a valid Request object.",
          |      "error" : {
          |        "obj.method" : [ {
          |          "msg" : [ "error.path.missing" ],
          |          "args" : []
          |        } ]
          |      }
          |    }
          |  },
          |  "id" : 1
          |}""".stripMargin
      )
      s"decodes to $jsonRpcResponseMessage" in assert(
        Json.fromJson(jsonRpcResponseMessageJson) === JsSuccess(
          jsonRpcResponseMessage)
      )
      s"encodes to $jsonRpcResponseMessageJson" in assert(
        Json.toJson(jsonRpcResponseMessage) === jsonRpcResponseMessageJson
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
          |  "jsonrpc" : "2.0",
          |  "error" : {
          |    "code" : -32601,
          |    "message" : "Method not found",
          |    "data" : {
          |      "meaning" : "The method does not exist / is not available.",
          |      "error" : "The method \"foo\" is not implemented."
          |    }
          |  },
          |  "id" : 1
          |}""".stripMargin
      )
      s"decodes to $jsonRpcResponseMessage" in assert(
        Json.fromJson(jsonRpcResponseMessageJson) === JsSuccess(
          jsonRpcResponseMessage)
      )
      s"encodes to $jsonRpcResponseMessageJson" in assert(
        Json.toJson(jsonRpcResponseMessage) === jsonRpcResponseMessageJson
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
          |  "jsonrpc" : "2.0",
          |  "error" : {
          |    "code" : -32602,
          |    "message" : "Invalid params",
          |    "data" : {
          |      "meaning" : "Invalid method parameter(s).",
          |      "error" : {
          |        "obj.arg1" : [ {
          |          "msg" : [ "error.path.missing" ],
          |          "args" : []
          |        } ]
          |      }
          |    }
          |  },
          |  "id" : 1
          |}""".stripMargin
      )
      s"decodes to $jsonRpcResponseMessage" in assert(
        Json.fromJson(jsonRpcResponseMessageJson) === JsSuccess(
          jsonRpcResponseMessage)
      )
      s"encodes to $jsonRpcResponseMessageJson" in assert(
        Json.toJson(jsonRpcResponseMessage) === jsonRpcResponseMessageJson
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
          |  "jsonrpc" : "2.0",
          |  "error" : { "code" : -32603,
          |    "message" : "Internal error",
          |    "data" : {
          |      "meaning" : "Internal JSON-RPC error."
          |    }
          |  },
          |  "id" : 1
          |}""".stripMargin
      )
      s"decodes to $jsonRpcResponseMessage" in assert(
        Json.fromJson(jsonRpcResponseMessageJson) === JsSuccess(
          jsonRpcResponseMessage)
      )
      s"encodes to $jsonRpcResponseMessageJson" in assert(
        Json.toJson(jsonRpcResponseMessage) === jsonRpcResponseMessageJson
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
          |  "jsonrpc" : "2.0",
          |  "error" : {
          |    "code" : -32099,
          |    "message" : "Server error",
          |    "data" : {
          |      "meaning" : "Something went wrong in the receiving application."
          |    }
          |  },
          |  "id" : 1
          |}""".stripMargin
      )
      s"decodes to $jsonRpcResponseMessage" in assert(
        Json.fromJson(jsonRpcResponseMessageJson) === JsSuccess(
          jsonRpcResponseMessage)
      )
      s"encodes to $jsonRpcResponseMessageJson" in assert(
        Json.toJson(jsonRpcResponseMessage) === jsonRpcResponseMessageJson
      )
    }
    "with an application error" - {
      val jsonRpcResponseMessage =
        JsonRpcResponseErrorMessage.applicationError(
          code = -31999,
          message = "Boom",
          data = None,
          NumericCorrelationId(1)
        )
      val jsonRpcResponseMessageJson = Json.parse(
        """
          |{
          |  "jsonrpc" : "2.0",
          |  "error" : { "code" : -31999, "message" : "Boom" },
          |  "id" : 1
          |}""".stripMargin
      )
      s"decodes to $jsonRpcResponseMessage" in assert(
        Json.fromJson(jsonRpcResponseMessageJson) === JsSuccess(
          jsonRpcResponseMessage)
      )
      s"encodes to $jsonRpcResponseMessageJson" in assert(
        Json.toJson(jsonRpcResponseMessage) === jsonRpcResponseMessageJson
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
            |  "jsonrpc" : "2.0",
            |  "result" : { "param1" : "param1", "param2" : "param2" },
            |  "id" : null
            |}""".stripMargin
        )
        s"decodes to $jsonRpcResponseMessage" in assert(
          Json.fromJson(jsonRpcResponseMessageJson) === JsSuccess(
            jsonRpcResponseMessage)
        )
        s"encodes to $jsonRpcResponseMessageJson" in assert(
          Json.toJson(jsonRpcResponseMessage) === jsonRpcResponseMessageJson
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
            |  "jsonrpc" : "2.0",
            |  "result" : { "param1" : "param1", "param2" : "param2" },
            |  "id" : "one"
            |}""".stripMargin
        )
        s"decodes to $jsonRpcResponseMessage" in assert(
          Json.fromJson(jsonRpcResponseMessageJson) === JsSuccess(
            jsonRpcResponseMessage)
        )
        s"encodes to $jsonRpcResponseMessageJson" in assert(
          Json.toJson(jsonRpcResponseMessage) === jsonRpcResponseMessageJson
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
            |  "jsonrpc" : "2.0",
            |  "result" : { "param1" : "param1", "param2" : "param2" },
            |  "id" : 1
            |}""".stripMargin
        )
        s"decodes to $jsonRpcResponseMessage" in assert(
          Json.fromJson(jsonRpcResponseMessageJson) === JsSuccess(
            jsonRpcResponseMessage)
        )
        s"encodes to $jsonRpcResponseMessageJson" in assert(
          Json.toJson(jsonRpcResponseMessage) === jsonRpcResponseMessageJson
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
              |  "jsonrpc" : "2.0",
              |  "result" : { "param1" : "param1", "param2" : "param2" },
              |  "id" : 1.1
              |}""".stripMargin
          )
          s"decodes to $jsonRpcResponseMessage" in assert(
            Json.fromJson(jsonRpcResponseMessageJson) === JsSuccess(
              jsonRpcResponseMessage)
          )
          s"encodes to $jsonRpcResponseMessageJson" in assert(
            Json.toJson(jsonRpcResponseMessage) === jsonRpcResponseMessageJson
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
      s"fails to decode with error $jsError" in assert(
        Json.fromJson[JsonRpcResponseMessageBatch](json) === jsError
      )
    }
    "with an invalid response" - {
      val json = Json.parse(
        """
            |[
            |  {
            |    "jsonrpc" : "2.0",
            |    "id" : 1
            |  }
            |]""".stripMargin
      )
      val jsError = JsError(__(0) \ "result", "error.path.missing")
      s"fails to decode with error $jsError" in assert(
        Json.fromJson[JsonRpcResponseMessageBatch](json) === jsError
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
          |    "jsonrpc" : "2.0",
          |    "result" : { "param1" : "param1", "param2" : "param2" },
          |    "id" : 1
          |  }
          |]""".stripMargin
      )
      s"decodes to $jsonRpcResponseMessageBatch" in assert(
        Json.fromJson(jsonRpcResponseMessageBatchJson) === JsSuccess(
          jsonRpcResponseMessageBatch)
      )
      s"encodes to $jsonRpcResponseMessageBatchJson" in assert(
        Json.toJson(jsonRpcResponseMessageBatch) ===
          jsonRpcResponseMessageBatchJson
      )
    }
  }

  "A JsonRpcNotificationMessage" - {
    "with an incorrect version" - {
      val json = Json.parse(
        """
            |{
            |  "jsonrpc" : "3.0",
            |  "method" : "testMethod",
            |  "params" : { "param1" : "param1", "param2" : "param2" }
            |}""".stripMargin
      )
      val jsError = JsError(__ \ "jsonrpc", "error.invalid")
      s"fails to decode with error $jsError" in assert(
        Json.fromJson[JsonRpcNotificationMessage](json) === jsError
      )
    }
    "with version of the wrong type" - {
      val json = Json.parse(
        """
            |{
            |  "jsonrpc" :2.0,
            |  "method" : "testMethod",
            |  "params" : { "param1" : "param1", "param2" : "param2" }
            |}""".stripMargin
      )
      val jsError = JsError(__ \ "jsonrpc", "error.expected.jsstring")
      s"fails to decode with error $jsError" in assert(
        Json.fromJson[JsonRpcNotificationMessage](json) === jsError
      )
    }
    "without a version" - {
      val json = Json.parse(
        """
            |{
            |  "method" : "testMethod",
            |  "params" : { "param1" : "param1", "param2" : "param2" }
            |}""".stripMargin
      )
      val jsError = JsError(__ \ "jsonrpc", "error.path.missing")
      s"fails to decode with error $jsError" in assert(
        Json.fromJson[JsonRpcNotificationMessage](json) === jsError
      )
    }
    "with method of the wrong type" - {
      val json = Json.parse(
        """
            |{
            |  "jsonrpc" : "2.0",
            |  "method" : 3.0,
            |  "params" : { "param1" : "param1", "param2" : "param2" }
            |}""".stripMargin
      )
      val jsError = JsError(__ \ "method", "error.expected.jsstring")
      s"fails to decode with error $jsError" in assert(
        Json.fromJson[JsonRpcNotificationMessage](json) === jsError
      )
    }
    "without a method" - {
      val json = Json.parse(
        """
            |{
            |  "jsonrpc" : "2.0",
            |  "params" : { "param1" : "param1", "param2" : "param2" }
            |}""".stripMargin
      )
      val jsError = JsError(__ \ "method", "error.path.missing")
      s"fails to decode with error $jsError" in assert(
        Json.fromJson[JsonRpcNotificationMessage](json) === jsError
      )
    }
    "with params of the wrong type" - {
      val json = Json.parse(
        """
            |{
            |  "jsonrpc" : "2.0",
            |  "method" : "testMethod",
            |  "params" : "params"
            |}""".stripMargin
      )
      val jsError = JsError(__ \ "params", "error.expected.jsobjectorjsarray")
      s"fails to decode with error $jsError" in assert(
        Json.fromJson[JsonRpcNotificationMessage](json) === jsError
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
          |  "jsonrpc" : "2.0",
          |  "method" : "testMethod"
          |}""".stripMargin
      )
      s"decodes to $jsonRpcNotificationMessage" in assert(
        Json.fromJson(jsonRpcNotificationMessageJson) === JsSuccess(
          jsonRpcNotificationMessage)
      )
      s"encodes to $jsonRpcNotificationMessageJson" in assert(
        Json.toJson(jsonRpcNotificationMessage) ===
          jsonRpcNotificationMessageJson
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
          |  "jsonrpc" : "2.0",
          |  "method" : "testMethod",
          |  "params" : [ "param1", "param2" ]
          |}""".stripMargin
      )
      s"decodes to $jsonRpcNotificationMessage" in assert(
        Json.fromJson(jsonRpcNotificationMessageJson) === JsSuccess(
          jsonRpcNotificationMessage)
      )
      s"encodes to $jsonRpcNotificationMessageJson" in assert(
        Json.toJson(jsonRpcNotificationMessage) ===
          jsonRpcNotificationMessageJson
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
          |  "jsonrpc" : "2.0",
          |  "method" : "testMethod",
          |  "params" : { "param1" : "param1", "param2" : "param2" }
          |}""".stripMargin
      )
      s"decodes to $jsonRpcNotificationMessage" in assert(
        Json.fromJson(jsonRpcNotificationMessageJson) === JsSuccess(
          jsonRpcNotificationMessage)
      )
      s"encodes to $jsonRpcNotificationMessageJson" in assert(
        Json.toJson(jsonRpcNotificationMessage) ===
          jsonRpcNotificationMessageJson
      )
    }
  }
}
