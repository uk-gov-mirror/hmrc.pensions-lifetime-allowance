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
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.Future

class LookupControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  implicit val hc = HeaderCarrier()
  val mockNpsConnector = mock[NpsConnector]

  val standardHeaders = Seq("Content-type" -> Seq("application/json"))
  val validExtraHOutboundHeaders = Seq("Environment" -> Seq("local"), "Authorisation" -> Seq("Bearer abcdef12345678901234567890"))

  object testController extends LookupController {
    override val npsConnector: NpsConnector = mockNpsConnector
  }

  override protected def beforeEach(): Unit = reset(mockNpsConnector)

  "Lookup Controller" should {
    "return 403 when no environment header present" in {
      when(mockNpsConnector.psaLookup(any(), any())(any(), any()))
        .thenReturn(Future.successful(HttpResponse(FORBIDDEN, None)))

      val result = testController.lookup("", "").apply(FakeRequest())
      status(result) mustBe FORBIDDEN
      contentAsString(result) mustBe "{\"message\":\"NPS request resulted in a response with: HTTP status = 403 body = null\"}"
    }

    "return 401 when no auth header present with body" in {
      when(mockNpsConnector.psaLookup(any(), any())(any(), any()))
        .thenReturn(Future.successful(HttpResponse(UNAUTHORIZED, None)))

      val result = testController.lookup("", "").apply(FakeRequest())
      status(result) mustBe UNAUTHORIZED
    }
  }


}
