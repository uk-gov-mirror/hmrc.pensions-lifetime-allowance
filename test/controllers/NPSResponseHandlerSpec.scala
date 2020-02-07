/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import model.HttpResponseDetails
import play.api.libs.json._
import uk.gov.hmrc.play.test.UnitSpec
import play.api.http.Status._
import uk.gov.hmrc.http.{ BadRequestException, Upstream4xxResponse, Upstream5xxResponse }

class NPSResponseHandlerSpec extends UnitSpec {
  val testResponseHandler: NPSResponseHandler = new NPSResponseHandler {}

  private implicit val system: ActorSystem = ActorSystem("test-sys")
  private implicit val mat: ActorMaterializer = ActorMaterializer()

  "NPSResponseHandler" should {
    "process a NPS response" when {
      "NPS returns an OK" in {
        val fakeResponse = HttpResponseDetails(OK, JsSuccess(Json.obj("result" -> JsString("success"))))
        status(testResponseHandler.handleNPSSuccess(fakeResponse)) shouldBe OK
      }
      "NPS returns a CONFLICT" in {
        val fakeResponse = HttpResponseDetails(CONFLICT, JsSuccess(Json.obj("result" -> JsString("conflict"))))
        status(testResponseHandler.handleNPSSuccess(fakeResponse)) shouldBe CONFLICT
      }
      "NPS returns an OK with an invalid body" in {
        val fakeResponse = HttpResponseDetails(OK, JsError("error message"))
        val result = testResponseHandler.handleNPSSuccess(fakeResponse)
        status(result) shouldBe INTERNAL_SERVER_ERROR
        jsonBodyOf(result) shouldBe Json.obj("message" -> JsString(
          "NPS request resulted in a response with: HTTP status=" + fakeResponse.status + ", but unable to parse the NPS response body"
        ))
      }
      "NPS returns a CONFLICT with an invalid body" in {
        val fakeResponse = HttpResponseDetails(CONFLICT, JsError("error message"))
        val result = testResponseHandler.handleNPSSuccess(fakeResponse)
        status(result) shouldBe INTERNAL_SERVER_ERROR
        jsonBodyOf(result) shouldBe Json.obj("message" -> JsString(
          "NPS request resulted in a response with: HTTP status=" + fakeResponse.status + ", but unable to parse the NPS response body"
        ))
      }
      "NPS returns a NOT_ACCEPTABLE with a valid body" in {
        val fakeResponse = HttpResponseDetails(NOT_ACCEPTABLE, JsSuccess(Json.obj("result" -> JsString("success"))))
        val result = testResponseHandler.handleNPSSuccess(fakeResponse)
        status(result) shouldBe INTERNAL_SERVER_ERROR
        jsonBodyOf(result) shouldBe Json.obj("message" -> JsString(
          "NPS request resulted in a response with: HTTP status=" + fakeResponse.status +  ", body=" + Json.asciiStringify(fakeResponse.body.get)
        ))
      }
      "NPS returns a NOT_ACCEPTABLE with an invalid body" in {
        val fakeResponse = HttpResponseDetails(NOT_ACCEPTABLE, JsError("error message"))
        val result = testResponseHandler.handleNPSSuccess(fakeResponse)
        status(result) shouldBe INTERNAL_SERVER_ERROR
        jsonBodyOf(result) shouldBe Json.obj("message" -> JsString(
          "NPS request resulted in a response with: HTTP status=" + fakeResponse.status + ", but unable to parse the NPS response body"
        ))
      }
    }

    "handle a NPS error" when {
      "a Service unavailable response is received" in {
        val npsError = Upstream5xxResponse("service unavailable", SERVICE_UNAVAILABLE, 1)
        val result = testResponseHandler.handleNPSError(npsError, "[TestController] [callNps]")
        status(result) shouldBe SERVICE_UNAVAILABLE
      }
      "a Bad gateway response is received" in {
        val npsError = Upstream5xxResponse("bad gateway", BAD_GATEWAY, 1)
        val result = testResponseHandler.handleNPSError(npsError, "[TestController] [callNps]")
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
      "an Unauthorized response is received" in {
        val npsError = Upstream4xxResponse("unauthorized", UNAUTHORIZED, 1)
        val result = testResponseHandler.handleNPSError(npsError, "[TestController] [callNps]")
        status(result) shouldBe UNAUTHORIZED
      }
      "a Forbidden response is received" in {
        val npsError = Upstream4xxResponse("forbidden", FORBIDDEN, 1)
        val result = testResponseHandler.handleNPSError(npsError, "[TestController] [callNps]")
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
      "a Bad request response is received" in {
        val npsError = new BadRequestException("bad request")
        val result = testResponseHandler.handleNPSError(npsError, "[TestController] [callNps]")
        status(result) shouldBe BAD_REQUEST
      }
      "a different error is thrown" in {
        val npsError = new RuntimeException("different error")
        a[RuntimeException] shouldBe thrownBy(testResponseHandler.handleNPSError(npsError, "[TestController] [callNps]"))
      }
    }
  }
}
