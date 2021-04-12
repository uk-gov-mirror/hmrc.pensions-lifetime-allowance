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
import connectors.{CitizenDetailsConnector, CitizenRecordOK, NpsConnector}
import org.mockito.ArgumentMatchers
import _root_.mock.AuthMock
import org.mockito.Mockito.when
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatestplus.play.PlaySpec
import play.api.mvc.{ControllerComponents, Result}
import util.NinoHelper
import play.api.libs.json._
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.domain.Generator

import scala.concurrent.Future
import uk.gov.hmrc.http.{HeaderCarrier, Upstream5xxResponse, UpstreamErrorResponse}

class AmendProtectionsControllerSpec  extends PlaySpec with GuiceOneServerPerSuite with AuthMock {

  val rand = new Random()
  val ninoGenerator = new Generator(rand)
  val mockCitizenDetailsConnector: CitizenDetailsConnector = mock[CitizenDetailsConnector]

  when(mockCitizenDetailsConnector.checkCitizenRecord(ArgumentMatchers.any[String])(ArgumentMatchers.any(), ArgumentMatchers.any()))
    .thenReturn(Future.successful(CitizenRecordOK))

  mockAuthConnector(Future.successful({}))

  def randomNino: String = ninoGenerator.nextNino.nino.replaceFirst("MA", "AA")

  val testNino = randomNino
  val (testNinoWithoutSuffix, _) = NinoHelper.dropNinoSuffix(testNino)
  val testProtectionId = 1
  val testProtectionVersion = 1
  implicit lazy val hc = mock[HeaderCarrier]
  val mockNpsConnector = mock[NpsConnector]

  lazy val cc = app.injector.instanceOf[ControllerComponents]
  lazy val controller = new AmendProtectionsController(mockAuthConnector,
    citizenDetailsConnector = mockCitizenDetailsConnector,
    testProtectionService, cc
    )

  val validAmendBody = Json.parse(
    s"""
       |{
       | "protectionType": "IP2016",
       | "status": "Open",
       | "version": $testProtectionVersion,
       | "relevantAmount": 1250000.00,
       | "postADayBenefitCrystallisationEvents": 250000.00,
       | "preADayPensionInPayment": 250000.00,
       | "nonUKRights": 250000.00,
       | "uncrystallisedRights": 500000.00
       |}
    """.stripMargin
  )

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
    override val npsConnector = mockNpsConnector
  }


  "AmendProtectionController" when {
    "respond to an invalid Amend Protection request with BAD_REQUEST" in {

      val fakeRequest = FakeRequest(
        method = "PUT",
        uri = "",
        headers = FakeHeaders(Seq("content-type" -> "application.json")),
        body = invalidAmendBody)

      val result = controller.amendProtection(testNino, testProtectionId.toString)(fakeRequest)
      status(result) must be(BAD_REQUEST)
    }

    "respond to a valid Amend Protection request with OK" in {
      when(mockNpsConnector.amendProtection(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(model.HttpResponseDetails(200, JsSuccess(successfulAmendIP2016NPSResponseBody))))

      val fakeRequest: FakeRequest[JsValue] = FakeRequest(
        method = "PUT",
        uri = "",
        headers = FakeHeaders(Seq("content-type" -> "application.json")),
        body = validAmendBody)

      val result: Future[Result] = controller.amendProtection(testNino, testProtectionId.toString).apply(fakeRequest)
      status(result) must be(OK)
    }

    "handle a 500 (INTERNAL_SERVER_ERROR) response from NPS service by passing it back to the caller" in {
      when(mockNpsConnector.amendProtection(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("test", INTERNAL_SERVER_ERROR, BAD_GATEWAY)))

      val fakeRequest = FakeRequest(
        method = "PUT",
        uri = "",
        headers = FakeHeaders(Seq("content-type" -> "application.json")),
        body = validAmendBody)

      val result = controller.amendProtection(testNino, testProtectionId.toString).apply(fakeRequest)
      status(result) must be(INTERNAL_SERVER_ERROR)
    }

    "return a 400 when an invalid ID is provided" in {
      val fakeRequest = FakeRequest(
        method = "PUT",
        uri = "",
        headers = FakeHeaders(Seq("content-type" -> "application.json")),
        body = validAmendBody)

      val result = controller.amendProtection(testNino, "not a long").apply(fakeRequest)
      status(result) must be(BAD_REQUEST)
    }
  }
}

