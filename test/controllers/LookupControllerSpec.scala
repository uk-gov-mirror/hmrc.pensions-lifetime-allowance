/*
 * Copyright 2017 HM Revenue & Customs
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

import connectors.NpsConnector
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse }

class LookupControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  implicit val hc = HeaderCarrier()
  val mockNpsConnector: NpsConnector = mock[NpsConnector]

  val validExtraHOutboundHeaders = Seq("Environment" -> "local", "Authorisation" -> "Bearer abcdef12345678901234567890")

  val validResponse: JsValue = Json.parse(
    s"""{"pensionSchemeAdministratorCheckReference": "PSA12345678A","ltaType": 7,"psaCheckResult": 1,"relevantAmount": 25000}"""
  )

  val notFoundResponse: JsValue = Json.parse(
    s"""{"Reason":"Resource not found"}"""
  )

  val badRequestResponse: JsValue = Json.parse(
    s"""{"Reason":"Your submission contains one or more errors. Failed Parameter(s) - [lifetimeAllowanceReference]"}"""
  )

  object testController extends LookupController {
    override val npsConnector: NpsConnector = mockNpsConnector
  }

  override protected def beforeEach(): Unit = reset(mockNpsConnector)

  "Lookup Controller" should {
    "return 403 when no environment header present and Forbidden is returned from nps" in {
      when(mockNpsConnector.getPSALookup(any(), any())(any(), any()))
        .thenReturn(Future.successful(HttpResponse(FORBIDDEN, None)))

      val result = testController.psaLookup("PSA12345678A", "IP141000000000A").apply(FakeRequest())
      status(result) mustBe FORBIDDEN
      contentAsString(result) mustBe "{\"message\":\"NPS request resulted in a response with: HTTP status = 403 body = null\"}"
    }

    "return 401 when no auth header present with body and Unauthorised is returned from nps" in {
      when(mockNpsConnector.getPSALookup(any(), any())(any(), any()))
        .thenReturn(Future.successful(HttpResponse(UNAUTHORIZED, Some(Json.toJson("Required OAuth credentials not provided")))))

      val result = testController.psaLookup("PSA12345678A", "IP141000000000A").apply(FakeRequest())
      status(result) mustBe UNAUTHORIZED
      contentAsString(result) mustBe "{\"message\":\"NPS request resulted in a response with: HTTP status = 401 body = \\\"Required OAuth credentials not provided\\\"\"}"
    }

    "return 500 when Internal Server Error is returned from nps" in {
      when(mockNpsConnector.getPSALookup(any(), any())(any(), any()))
        .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, None)))

      val result = testController.psaLookup("PSA12345678A", "IP141000000000A").apply(FakeRequest().withHeaders(validExtraHOutboundHeaders: _*))
      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsString(result) mustBe "{\"message\":\"NPS request resulted in a response with: HTTP status = 500 body = null\"}"
    }

    "return 503 when Service Unavailable is returned from nps" in {
      when(mockNpsConnector.getPSALookup(any(), any())(any(), any()))
        .thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, None)))

      val result = testController.psaLookup("PSA12345678A", "IP141000000000A").apply(FakeRequest())
      status(result) mustBe SERVICE_UNAVAILABLE
      contentAsString(result) mustBe "{\"message\":\"NPS request resulted in a response with: HTTP status = 503 body = null\"}"
    }

    "return 400 when Bad Request is returned from nps" in {
      when(mockNpsConnector.getPSALookup(any(), any())(any(), any()))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(badRequestResponse))))

      val result = testController.psaLookup("PSA12345678A", "IP14100").apply(FakeRequest())
      status(result) mustBe BAD_REQUEST
    }

    "return 404 when Not Found is returned from nps" in {
      when(mockNpsConnector.getPSALookup(any(), any())(any(), any()))
        .thenReturn(Future.successful(HttpResponse(NOT_FOUND, Some(notFoundResponse))))

      val result = testController.psaLookup("PSA12345678Z", "IP141000000000Z").apply(FakeRequest())
      status(result) mustBe NOT_FOUND
    }

    "return 200 when OK is returned from nps" in {
      when(mockNpsConnector.getPSALookup(any(), any())(any(), any()))
        .thenReturn(Future.successful(HttpResponse(OK, Some(validResponse))))

      val result = testController.psaLookup("PSA12345678A", "IP141000000000A").apply(FakeRequest())
      status(result) mustBe OK
      contentAsString(result) mustBe validResponse.toString
    }
  }


}
