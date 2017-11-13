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

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http.Status._
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import play.api.test.{FakeHeaders, FakeRequest}
import util.IntegrationSpec

class CreateProtectionsControllerSpec extends IntegrationSpec {

  lazy val ws = app.injector.instanceOf[WSClient]

  def fakeRequest(body: JsValue): FakeRequest[JsValue] = FakeRequest(
    method = "POST",
    uri = "",
    headers = FakeHeaders(Seq("content-type" -> "application.json")),
    body = body)

  def validCreateIPBody(ninoWithoutSuffix: String): JsObject = Json.parse(
    s"""
       |  {
       |    "protectionType" : "IP2016",
       |    "relevantAmount" : 10000.0,
       |    "preADayPensionInPayment" : 1000.0,
       |    "postADayBenefitCrystallisationEvents" : 1000.0,
       |    "uncrystallisedRights" : 1000.0,
       |    "nonUKRights": 1000.0
       |  }
    """.stripMargin).as[JsObject]

  def validSubmissionIPBody(ninoWithoutSuffix: String): String = Json.parse(
    s"""
       |{
       |"nino" : "$ninoWithoutSuffix",
       |"protection" : {
       |  "type" : 3,
       |  "relevantAmount" : 10000,
       |  "postADayBCE" : 1000,
       |  "preADayPensionInPayment" : 1000,
       |  "uncrystallisedRights" : 1000,
       |  "nonUKRights" : 1000
       |  }
       |}
     """.stripMargin
  ).toString()

