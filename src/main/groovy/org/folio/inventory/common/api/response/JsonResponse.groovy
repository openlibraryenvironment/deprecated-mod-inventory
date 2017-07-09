package org.folio.inventory.common.api.response

import io.vertx.core.json.Json
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

class JsonResponse {
  static success(HttpServerResponse response, body) {
    jsonResponse(response, body, 200)
  }

  static created(HttpServerResponse response, body) {
    jsonResponse(response, body, 201)
  }

  static unprocessable(HttpServerResponse response, String property, String value, String message) {
    JsonObject body = new JsonObject()

    JsonArray errors = new JsonArray()

    def parameters = new JsonArray().add(
      new JsonObject()
        .put("key", property)
        .put("value", value))

    errors.add(new JsonObject()
      .put("message", message)
      .put("parameters", parameters))

    body.put("errors", errors)

    jsonResponse(response, body, 422)
  }

  private static void jsonResponse(
    HttpServerResponse response, body, Integer status) {

    def json = Json.encodePrettily(body)
    def buffer = Buffer.buffer(json, "UTF-8")

    response.statusCode = status
    response.putHeader "content-type", "application/json; charset=utf-8"
    response.putHeader "content-length", Integer.toString(buffer.length())

    println("JSON Success: ${json}")

    response.write(buffer)
    response.end()
  }
}
