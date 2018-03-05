/*
 * Copyright 2018 HM Revenue & Customs
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

import akka.stream.Materializer
import connectors.{CitizenDetailsConnector, CitizenRecordOK, NpsConnector}
import org.mockito.Matchers
import org.mockito.Mockito._
import _root_.mock.AuthMock
import org.scalatest.BeforeAndAfter
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.{JsError, JsObject, JsSuccess, Json}
import play.api.mvc.{ActionBuilder, Request, Result}
import play.api.test.Helpers._
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.Generator
import util.NinoHelper
import play.api.libs.json.JsNumber

import scala.concurrent.Future
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}

class ReadProtectionsControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfter with AuthMock {

  val rand = new Random()
  val ninoGenerator = new Generator(rand)
  implicit lazy val materializer: Materializer = app.materializer
  def randomNino: String = ninoGenerator.nextNino.nino.replaceFirst("MA", "AA")
  val mockCitizenDetailsConnector = mock[CitizenDetailsConnector]
  val testNino = randomNino
  val (testNinoWithoutSuffix, _) = NinoHelper.dropNinoSuffix(testNino)

  when(mockCitizenDetailsConnector.checkCitizenRecord(Matchers.any[String])(Matchers.any(), Matchers.any()))
    .thenReturn(Future.successful(CitizenRecordOK))

  mockAuthConnector(Future.successful({}))

  implicit val hc = HeaderCarrier()
  val mockNpsConnector = mock[NpsConnector]

  val successfulReadResponseBody = Json.parse(
    s"""
       |  {
       |      "pensionSchemeAdministratorCheckReference" : "PSA123456789",
       |      "nino": "${testNinoWithoutSuffix}",
       |      "protections": [
       |      {
       |        "id": 1,
       |        "version": 1,
       |        "type": 1,
       |        "certificateDate": "2015-05-22",
       |        "certificateTime": "12:22:59",
       |        "status": 1,
       |        "protectionReference": "FP161234567890C",
       |        "relevantAmount": 1250000.00,
       |        "preADayPensionInPayment": 250000,
       |        "postADayBCE": 250000,
       |        "uncrystallisedRights": 500000,
       |        "nonUKRights": 250000,
       |        "pensionDebitAmount": 0,
       |        "notificationID": 5,
       |        "protectedAmount": 600000,
       |        "pensionDebitEnteredAmount": 300,
       |        "pensionDebitStartDate": "2015-01-29",
       |        "pensionDebitTotalAmount": 800,
       |        "previousVersions": []
       |      }
       |      ]
       |    }
       |
    """.stripMargin).as[JsObject]

  val successfulReadResponseBodyEmptyProtections = Json.parse(
    s"""
       |  {
       |      "pensionSchemeAdministratorCheckReference" : "PSA123456789",
       |      "nino": "${testNinoWithoutSuffix}",
       |      "protections": [
       |      ]
       |    }
       |
    """.stripMargin).as[JsObject]

  val successfulReadResponseBodyNoProtections = Json.parse(
    s"""
       |  {
       |      "pensionSchemeAdministratorCheckReference" : "PSA123456789",
       |      "nino": "${testNinoWithoutSuffix}"
       |  }
       |
    """.stripMargin).as[JsObject]

  val unsuccessfulReadResponseBody = Json.parse(
    s"""
       |  {
       |      "nino": "${testNinoWithoutSuffix}",
       |      "protection": {
       |        "id": 1,
       |        "version": 1,
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

  object testCreateController extends ReadProtectionsController {
    override val protectionService = testProtectionService
    override lazy val authConnector = mockAuthConnector
    override lazy val citizenDetailsConnector = mockCitizenDetailsConnector
  }

  "ReadProtectionsController" should {
    "respond to a valid Read Protections request with OK" in {
      when(mockNpsConnector.readExistingProtections(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(model.HttpResponseDetails(200, JsSuccess(successfulReadResponseBody))))

      val result = testCreateController.readExistingProtections(testNino)(FakeRequest())
      status(result) must be(OK)
    }
  }

  "ReadProtectionsController" should {
    "respond to an invalid Read Protections request with BAD_REQUEST" in {
      when(mockNpsConnector.readExistingProtections(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.failed(new BadRequestException("bad request")))

      val result = testCreateController.readExistingProtections(testNino)(FakeRequest())
      status(result) must be(BAD_REQUEST)
    }
  }

  "ReadProtectionsController" should {
    "handle a 202 (INTERNAL_SERVER_ERROR) response from NPS service to a read protections request by passing it back to the caller" in {
      when(mockNpsConnector.readExistingProtections(Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(model.HttpResponseDetails(202, JsSuccess(successfulReadResponseBody))))

      val result = testCreateController.readExistingProtections(testNino).apply(FakeRequest())
      status(result) must be(INTERNAL_SERVER_ERROR)
    }
  }

      "ReadProtectionsController " should {
        "handle an OK status but non-parseable response body from NPS service to a read protections request by passing an INTERNAL_SERVER_ERROR back to the caller" in {
          when(mockNpsConnector.readExistingProtections(Matchers.any())(Matchers.any(), Matchers.any()))
            .thenReturn(Future.successful(model.HttpResponseDetails(200, JsError())))

          val result = testCreateController.readExistingProtections(testNino).apply(FakeRequest())
          info(status(result).toString)
          status(result) must be(INTERNAL_SERVER_ERROR)
        }
      }
      "ReadProtectionsController" should {
        "return a count of zero to a read protections count request when an empty protections array is returned from NPS" in {
          when(mockNpsConnector.readExistingProtections(Matchers.any())(Matchers.any(), Matchers.any()))
            .thenReturn(Future.successful(model.HttpResponseDetails(200,JsSuccess(successfulReadResponseBodyEmptyProtections))))

          val result = testCreateController.readExistingProtectionsCount(testNino).apply(FakeRequest())

          status(result) must be(OK)
          (contentAsJson(result) \ "count").get must be(JsNumber(0))
        }
      }
      "ReadProtectionsController" should {
        "return a count of zero to a read protections count request when no protections array is returned from NPS" in {
          when(mockNpsConnector.readExistingProtections(Matchers.any())(Matchers.any(), Matchers.any()))
            .thenReturn(Future.successful(model.HttpResponseDetails(200,JsSuccess(successfulReadResponseBodyNoProtections))))

          val result: Future[Result] = testCreateController.readExistingProtectionsCount(testNino).apply(FakeRequest())

          status(result) must be(OK)
          (contentAsJson(result) \ "count").get must be(JsNumber(0))
        }
      }
      "ReadProtectionsController" should {
        "return a count of one to a read protections count request when a protections array with a single protection is returned from NPS" in {
          when(mockNpsConnector.readExistingProtections(Matchers.any())(Matchers.any(), Matchers.any()))
            .thenReturn(Future.successful(model.HttpResponseDetails(200,JsSuccess(successfulReadResponseBody))))

          val result = testCreateController.readExistingProtectionsCount(testNino).apply(FakeRequest())
          status(result) must be(OK)
          (contentAsJson(result) \ "count").get must be(JsNumber(1))
        }
      }

      "ReadProtectionsController" should {
        "handle an OK status but non-parseable response body from NPS service to a read protections count request by passing an INTERNAL_SERVER_ERROR back to the caller" in {
          when(mockNpsConnector.readExistingProtections(Matchers.any())(Matchers.any(), Matchers.any()))
            .thenReturn(Future.successful(model.HttpResponseDetails(200, JsError())))

          val result = testCreateController.readExistingProtectionsCount(testNino).apply(FakeRequest())
          status(result) must be(INTERNAL_SERVER_ERROR)
        }
      }
    }