  def validResponseIPBody(ninoWithoutSuffix: String): String =
    s"""
       |{
       |    "nino": "$ninoWithoutSuffix",
       |    "pensionSchemeAdministratorCheckReference": "PSA12345678A",
       |    "protection": {
       |        "id": 1,
       |        "version": 1,
       |        "type": 3,
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

  def validResultIPBody(nino: String): String = {
    Json.parse(s"""
                  |{
                  |"nino":"$nino",
                  |"pensionSchemeAdministratorCheckReference":"PSA12345678A",
                  |"psaCheckReference":"PSA12345678A",
                  |"pensionDebitAmount":0,
                  |"uncrystallisedRights":500000,
                  |"protectionType":"IP2016",
                  |"protectedAmount":600000,
                  |"protectionID":1,
                  |"pensionDebitEnteredAmount":300,
                  |"notificationId":5,
                  |"protectionReference":"IP141234567890C",
                  |"postADayBenefitCrystallisationEvents":250000,
                  |"nonUKRights":250000,
                  |"pensionDebitStartDate":"2015-01-29",
                  |"version":1,"pensionDebitTotalAmount":800,
                  |"relevantAmount":1250000,"status":"Open",
                  |"preADayPensionInPayment":250000,
                  |"certificateDate":"2015-05-22T12:22:59"
                  |}
    """.stripMargin).toString()
  }

  def invalidResultBodyForCreate(ninoWithoutSuffix: String, status: Int, message: String): String = {
    s"POST of 'http://localhost:11111/pensions-lifetime-allowance/individual/$ninoWithoutSuffix/protection' returned $status. Response body: '$message'"
  }

  def badRequestResultBody(ninoWithoutSuffix: String, message: String): String = {
    s"POST of 'http://localhost:11111/pensions-lifetime-allowance/individual/$ninoWithoutSuffix/protection' returned 400 (Bad Request). Response body '$message'"
  }

  def validCreateFPBody(ninoWithoutSuffix: String): JsObject = Json.parse(
    s"""
       |  {
       |    "protectionType" : "FP2016",
       |    "relevantAmount" : 10000.0,
       |    "preADayPensionInPayment" : 1000.0,
       |    "postADayBenefitCrystallisationEvents" : 1000.0,
       |    "uncrystallisedRights" : 1000.0,
       |    "nonUKRights": 1000.0,
       |    "pensionDebits": [
       |      {
       |        "startDate": "2016-04-04",
       |        "amount": 1001.0
       |      }
       |    ]
       |  }
    """.stripMargin).as[JsObject]

  def validSubmissionFPBody(ninoWithoutSuffix: String): String = Json.parse(
    s"""
       |{
       |"nino" : "$ninoWithoutSuffix",
       |"pensionDebits": [
       |  {
       |    "pensionDebitStartDate":  "2016-04-04",
       |    "pensionDebitEnteredAmount": 1001.00
       |  }
       |],
       |"protection" : {
       |  "type" : 1,
       |  "relevantAmount" : 10000,
       |  "postADayBCE" : 1000,
       |  "preADayPensionInPayment" : 1000,
       |  "uncrystallisedRights" : 1000,
       |  "nonUKRights" : 1000
       |  }
       |}
     """.stripMargin
  ).toString()

  def validResponseFPBody(ninoWithoutSuffix: String): String =
    s"""
       |{
       |    "nino": "$ninoWithoutSuffix",
       |    "pensionSchemeAdministratorCheckReference": "PSA12345678A",
       |    "protection": {
       |        "id": 1,
       |        "version": 1,
       |        "type": 1,
       |        "certificateDate": "2015-05-22",
       |        "certificateTime": "12:22:59",
       |        "status": 1,
       |        "protectionReference": "FP141234567890C",
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

  def validResultFPBody(nino: String): String = {
    Json.parse(s"""
                  |{
                  |"nino":"$nino",
                  |"pensionSchemeAdministratorCheckReference":"PSA12345678A",
                  |"psaCheckReference":"PSA12345678A",
                  |"pensionDebitAmount":0,
                  |"uncrystallisedRights":500000,
                  |"protectionType":"FP2016",
                  |"protectedAmount":600000,
                  |"protectionID":1,
                  |"pensionDebitEnteredAmount":300,
                  |"notificationId":5,
                  |"protectionReference":"FP141234567890C",
                  |"postADayBenefitCrystallisationEvents":250000,
                  |"nonUKRights":250000,
                  |"pensionDebitStartDate":"2015-01-29",
                  |"version":1,"pensionDebitTotalAmount":800,
                  |"relevantAmount":1250000,"status":"Open",
                  |"preADayPensionInPayment":250000,
                  |"certificateDate":"2015-05-22T12:22:59"
                  |}
    """.stripMargin).toString()
  }

  def sucessfulAuditResult(nino: String, ninoWithoutSuffix: String, protectionType: Int, status: Int): String = Json.parse(
    s"""
       |{
       |"auditSource" : "pensions-lifetime-allowance",
       |"auditType" : "CreateAllowance",
       |"tags" : {
       |  "clientIP" : "-",
       |  "path" : "http://localhost:11111/pensions-lifetime-allowance/individual/$ninoWithoutSuffix/protection",
       |  "X-Session-ID" : "session-12345",
       |  "Akamai-Reputation" : "-",
       |  "X-Request-ID" : "-",
       |  "clientPort" : "-",
       |  "transactionName" : "create-pensions-lifetime-allowance"
       |},
       |"detail" : {
       |  "nino" : "$nino",
       |  "protectionType" : "$protectionType",
       |  "statusCode" : "$status",
       |  "protectionStatus" : "1"
       |}
       |}
     """.stripMargin
  ).toString()

  def failureAuditResponse(nino: String, ninoWithoutSuffix: String, protectionType: Int, status: Int, message: String): String = Json.parse(
    s"""
       |{
       |"auditSource" : "pensions-lifetime-allowance",
       |"auditType" : "OutboundCall",
       |"request" : {
       |  "tags" : {
       |    "clientIP" : "-",
       |    "path" : "http://localhost:11111/pensions-lifetime-allowance/individual/$ninoWithoutSuffix/protection",
       |    "X-Session-ID" : "session-12345",
       |    "Akamai-Reputation" : "-",
       |    "X-Request-ID" : "-",
       |    "clientPort" : "-",
       |    "transactionName" : "http://localhost:11111/pensions-lifetime-allowance/individual/$ninoWithoutSuffix/protection"
       |  },
       |  "detail" : {
       |    "method" : "POST",
       |    "path" : "http://localhost:11111/pensions-lifetime-allowance/individual/$ninoWithoutSuffix/protection",
       |    "deviceID" : "-",
       |    "ipAddress" : "-",
       |    "token" : "-",
       |    "Authorization" : "Bearer accessToken"
       |  }
       |},
       |"response" : {
       |  "tags" : { },
       |  "detail" : {
       |    "responseMessage" : "$message",
       |    "statusCode" : "$status"
       |  }
       |}
       |}
     """.stripMargin
  ).toString()

  "CreateProtectionsController" when {

    "submitting a successful application for IP2016" should {
      val ninoWithoutSuffix = "AA100001"
      val nino = s"${ninoWithoutSuffix}A"

      def testMocks(): Unit = {
        mockNPSConnector(ninoWithoutSuffix, OK, validResponseIPBody(ninoWithoutSuffix))
        mockAuth(OK, nino)
        mockAudit(OK)
        mockCitizenDetails(nino, OK)
      }

      def client(path: String): WSRequest = ws.url(s"http://localhost:$port/protect-your-lifetime-allowance/$path")
        .withFollowRedirects(false)
        .withHeaders(("X-Session-ID","session-12345"))

      def result: WSResponse = client(s"individuals/$nino/protections").post(validCreateIPBody(ninoWithoutSuffix)).futureValue

     "return a success response" in {
        testMocks()
        result.status shouldBe 200
      }

      "include the response in the body" in {
        testMocks()
        result.body shouldBe validResultIPBody(nino)
      }

      "submit the correct json to nps" in {
        testMocks()
        result
        verify(postRequestedFor(urlEqualTo(s"/pensions-lifetime-allowance/individual/$ninoWithoutSuffix/protection"))
          .withRequestBody(equalToJson(validSubmissionIPBody(ninoWithoutSuffix), false, false))
        )
      }

      "submit the correct auditing data" in {
        testMocks()
        result
        verify(postRequestedFor(urlEqualTo("/write/audit"))
          .withRequestBody(equalToJson(sucessfulAuditResult(nino, ninoWithoutSuffix, 3, OK), false, true))
        )
      }
    }

    "submitting a conflicting application for IP2016" should {

      val ninoWithoutSuffix = "AA100002"
      val nino = s"${ninoWithoutSuffix}A"

      def testMocks(): Unit = {
        mockNPSConnector(ninoWithoutSuffix, CONFLICT, validResponseIPBody(ninoWithoutSuffix))
        mockAuth(OK, nino)
        mockAudit(OK)
        mockCitizenDetails(nino, OK)
      }

      def client(path: String): WSRequest = ws.url(s"http://localhost:$port/protect-your-lifetime-allowance/$path")
        .withFollowRedirects(false)
        .withHeaders(("X-Session-ID","session-12345"))

      def result: WSResponse = client(s"individuals/$nino/protections").post(validCreateIPBody(ninoWithoutSuffix)).futureValue

      "return a conflict response" in {
        testMocks()
        result.status shouldBe CONFLICT
      }

      "include the response in the body" in {
        testMocks()
        result.body shouldBe validResultIPBody(nino)
      }

      "submit the correct json to nps" in {
        testMocks()
        result
        verify(postRequestedFor(urlEqualTo(s"/pensions-lifetime-allowance/individual/$ninoWithoutSuffix/protection"))
          .withRequestBody(equalToJson(validSubmissionIPBody(ninoWithoutSuffix), false, true))
        )
      }

      "submit the correct auditing data" in {
        testMocks()
        result
        verify(postRequestedFor(urlEqualTo("/write/audit"))
          .withRequestBody(equalToJson(sucessfulAuditResult(nino, ninoWithoutSuffix, 3, CONFLICT), false, true))
        )
      }
    }

    "submitting an application which results in a Service Unavailable response" should {

      val ninoWithoutSuffix = "AA100002"
      val nino = s"${ninoWithoutSuffix}A"

      def testMocks(): Unit = {
        mockNPSConnector(ninoWithoutSuffix, SERVICE_UNAVAILABLE, "error message")
        mockAuth(OK, nino)
        mockAudit(OK)
        mockCitizenDetails(nino, OK)
      }

      def client(path: String): WSRequest = ws.url(s"http://localhost:$port/protect-your-lifetime-allowance/$path")
        .withFollowRedirects(false)
        .withHeaders(("X-Session-ID","session-12345"))

      def result: WSResponse = client(s"individuals/$nino/protections").post(validCreateIPBody(ninoWithoutSuffix)).futureValue


      "return a service_unavailable response" in {
        testMocks()
        result.status shouldBe SERVICE_UNAVAILABLE
      }

      "includes a body with the error message" in {
        testMocks()
        result.body shouldBe invalidResultBodyForCreate(ninoWithoutSuffix, SERVICE_UNAVAILABLE, "error message")
      }

      "submit the correct json to nps" in {
        testMocks()
        result
        verify(postRequestedFor(urlEqualTo(s"/pensions-lifetime-allowance/individual/$ninoWithoutSuffix/protection"))
          .withRequestBody(equalToJson(validSubmissionIPBody(ninoWithoutSuffix), false, true))
        )
      }

      "submit the correct auditing data" in {
        testMocks()
        result
        verify(postRequestedFor(urlEqualTo("/write/audit/merged"))
          .withRequestBody(equalToJson(failureAuditResponse(nino, ninoWithoutSuffix, 3, SERVICE_UNAVAILABLE, "error message"), false, true))
        )
      }
    }

    "submitting an application which results in a generic 500 response" should {

      val ninoWithoutSuffix = "AA100002"
      val nino = s"${ninoWithoutSuffix}A"

      def testMocks(): Unit = {
        mockNPSConnector(ninoWithoutSuffix, BAD_GATEWAY, "error message")
        mockAuth(OK, nino)
        mockAudit(OK)
        mockCitizenDetails(nino, OK)
      }

      def client(path: String): WSRequest = ws.url(s"http://localhost:$port/protect-your-lifetime-allowance/$path")
        .withFollowRedirects(false)
        .withHeaders(("X-Session-ID","session-12345"))

      def result: WSResponse = client(s"individuals/$nino/protections").post(validCreateIPBody(ninoWithoutSuffix)).futureValue

      "return a internal_server_error response" in {
        testMocks()
        result.status shouldBe INTERNAL_SERVER_ERROR
      }

      "includes a body with the error message" in {
        testMocks()
        result.body shouldBe invalidResultBodyForCreate(ninoWithoutSuffix, BAD_GATEWAY, "error message")
      }

      "submit the correct json to nps" in {
        testMocks()
        result
        verify(postRequestedFor(urlEqualTo(s"/pensions-lifetime-allowance/individual/$ninoWithoutSuffix/protection"))
          .withRequestBody(equalToJson(validSubmissionIPBody(ninoWithoutSuffix), false, true))
        )
      }

      "submit the correct auditing data" in {
        testMocks()
        result
        verify(postRequestedFor(urlEqualTo("/write/audit/merged"))
          .withRequestBody(equalToJson(failureAuditResponse(nino, ninoWithoutSuffix, 3, BAD_GATEWAY, "error message"), false, true))
        )
      }
    }

    "submitting an application which results in a Unauthorised response" should {

      val ninoWithoutSuffix = "AA100002"
      val nino = s"${ninoWithoutSuffix}A"

      def testMocks(): Unit = {
        mockNPSConnector(ninoWithoutSuffix, UNAUTHORIZED, "error message")
        mockAuth(OK, nino)
        mockAudit(OK)
        mockCitizenDetails(nino, OK)
      }

      def client(path: String): WSRequest = ws.url(s"http://localhost:$port/protect-your-lifetime-allowance/$path")
        .withFollowRedirects(false)
        .withHeaders(("X-Session-ID","session-12345"))

      def result: WSResponse = client(s"individuals/$nino/protections").post(validCreateIPBody(ninoWithoutSuffix)).futureValue

      "return a unauthorised response" in {
        testMocks()
        result.status shouldBe UNAUTHORIZED
      }

      "includes a body with the error message" in {
        testMocks()
        result.body shouldBe invalidResultBodyForCreate(ninoWithoutSuffix, UNAUTHORIZED, "error message")
      }

      "submit the correct json to nps" in {
        testMocks()
        result
        verify(postRequestedFor(urlEqualTo(s"/pensions-lifetime-allowance/individual/$ninoWithoutSuffix/protection"))
          .withRequestBody(equalToJson(validSubmissionIPBody(ninoWithoutSuffix), false, true))
        )
      }

      "submit the correct auditing data" in {
        testMocks()
        result
        verify(postRequestedFor(urlEqualTo("/write/audit/merged"))
          .withRequestBody(equalToJson(failureAuditResponse(nino, ninoWithoutSuffix, 3, UNAUTHORIZED, "error message"), false, true))
        )
      }
    }

    "submitting an application which results in a generic 400 response" should {

      val ninoWithoutSuffix = "AA100002"
      val nino = s"${ninoWithoutSuffix}A"

      def testMocks(): Unit = {
        mockNPSConnector(ninoWithoutSuffix, FORBIDDEN, "error message")
        mockAuth(OK, nino)
        mockAudit(OK)
        mockCitizenDetails(nino, OK)
      }

      def client(path: String): WSRequest = ws.url(s"http://localhost:$port/protect-your-lifetime-allowance/$path")
        .withFollowRedirects(false)
        .withHeaders(("X-Session-ID","session-12345"))

      def result: WSResponse = client(s"individuals/$nino/protections").post(validCreateIPBody(ninoWithoutSuffix)).futureValue

      "return a internal_server_error response" in {
        testMocks()
        result.status shouldBe INTERNAL_SERVER_ERROR
      }

      "includes a body with the error message" in {
        testMocks()
        result.body shouldBe invalidResultBodyForCreate(ninoWithoutSuffix, FORBIDDEN, "error message")
      }

      "submit the correct json to nps" in {
        testMocks()
        result
        verify(postRequestedFor(urlEqualTo(s"/pensions-lifetime-allowance/individual/$ninoWithoutSuffix/protection"))
          .withRequestBody(equalToJson(validSubmissionIPBody(ninoWithoutSuffix), false, true))
        )
      }

      "submit the correct auditing data" in {
        testMocks()
        result
        verify(postRequestedFor(urlEqualTo("/write/audit/merged"))
          .withRequestBody(equalToJson(failureAuditResponse(nino, ninoWithoutSuffix, 3, FORBIDDEN, "error message"), false, true))
        )
      }
    }

    "submitting an application which results in a BadRequest response" should {

      val ninoWithoutSuffix = "AA100002"
      val nino = s"${ninoWithoutSuffix}A"

      def testMocks(): Unit = {
        mockNPSConnector(ninoWithoutSuffix, BAD_REQUEST, "error message")
        mockAuth(OK, nino)
        mockAudit(OK)
        mockCitizenDetails(nino, OK)
      }

      def client(path: String): WSRequest = ws.url(s"http://localhost:$port/protect-your-lifetime-allowance/$path")
        .withFollowRedirects(false)
        .withHeaders(("X-Session-ID","session-12345"))

      def result: WSResponse = client(s"individuals/$nino/protections").post(validCreateIPBody(ninoWithoutSuffix)).futureValue

      "return a bad_request response" in {
        testMocks()
        result.status shouldBe BAD_REQUEST
      }

      "includes a body with the error message" in {
        testMocks()
        result.body shouldBe badRequestResultBody(ninoWithoutSuffix, "error message")
      }

      "submit the correct json to nps" in {
        testMocks()
        result
        verify(postRequestedFor(urlEqualTo(s"/pensions-lifetime-allowance/individual/$ninoWithoutSuffix/protection"))
          .withRequestBody(equalToJson(validSubmissionIPBody(ninoWithoutSuffix), false, true))
        )
      }

      "submit the correct auditing data" in {
        testMocks()
        result
        verify(postRequestedFor(urlEqualTo("/write/audit/merged"))
          .withRequestBody(equalToJson(failureAuditResponse(nino, ninoWithoutSuffix, 3, BAD_REQUEST, "error message"), false, true))
        )
      }
    }

    "submitting a successful application for FP2016" should {
      val ninoWithoutSuffix = "AA100001"
      val nino = s"${ninoWithoutSuffix}A"

      def testMocks(): Unit = {
        mockNPSConnector(ninoWithoutSuffix, OK, validResponseFPBody(ninoWithoutSuffix))
        mockAuth(OK, nino)
        mockAudit(OK)
        mockCitizenDetails(nino, OK)
      }

      def client(path: String): WSRequest = ws.url(s"http://localhost:$port/protect-your-lifetime-allowance/$path")
        .withFollowRedirects(false)
        .withHeaders(("X-Session-ID","session-12345"))

      def result: WSResponse = client(s"individuals/$nino/protections").post(validCreateFPBody(ninoWithoutSuffix)).futureValue

      "return a success response" in {
        testMocks()
        result.status shouldBe 200
      }

      "include the response in the body" in {
        testMocks()
        result.body shouldBe validResultFPBody(nino)
      }

      "submit the correct json to nps" in {
        testMocks()
        result
        verify(postRequestedFor(urlEqualTo(s"/pensions-lifetime-allowance/individual/$ninoWithoutSuffix/protection"))
          .withRequestBody(equalToJson(validSubmissionFPBody(ninoWithoutSuffix), false, true))
        )
      }

      "submit the correct auditing data" in {
        testMocks()
        result
        verify(postRequestedFor(urlEqualTo("/write/audit"))
          .withRequestBody(equalToJson(sucessfulAuditResult(nino, ninoWithoutSuffix, 1, OK), false, true))
        )
      }
    }

    "submitting a conflicting application for FP2016" should {

      val ninoWithoutSuffix = "AA100002"
      val nino = s"${ninoWithoutSuffix}A"

      def testMocks(): Unit = {
        mockNPSConnector(ninoWithoutSuffix, CONFLICT, validResponseFPBody(ninoWithoutSuffix))
        mockAuth(OK, nino)
        mockAudit(OK)
        mockCitizenDetails(nino, OK)
      }

      def client(path: String): WSRequest = ws.url(s"http://localhost:$port/protect-your-lifetime-allowance/$path")
        .withFollowRedirects(false)
        .withHeaders(("X-Session-ID","session-12345"))

      def result: WSResponse = client(s"individuals/$nino/protections").post(validCreateFPBody(ninoWithoutSuffix)).futureValue

      "return a conflict response" in {
        testMocks()
        result.status shouldBe CONFLICT
      }

      "include the response in the body" in {
        testMocks()
        result.body shouldBe validResultFPBody(nino)
      }

      "submit the correct json to nps" in {
        testMocks()
        result
        verify(postRequestedFor(urlEqualTo(s"/pensions-lifetime-allowance/individual/$ninoWithoutSuffix/protection"))
          .withRequestBody(equalToJson(validSubmissionFPBody(ninoWithoutSuffix), false, true))
        )
      }

      "submit the correct auditing data" in {
        testMocks()
        result
        verify(postRequestedFor(urlEqualTo("/write/audit"))
          .withRequestBody(equalToJson(sucessfulAuditResult(nino, ninoWithoutSuffix, 1, CONFLICT), false, true))
        )
      }
    }

    "submitting an FP application which results in a Service Unavailable response" should {

      val ninoWithoutSuffix = "AA100002"
      val nino = s"${ninoWithoutSuffix}A"

      def testMocks(): Unit = {
        mockNPSConnector(ninoWithoutSuffix, SERVICE_UNAVAILABLE, "error message")
        mockAuth(OK, nino)
        mockAudit(OK)
        mockCitizenDetails(nino, OK)
      }

      def client(path: String): WSRequest = ws.url(s"http://localhost:$port/protect-your-lifetime-allowance/$path")
        .withFollowRedirects(false)
        .withHeaders(("X-Session-ID","session-12345"))

      def result: WSResponse = client(s"individuals/$nino/protections").post(validCreateFPBody(ninoWithoutSuffix)).futureValue

      "return a service_unavailable response" in {
        testMocks()
        result.status shouldBe SERVICE_UNAVAILABLE
      }

      "includes a body with the error message" in {
        testMocks()
        result.body shouldBe invalidResultBodyForCreate(ninoWithoutSuffix, SERVICE_UNAVAILABLE, "error message")
      }

      "submit the correct json to nps" in {
        testMocks()
        result
        verify(postRequestedFor(urlEqualTo(s"/pensions-lifetime-allowance/individual/$ninoWithoutSuffix/protection"))
          .withRequestBody(equalToJson(validSubmissionFPBody(ninoWithoutSuffix), false, true))
        )
      }

      "submit the correct auditing data" in {
        testMocks()
        result
        verify(postRequestedFor(urlEqualTo("/write/audit/merged"))
          .withRequestBody(equalToJson(failureAuditResponse(nino, ninoWithoutSuffix, 1, SERVICE_UNAVAILABLE, "error message"), false, true))
        )
      }
    }

    "submitting an FP application which results in a generic 500 response" should {

      val ninoWithoutSuffix = "AA100002"
      val nino = s"${ninoWithoutSuffix}A"

      def testMocks(): Unit = {
        mockNPSConnector(ninoWithoutSuffix, BAD_GATEWAY, "error message")
        mockAuth(OK, nino)
        mockAudit(OK)
        mockCitizenDetails(nino, OK)
      }

      def client(path: String): WSRequest = ws.url(s"http://localhost:$port/protect-your-lifetime-allowance/$path")
        .withFollowRedirects(false)
        .withHeaders(("X-Session-ID","session-12345"))

      def result: WSResponse = client(s"individuals/$nino/protections").post(validCreateFPBody(ninoWithoutSuffix)).futureValue

      "return a internal_server_error response" in {
        testMocks()
        result.status shouldBe INTERNAL_SERVER_ERROR
      }

      "includes a body with the error message" in {
        testMocks()
        result.body shouldBe invalidResultBodyForCreate(ninoWithoutSuffix, BAD_GATEWAY, "error message")
      }

      "submit the correct json to nps" in {
        testMocks()
        result
        verify(postRequestedFor(urlEqualTo(s"/pensions-lifetime-allowance/individual/$ninoWithoutSuffix/protection"))
          .withRequestBody(equalToJson(validSubmissionFPBody(ninoWithoutSuffix), false, true))
        )
      }

      "submit the correct auditing data" in {
        testMocks()
        result
        verify(postRequestedFor(urlEqualTo("/write/audit/merged"))
          .withRequestBody(equalToJson(failureAuditResponse(nino, ninoWithoutSuffix, 1, BAD_GATEWAY, "error message"), false, true))
        )
      }
    }

    "submitting an FP application which results in a Unauthorised response" should {

      val ninoWithoutSuffix = "AA100002"
      val nino = s"${ninoWithoutSuffix}A"

      def testMocks(): Unit = {
        mockNPSConnector(ninoWithoutSuffix, UNAUTHORIZED, "error message")
        mockAuth(OK, nino)
        mockAudit(OK)
        mockCitizenDetails(nino, OK)
      }

      def client(path: String): WSRequest = ws.url(s"http://localhost:$port/protect-your-lifetime-allowance/$path")
        .withFollowRedirects(false)
        .withHeaders(("X-Session-ID","session-12345"))

      def result: WSResponse = client(s"individuals/$nino/protections").post(validCreateFPBody(ninoWithoutSuffix)).futureValue

      "return a unauthorised response" in {
        testMocks()
        result.status shouldBe UNAUTHORIZED
      }

      "includes a body with the error message" in {
        testMocks()
        result.body shouldBe invalidResultBodyForCreate(ninoWithoutSuffix, UNAUTHORIZED, "error message")
      }

      "submit the correct json to nps" in {
        testMocks()
        result
        verify(postRequestedFor(urlEqualTo(s"/pensions-lifetime-allowance/individual/$ninoWithoutSuffix/protection"))
          .withRequestBody(equalToJson(validSubmissionFPBody(ninoWithoutSuffix), false, true))
        )
      }

      "submit the correct auditing data" in {
        testMocks()
        result
        verify(postRequestedFor(urlEqualTo("/write/audit/merged"))
          .withRequestBody(equalToJson(failureAuditResponse(nino, ninoWithoutSuffix, 1, UNAUTHORIZED, "error message"), false, true))
        )
      }
    }

    "submitting an FP application which results in a generic 400 response" should {

      val ninoWithoutSuffix = "AA100002"
      val nino = s"${ninoWithoutSuffix}A"

      def testMocks(): Unit = {
        mockNPSConnector(ninoWithoutSuffix, FORBIDDEN, "error message")
        mockAuth(OK, nino)
        mockAudit(OK)
        mockCitizenDetails(nino, OK)
      }

      def client(path: String): WSRequest = ws.url(s"http://localhost:$port/protect-your-lifetime-allowance/$path")
        .withFollowRedirects(false)
        .withHeaders(("X-Session-ID","session-12345"))

      def result: WSResponse = client(s"individuals/$nino/protections").post(validCreateFPBody(ninoWithoutSuffix)).futureValue

      "return a internal_server_error response" in {
        testMocks()
        result.status shouldBe INTERNAL_SERVER_ERROR
      }

      "includes a body with the error message" in {
        testMocks()
        result.body shouldBe invalidResultBodyForCreate(ninoWithoutSuffix, FORBIDDEN, "error message")
      }

      "submit the correct json to nps" in {
        testMocks()
        result
        verify(postRequestedFor(urlEqualTo(s"/pensions-lifetime-allowance/individual/$ninoWithoutSuffix/protection"))
          .withRequestBody(equalToJson(validSubmissionFPBody(ninoWithoutSuffix), false, true))
        )
      }

      "submit the correct auditing data" in {
        testMocks()
        result
        verify(postRequestedFor(urlEqualTo("/write/audit/merged"))
          .withRequestBody(equalToJson(failureAuditResponse(nino, ninoWithoutSuffix, 1, FORBIDDEN, "error message"), false, true))
        )
      }
    }

    "submitting an FP application which results in a BadRequest response" should {

      val ninoWithoutSuffix = "AA100002"
      val nino = s"${ninoWithoutSuffix}A"

      def testMocks(): Unit = {
        mockNPSConnector(ninoWithoutSuffix, BAD_REQUEST, "error message")
        mockAuth(OK, nino)
        mockAudit(OK)
        mockCitizenDetails(nino, OK)
      }

      def client(path: String): WSRequest = ws.url(s"http://localhost:$port/protect-your-lifetime-allowance/$path")
        .withFollowRedirects(false)
        .withHeaders(("X-Session-ID","session-12345"))

      def result: WSResponse = client(s"individuals/$nino/protections").post(validCreateFPBody(ninoWithoutSuffix)).futureValue

      "return a bad_request response" in {
        testMocks()
        result.status shouldBe BAD_REQUEST
      }

      "includes a body with the error message" in {
        testMocks()
        result.body shouldBe badRequestResultBody(ninoWithoutSuffix, "error message")
      }

      "submit the correct json to nps" in {
        testMocks()
        result
        verify(postRequestedFor(urlEqualTo(s"/pensions-lifetime-allowance/individual/$ninoWithoutSuffix/protection"))
          .withRequestBody(equalToJson(validSubmissionFPBody(ninoWithoutSuffix), false, true))
        )
      }

      "submit the correct auditing data" in {
        testMocks()
        result
        verify(postRequestedFor(urlEqualTo("/write/audit/merged"))
          .withRequestBody(equalToJson(failureAuditResponse(nino, ninoWithoutSuffix, 1, BAD_REQUEST, "error message"), false, true))
        )
      }
    }
  }
}