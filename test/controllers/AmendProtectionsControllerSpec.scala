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

import java.util.Random

import connectors.NpsConnector
import model.{ProtectionAmendment, ProtectionApplication}
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.{ActionBuilder, Request, Result}
import util.NinoHelper
import play.api.libs.json._
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class AmendProtectionsControllerSpec  extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfter {

  val rand = new Random()
  val ninoGenerator = new Generator(rand)

  def randomNino: String = ninoGenerator.nextNino.nino.replaceFirst("MA", "AA")

  val testNino = randomNino
  val (testNinoWithoutSuffix, _) = NinoHelper.dropNinoSuffix(testNino)
  val testProtectionId = 1
  val testProtectionVersion = 1
  implicit val hc = HeaderCarrier()
  val mockNpsConnector = mock[NpsConnector]

  val validAmendBody = Json.toJson(ProtectionAmendment(
    protectionType = "IP2016",
    status = "Open",
    version = testProtectionVersion,
    relevantAmount = 1250000.00,
    postADayBenefitCrystallisationEvents = 250000.00,
    preADayPensionInPayment = 250000.00,
    nonUKRights = 250000.00,
    uncrystallisedRights = 500000.00
  ))

  val invalidAmendBody = Json.parse(
    """
      |{
      |  "type" : 100000
      |}
    """.stripMargin)

  val successfulAmendIP2016NPSResponseBody = Json.parse(
    s"""
       |  {
       |      "nino": "${testNinoWithoutSuffix}",
       |      "pensionSchemeAdministratorCheckReference" : "PSA123456789",
       |      "protection": {
       |        "id": ${testProtectionId},
       |        "version": ${testProtectionVersion},
       |        "type": 1,
       |        "certificateDate": "2015-05-22",
       |        "certificateTime": "12:22:59",
       |        "status": 1,
       |        "protectionReference": "IP161234567890C",
       |        "relevantAmount": 1250000.00,
       |        "notificationID": 12
       |      }
       |    }
       |
    """.stripMargin).as[JsObject]

  val unsuccessfulAmendIP2016NPSResponseBody = Json.parse(
    s"""
       |  {
       |      "nino": "${testNinoWithoutSuffix}",
       |      "protection": {
       |        "id": ${testProtectionId},
       |        "version": ${testProtectionVersion + 1},
       |        "type": 2,
       |        "status": 5,
       |        "notificationID": 10
       |      }
       |  }
    """.stripMargin).as[JsObject]

  val standardHeaders = Seq("Content-type" -> Seq("application/json"))
  val validExtraHOutboundHeaders = Seq("Environment" -> Seq("local"), "Authorisation" -> Seq("Bearer abcdef12345678901234567890"))

  object testProtectionService extends services.ProtectionService {
    override val nps = mockNpsConnector
  }

  case class AlwaysExecuteAction(nino: String) extends ActionBuilder[Request] {

    def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = {
      block(request)
    }
  }

  object testAmendController extends AmendProtectionsController {
    override val protectionService = testProtectionService
    override def WithCitizenRecordCheck(nino:String) = AlwaysExecuteAction(nino)
  }

  "AmendProtectionController" should {
    "respond to a valid Amend Protection request with OK" in {
      when(mockNpsConnector.amendProtection(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(model.HttpResponseDetails(200, JsSuccess(successfulAmendIP2016NPSResponseBody))))

      val fakeRequest: FakeRequest[JsValue] = FakeRequest(
        method = "PUT",
        uri = "",
        headers = FakeHeaders(Seq("Content-type" -> Seq("application/json"))),
        body = validAmendBody)

      val result: Future[Result] = testAmendController.amendProtection(testNino, testProtectionId.toString).apply(fakeRequest)
      status(result) must be(OK)
    }
  }

  "AmendProtectionController" should {
    "respond to an invalid Amend Protection request with BAD_REQUEST" in {
      when(mockNpsConnector.amendProtection(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(model.HttpResponseDetails(200, JsSuccess(successfulAmendIP2016NPSResponseBody))))

      val fakeRequest = FakeRequest(
        method = "PUT",
        uri = "",
        headers = FakeHeaders(Seq("Content-type" -> Seq("application/json"))),
        body = invalidAmendBody)

      val result = testAmendController.amendProtection(testNino, testProtectionId.toString).apply(fakeRequest)
      status(result) must be(BAD_REQUEST)
    }
  }

  "AmendProtectionController" should {
    "handle a 500 (INTERNAL_SERVER_ERROR) response from NPS service by passing it back to the caller" in {
      when(mockNpsConnector.amendProtection(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(model.HttpResponseDetails(500, JsSuccess(successfulAmendIP2016NPSResponseBody))))

      val fakeRequest = FakeRequest(
        method = "PUT",
        uri = "",
        headers = FakeHeaders(Seq("Content-type" -> Seq("application/json"))),
        body = validAmendBody)

      val result = testAmendController.amendProtection(testNino, testProtectionId.toString).apply(fakeRequest)
      status(result) must be(INTERNAL_SERVER_ERROR)
    }
  }

  "AmendProtectionController" should {
    "handle a 499 response from NPS service by passing 500 (INTERNAL_SERVER_ERROR) back to the caller" in {
      when(mockNpsConnector.amendProtection(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(model.HttpResponseDetails(499, JsSuccess(successfulAmendIP2016NPSResponseBody))))

      val fakeRequest = FakeRequest(
        method = "PUT",
        uri = "",
        headers = FakeHeaders(Seq("Content-type" -> Seq("application/json"))),
        body = validAmendBody)

      val result = testAmendController.amendProtection(testNino, testProtectionId.toString).apply(fakeRequest)
      status(result) must be(INTERNAL_SERVER_ERROR)
    }
  }


  "AmendProtectionController" should {
    "handle a 503 response from NPS service by passing same code back to the caller" in {
      when(mockNpsConnector.amendProtection(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(model.HttpResponseDetails(503, JsSuccess(successfulAmendIP2016NPSResponseBody))))

      val fakeRequest = FakeRequest(
        method = "PUT",
        uri = "",
        headers = FakeHeaders(Seq("Content-type" -> Seq("application/json"))),
        body = validAmendBody)

      val result = testAmendController.amendProtection(testNino, testProtectionId.toString).apply(fakeRequest)
      status(result) must be(SERVICE_UNAVAILABLE)
    }
  }

  "AmendProtectionController" should {
    "handle a 401 (UNAUTHORIZED) response from NPS service by passing it back to the caller" in {
      when(mockNpsConnector.amendProtection(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(model.HttpResponseDetails(401, JsSuccess(successfulAmendIP2016NPSResponseBody))))

      val fakeRequest = FakeRequest(
        method = "PUT",
        uri = "",
        headers = FakeHeaders(Seq("Content-type" -> Seq("application/json"))),
        body = validAmendBody)

      val result = testAmendController.amendProtection(testNino, testProtectionId.toString).apply(fakeRequest)
      status(result) must be(UNAUTHORIZED)
    }

    "AmendProtectionController" should {
      "handle a 400 (BAD_REQUEST) response from NPS service by passing it back to the caller" in {
        when(mockNpsConnector.amendProtection(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(model.HttpResponseDetails(400, JsSuccess(successfulAmendIP2016NPSResponseBody))))

        val fakeRequest = FakeRequest(
          method = "PUT",
          uri = "",
          headers = FakeHeaders(Seq("Content-type" -> Seq("application/json"))),
          body = validAmendBody)

        val result = testAmendController.amendProtection(testNino, testProtectionId.toString).apply(fakeRequest)
        status(result) must be(BAD_REQUEST)
      }

    "AmendProtectionController" should {
        "handle an OK status but non-parseable response body from NPS service by passing an INTERNAL_SERVER_ERROR back to the caller" in {
          when(mockNpsConnector.amendProtection(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
            .thenReturn(Future.successful(model.HttpResponseDetails(200, JsError("Unparseable response"))))

          val fakeRequest = FakeRequest(
            method = "PUT",
            uri = "",
            headers = FakeHeaders(Seq("Content-type" -> Seq("application/json"))),
            body = validAmendBody)

          val result = testAmendController.amendProtection(testNino,testProtectionId.toString).apply(fakeRequest)
          status(result) must be(INTERNAL_SERVER_ERROR)
        }
      }
    }
  }
}
