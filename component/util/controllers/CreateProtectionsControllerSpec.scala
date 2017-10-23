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

package util.controllers

import controllers.CreateProtectionsController
import util.IntegrationSpec
import play.api.http.Status._
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.test.{FakeHeaders, FakeRequest}

class CreateProtectionsControllerSpec extends IntegrationSpec {

  def fakeRequest(body: JsValue): FakeRequest[JsValue] = FakeRequest(
    method = "POST",
    uri = "",
    headers = FakeHeaders(Seq("content-type" -> "application.json")),
    body = body)

  def validCreateBody(ninoWithoutSuffix: String): JsObject = Json.parse(
    s"""
       |  {
       |      "nino": "$ninoWithoutSuffix",
       |      "pensionSchemeAdministratorCheckReference" : "PSA123456789",
       |      "protection": {
       |        "id": 1,
       |        "version": 1,
       |        "type": 1,
       |        "certificateDate": "2015-05-22",
       |        "certificateTime": "12:22:59",
       |        "status": 1,
       |        "protectionReference": "FP161234567890C",
       |        "relevantAmount": 1250000.00,
       |        "notificationID": 12
       |      }
       |    }
       |
    """.stripMargin).as[JsObject]

  def validResponseBody(ninoWithoutSuffix: String): String =
    s"""
       |{
       |    "nino": "$ninoWithoutSuffix",
       |    "pensionSchemeAdministratorCheckReference": "PSA12345678A",
       |    "protection": {
       |        "id": 1,
       |        "version": 1,
       |        "type": 0,
       |        "certificateDate": "2015-05-22",
       |        "certificateTime": "12:22:59",
       |        "status": 1,
       |        "protectionReference": "IP141234567890C",
       |        "relevantAmount": 1250000,
       |        "preADayPensionInPayment": 250000,
       |        "postADayBCE": 250000,
       |        "uncrystallisedRights": 500000,
       |        "nonUKRights": 250000,
       |        "pensionDebitAmount": 0,
       |        "notificationID": 5,
       |        "protectedAmount": 600000,
       |        "pensionDebitEnteredAmount": 300,
       |        "pensionDebitStartDate": "2015-01-29",
       |        "pensionDebitTotalAmount": 800
       |    }
       |}
      """.stripMargin

  "CreateProtectionsController" when {

    "submitting a successful application for IP2016" should {
      val ninoWithoutSuffix = "AA10001"
      mockNPSConnector(ninoWithoutSuffix, OK)
      mockAudit(OK)
      mockCitizenDetails("AA10001", OK)

      lazy val result = CreateProtectionsController.applyForProtection("AA10001A").apply(fakeRequest(validCreateBody(ninoWithoutSuffix)))
      "return a success response" in {
        status(result) shouldBe 200
      }

      "include the response in the body" in {
        bodyOf(result) shouldBe validResponseBody(ninoWithoutSuffix)
      }
    }

    "submitting a conflicting application for IP2016" should {

      "return a conflict response" in {

      }

      "include the response in the body" in {

      }
    }

    "submitting an application which results in a Service Unavailable response" should {

      "return a service_unavailable response" in {

      }

      "includes a body with the error message" in {

      }
    }

    "submitting an application which results in a generic 500 response" should {

      "return a internal_server_error response" in {

      }

      "includes a body with the error message" in {

      }
    }

    "submitting an application which results in a Unauthorised response" should {

      "return a unauthorised response" in {

      }

      "includes a body with the error message" in {

      }
    }

    "submitting an application which results in a generic 400 response" should {

      "return a internal_server_error response" in {

      }

      "includes a body with the error message" in {

      }
    }

    "submitting an application which results in a BadRequest response" should {

      "return a bad_request response" in {

      }

      "includes a body with the error message" in {

      }
    }
  }
}
