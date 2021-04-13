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

import java.util.Random
import akka.stream.Materializer
import connectors.{CitizenDetailsConnector, CitizenRecordOK}
import org.mockito.ArgumentMatchers
import org.scalatestplus.mockito.MockitoSugar
import _root_.mock.AuthMock
import com.codahale.metrics.SharedMetricRegistries
import model.HttpResponseDetails
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfter
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{JsError, JsObject, JsSuccess, Json}
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.Generator
import util.NinoHelper
import play.api.libs.json.JsNumber
import services.ProtectionService

import scala.concurrent.Future
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}

class ReadProtectionsControllerSpec extends PlaySpec with GuiceOneServerPerSuite with MockitoSugar with BeforeAndAfter with AuthMock {

  SharedMetricRegistries.clear()

  val rand = new Random()
  val ninoGenerator = new Generator(rand)

  def randomNino: String = ninoGenerator.nextNino.nino.replaceFirst("MA", "AA")

  val mockCitizenDetailsConnector: CitizenDetailsConnector = mock[CitizenDetailsConnector]
  val mockProtectionService: ProtectionService = mock[ProtectionService]
  val testNino: String = randomNino
  val (testNinoWithoutSuffix, _) = NinoHelper.dropNinoSuffix(testNino)

  when(mockCitizenDetailsConnector.checkCitizenRecord(ArgumentMatchers.any[String])(ArgumentMatchers.any(), ArgumentMatchers.any()))
    .thenReturn(Future.successful(CitizenRecordOK))

  mockAuthConnector(Future.successful({}))

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val cc: ControllerComponents = app.injector.instanceOf[ControllerComponents]
  implicit val materializer: Materializer = app.materializer

  val controller = new ReadProtectionsController(mockAuthConnector, mockCitizenDetailsConnector, mockProtectionService, cc)

  val successfulReadResponseBody: JsObject = Json.parse(
    s"""
       |  {
       |      "pensionSchemeAdministratorCheckReference" : "PSA123456789",
       |      "nino": "$testNinoWithoutSuffix",
       |      "lifetimeAllowanceProtections": [
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

  val successfulReadResponseBodyEmptyProtections: JsObject = Json.parse(
    s"""
       |  {
       |      "pensionSchemeAdministratorCheckReference" : "PSA123456789",
       |      "nino": "$testNinoWithoutSuffix",
       |      "protections": [
       |      ]
       |    }
       |
    """.stripMargin).as[JsObject]


  val successfulReadResponseBodyNoProtections: JsObject = Json.parse(
    s"""
       |  {
       |      "pensionSchemeAdministratorCheckReference" : "PSA123456789",
       |      "nino": "$testNinoWithoutSuffix"
       |  }
       |
    """.stripMargin).as[JsObject]

  val unsuccessfulReadResponseBody: JsObject = Json.parse(
    s"""
       |  {
       |      "nino": "$testNinoWithoutSuffix",
       |      "protection": {
       |        "id": 1,
       |        "version": 1,
       |        "type": 2,
       |        "status": 5,
       |        "notificationID": 10
       |      }
       |  }
    """.stripMargin).as[JsObject]

  val standardHeaders: ((String, String), (String, String), (String, String)) = (
    "Content-type" -> "application/json",
    "Environment" -> "local",
    "Authorisation" -> "Bearer abcdef12345678901234567890")

  def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]): Future[Result] = {
    block(request)
  }

  "ReadProtectionsController" when {
    "respond to a valid Read Protections request with OK" in {

      when(mockProtectionService.readExistingProtections(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponseDetails(OK, JsSuccess(successfulReadResponseBody))))

      lazy val result = controller.readExistingProtections(testNino)(FakeRequest().withHeaders("Content-Type" -> "application/json"))
      status(result) must be(OK)
    }

    "respond to an invalid Read Protections request with BAD_REQUEST" in {
      when(mockProtectionService.readExistingProtections(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new BadRequestException("bad request")))

      lazy val result = controller.readExistingProtections(testNino)(FakeRequest())
      status(result) must be(BAD_REQUEST)
    }

    "handle a 202 (INTERNAL_SERVER_ERROR) response from NPS service to a read protections request by passing it back to the caller" in {
      when(mockProtectionService.readExistingProtections(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(model.HttpResponseDetails(ACCEPTED, JsSuccess(successfulReadResponseBody))))

      lazy val result = controller.readExistingProtections(testNino).apply(FakeRequest())
      status(result) must be(INTERNAL_SERVER_ERROR)
    }

    "handle an OK status but non-parseable response body from NPS service to a read protections request " +
      "by passing an INTERNAL_SERVER_ERROR back to the caller" in {
      when(mockProtectionService.readExistingProtections(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(model.HttpResponseDetails(OK, JsError())))

      lazy val result = controller.readExistingProtections(testNino).apply(FakeRequest())
      info(status(result).toString)
      status(result) must be(INTERNAL_SERVER_ERROR)
    }

    "return a count of zero to a read protections count request when an empty protections array is returned from NPS" in {
      when(mockProtectionService.readExistingProtections(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(model.HttpResponseDetails(OK, JsSuccess(successfulReadResponseBodyEmptyProtections))))

      lazy val result = controller.readExistingProtectionsCount(testNino).apply(FakeRequest())

      status(result) must be(OK)
      (contentAsJson(result) \ "count").get must be(JsNumber(0))
    }

    "return a count of zero to a read protections count request when no protections array is returned from NPS" in {
      when(mockProtectionService.readExistingProtections(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(model.HttpResponseDetails(OK, JsSuccess(successfulReadResponseBodyNoProtections))))

      lazy val result: Future[Result] = controller.readExistingProtectionsCount(testNino).apply(FakeRequest())

      status(result) must be(OK)
      (contentAsJson(result) \ "count").get must be(JsNumber(0))
    }

    "return a count of one to a read protections count request when a protections array with a single protection is returned from NPS" in {
      when(mockProtectionService.readExistingProtections(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(model.HttpResponseDetails(OK, JsSuccess(successfulReadResponseBody))))

      lazy val result: Future[Result] = controller.readExistingProtectionsCount(testNino).apply(FakeRequest())
      status(result) must be(OK)
      (contentAsJson(result) \ "count").get must be(JsNumber(1))
    }

    "handle an OK status but non-parseable response body from NPS service to a read protections " +
      "count request by passing an INTERNAL_SERVER_ERROR back to the caller" in {
      when(mockProtectionService.readExistingProtections(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(model.HttpResponseDetails(OK, JsError())))

      lazy val result = controller.readExistingProtectionsCount(testNino).apply(FakeRequest())
      status(result) must be(INTERNAL_SERVER_ERROR)
    }
  }
}
