/*
 * Copyright 2021 HM Revenue & Customs
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
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.ControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.Future
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, HttpResponse}

class LookupControllerSpec extends PlaySpec with GuiceOneServerPerSuite with BeforeAndAfterEach {

  implicit lazy val hc = HeaderCarrier()
  val mockNpsConnector: NpsConnector = mock[NpsConnector]

  implicit lazy val cc = app.injector.instanceOf[ControllerComponents]


  lazy val validResponse: JsValue = Json.parse(
    s"""{"pensionSchemeAdministratorCheckReference": "PSA12345678A","ltaType": 7,"psaCheckResult": 1,"relevantAmount": 25000}"""
  )

  lazy val controller = new DefaultLookupController(mockNpsConnector, cc
  )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockNpsConnector)
  }

  "LookupController" when {
    "return 200 when OK is returned from nps" in {
      when(mockNpsConnector.getPSALookup(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, validResponse.toString())))

      val result = controller.psaLookup("PSA12345678A", "IP141000000000A").apply(FakeRequest())
      status(result) mustBe OK
      contentAsString(result) mustBe validResponse.toString
    }
    "return 400 when Bad Request is returned from nps" in {
      when(mockNpsConnector.getPSALookup(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new BadRequestException("bad request")))

      val result = controller.psaLookup("PSA12345678A", "IP14100000000A").apply(FakeRequest())
      status(result) mustBe BAD_REQUEST
    }
    "return 202 when Internal Server Error is returned from nps" in {
      when(mockNpsConnector.getPSALookup(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(202, validResponse.toString())))

      val result = controller.psaLookup("PSA12345678A", "IP14100000000A").apply(FakeRequest())
      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }
}
