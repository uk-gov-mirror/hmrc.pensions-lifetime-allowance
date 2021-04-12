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

class AmendProtectionsControllerSpec extends IntegrationSpec {

  lazy val ws = app.injector.instanceOf[WSClient]

  def fakeRequest(body: JsValue): FakeRequest[JsValue] = FakeRequest(
    method = "PUT",
    uri = "",
    headers = FakeHeaders(Seq("content-type" -> "application.json")),
    body = body)

  def validAmendIPBody(ninoWithoutSuffix: String, protectionType: String): JsObject = Json.parse(
    s"""
       |  {
       |    "protectionType" : "$protectionType",
       |    "version" : 1,
       |    "status" : "1",
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

  def validResponseIPBody(ninoWithoutSuffix: String, protectionType: Int): String =
    s"""
       |{
       |    "nino": "$ninoWithoutSuffix",
       |    "pensionSchemeAdministratorCheckReference": "PSA12345645A",
       |    "protection": {
       |        "id": 1,
       |        "version": 1,
       |        "type": $protectionType,
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
       |        "protectedAmount": 700,
       |        "pensionDebitEnteredAmount": 6000,
       |        "pensionDebitStartDate": "2015-05-25",
       |        "pensionDebitTotalAmount": 14000
       |    }
       |}
      """.stripMargin

  def validResultIPBody(nino: String, protectionType: String): String = {
    Json.parse(
      s"""
         |{
         |	"nino":"$nino",
         |	"psaCheckReference":"PSA12345645A",
         | 	"protectionID":1,
         |	"certificateDate":"2015-05-22T12:22:59",
         | 	"version":1,
         |  "protectionType":"$protectionType",
         |	"status":"Open",
         |	"protectedAmount":700,
         |	"relevantAmount":1250000,
         |	"postADayBenefitCrystallisationEvents":250000,
         |	"preADayPensionInPayment":250000,
         |	"uncrystallisedRights":500000,
         |	"nonUKRights":250000,
         | 	"pensionDebitAmount":0,
         |	"pensionDebitEnteredAmount":6000,
         |	"pensionDebitStartDate":"2015-05-25",
         |	"pensionDebitTotalAmount":14000,
         |	"notificationId":5,
         |	"protectionReference":"IP141234567890C"
         |}
    """.stripMargin).toString()
  }

  def validAmendSubmissionIPBody(ninoWithoutSuffix: String, protectionType: Int) = Json.parse(
    s"""
       |{
       |  "nino" : "$ninoWithoutSuffix",
       |  "pensionDebits": [
       |    {
       |      "pensionDebitStartDate": "2016-04-04",
       |      "pensionDebitEnteredAmount": 1001.0
       |    }
       |  ],
       |  "protection" : {
       |    "type" : $protectionType,
       |    "status" : -1,
       |    "version" : 1,
       |    "relevantAmount" : 10000,
       |    "postADayBCE" : 1000,
       |    "preADayPensionInPayment" : 1000,
       |    "uncrystallisedRights" : 1000,
       |    "nonUKRights" : 1000,
       |    "id" : 1
       |  }
       |}
    """.stripMargin).toString()

  def sucessfulAuditResult(nino: String, ninoWithoutSuffix: String, protectionId: Long, protectionType: Int): String = Json.parse(
    s"""
       |{
       |  "auditSource" : "pensions-lifetime-allowance",
       |  "auditType" : "AmendAllowance",
       |  "tags" : {
       |    "clientIP" : "-",
       |    "path" : "http://localhost:11111/pensions-lifetime-allowance/individual/$ninoWithoutSuffix/protections/$protectionId",
       |    "X-Session-ID" : "session-12345",
       |    "Akamai-Reputation" : "-",
       |    "X-Request-ID" : "-",
       |    "clientPort" : "-",
       |    "transactionName" : "amend-pensions-lifetime-allowance"
       |  },
       |  "detail" : {
       |    "statusCode" : "200",
       |    "nino" : "$nino",
       |    "protectionId" : "$protectionId",
       |    "protectionType" : "$protectionType",
       |    "protectionStatus" : "1"
       |  }
       |}
     """.stripMargin
  ).toString()

  def invalidResultBodyForAmend(ninoWithoutSuffix: String, status: Int, message: String, id: Long): String = {
    s"PUT of 'http://localhost:11111/pensions-lifetime-allowance/individual/$ninoWithoutSuffix/protections/$id' returned $status. Response body: '$message'"
  }

  def failureAuditResponse(nino: String, ninoWithoutSuffix: String, protectionType: Int, status: Int, message: String, id: Long): String = Json.parse(
    s"""
       |{
       |"auditSource" : "pensions-lifetime-allowance",
       |"auditType" : "OutboundCall",
       |"request" : {
       |  "tags" : {
       |    "clientIP" : "-",
       |    "path" : "http://localhost:11111/pensions-lifetime-allowance/individual/$ninoWithoutSuffix/protections/$id",
       |    "X-Session-ID" : "session-12345",
       |    "Akamai-Reputation" : "-",
       |    "X-Request-ID" : "-",
       |    "clientPort" : "-",
       |    "transactionName" : "http://localhost:11111/pensions-lifetime-allowance/individual/$ninoWithoutSuffix/protections/$id"
       |  },
       |  "detail" : {
       |    "method" : "PUT",
       |    "path" : "http://localhost:11111/pensions-lifetime-allowance/individual/$ninoWithoutSuffix/protections/$id",
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

  def badRequestResultBody(ninoWithoutSuffix: String, message: String, id: Long): String = {
    s"PUT of 'http://localhost:11111/pensions-lifetime-allowance/individual/$ninoWithoutSuffix/protections/$id' returned 400 (Bad Request). Response body '$message'"
  }


  "AmendProtectionsController" when {

    "amending an IP2016 application" in {
      val ninoWithoutSuffix = "AA100001"
      val nino = s"${ninoWithoutSuffix}A"
      val id = 1

      def testMocks(): Unit = {
        mockAmend(ninoWithoutSuffix, OK, validResponseIPBody(ninoWithoutSuffix, 3), id)
        mockAuth(OK)
        mockAudit(NO_CONTENT)
        mockCitizenDetails(nino, OK)
      }

      def client(path: String): WSRequest = ws.url(s"http://localhost:$port/protect-your-lifetime-allowance/$path")
        .withFollowRedirects(false)
        .withHeaders(("X-Session-ID", "session-12345"))

      def result: WSResponse = client(s"individuals/$nino/protections/$id").put(validAmendIPBody(ninoWithoutSuffix, "IP2016")).futureValue


      "return a success response" in {
        testMocks()
        result.status shouldBe OK
      }

      "include the response in the body" in {
        testMocks()
        result.body shouldBe validResultIPBody(nino, "IP2016")
      }

      "submit the correct json to nps" in {
        testMocks()
        result
        verify(putRequestedFor(urlEqualTo(s"/pensions-lifetime-allowance/individual/$ninoWithoutSuffix/protections/$id"))
          .withRequestBody(equalToJson(validAmendSubmissionIPBody(ninoWithoutSuffix, 3), false, true))
        )
      }

      "submit the correct auditing data" in {
        testMocks()
        result
        verify(postRequestedFor(urlEqualTo("/write/audit"))
          .withRequestBody(equalToJson(sucessfulAuditResult(nino, ninoWithoutSuffix, id, 3), false, true))
        )
      }
    }

    "submitting an amend IP2016 application which results in a Service Unavailable response" in {
      val ninoWithoutSuffix = "AA100001"
      val nino = s"${ninoWithoutSuffix}A"
      val id = 1

      def testMocks(): Unit = {
        mockAmend(ninoWithoutSuffix, SERVICE_UNAVAILABLE, "error message", id)
        mockAuth(OK)
        mockAudit(NO_CONTENT)
        mockCitizenDetails(nino, OK)

      }

      def client(path: String): WSRequest = ws.url(s"http://localhost:$port/protect-your-lifetime-allowance/$path")
        .withFollowRedirects(false)
        .withHeaders(("X-Session-ID", "session-12345"))

      def result: WSResponse = client(s"individuals/$nino/protections/$id").put(validAmendIPBody(ninoWithoutSuffix, "IP2016")).futureValue

      "return a service_unavailable response" in {
        testMocks()
        result.status shouldBe SERVICE_UNAVAILABLE
      }

      "includes a body with the error message" in {
        testMocks()
        result.body shouldBe invalidResultBodyForAmend(ninoWithoutSuffix, SERVICE_UNAVAILABLE, "error message", 1)
      }

      "submit the correct json to nps" in {
        testMocks()
        result
        verify(putRequestedFor(urlEqualTo(s"/pensions-lifetime-allowance/individual/$ninoWithoutSuffix/protections/$id"))
          .withRequestBody(equalToJson(validAmendSubmissionIPBody(ninoWithoutSuffix, 3), false, true))
        )
      }

      "submit the correct auditing data" in {
        testMocks()
        result
        verify(postRequestedFor(urlEqualTo("/write/audit/merged"))
          .withRequestBody(equalToJson(failureAuditResponse(nino, ninoWithoutSuffix, 3, SERVICE_UNAVAILABLE, "error message", 1), false, true))
        )
      }
    }

    "submitting an amend IP2016 application which results in a generic 500 response" in {


      val ninoWithoutSuffix = "AA100001"
      val nino = s"${ninoWithoutSuffix}A"
      val id = 1

      def testMocks(): Unit = {
        mockAmend(ninoWithoutSuffix, BAD_GATEWAY, "error message", id)
        mockAuth(OK)
        mockAudit(NO_CONTENT)
        mockCitizenDetails(nino, OK)

      }

      def client(path: String): WSRequest = ws.url(s"http://localhost:$port/protect-your-lifetime-allowance/$path")
        .withFollowRedirects(false)
        .withHeaders(("X-Session-ID", "session-12345"))

      def result: WSResponse = client(s"individuals/$nino/protections/$id").put(validAmendIPBody(ninoWithoutSuffix, "IP2016")).futureValue

      "return a service_unavailable response" in {
        testMocks()
        result.status shouldBe INTERNAL_SERVER_ERROR
      }

      "includes a body with the error message" in {
        testMocks()
        result.body shouldBe invalidResultBodyForAmend(ninoWithoutSuffix, BAD_GATEWAY, "error message", 1)
      }

      "submit the correct json to nps" in {
        testMocks()
        result
        verify(putRequestedFor(urlEqualTo(s"/pensions-lifetime-allowance/individual/$ninoWithoutSuffix/protections/$id"))
          .withRequestBody(equalToJson(validAmendSubmissionIPBody(ninoWithoutSuffix, 3), false, true))
        )
      }

      "submit the correct auditing data" in {
        testMocks()
        result
        verify(postRequestedFor(urlEqualTo("/write/audit/merged"))
          .withRequestBody(equalToJson(failureAuditResponse(nino, ninoWithoutSuffix, 3, BAD_GATEWAY, "error message", 1), false, true))
        )
      }
    }

    "submitting an amend IP2016 application which results in a Unauthorised response" in {


      val ninoWithoutSuffix = "AA100001"
      val nino = s"${ninoWithoutSuffix}A"
      val id = 1

      def testMocks(): Unit = {
        mockAmend(ninoWithoutSuffix, UNAUTHORIZED, "error message", id)
        mockAuth(OK)
        mockAudit(NO_CONTENT)
        mockCitizenDetails(nino, OK)

      }

      def client(path: String): WSRequest = ws.url(s"http://localhost:$port/protect-your-lifetime-allowance/$path")
        .withFollowRedirects(false)
        .withHeaders(("X-Session-ID", "session-12345"))

      def result: WSResponse = client(s"individuals/$nino/protections/$id").put(validAmendIPBody(ninoWithoutSuffix, "IP2016")).futureValue

      "return a service_unavailable response" in {
        testMocks()
        result.status shouldBe UNAUTHORIZED
      }

      "includes a body with the error message" in {
        testMocks()
        result.body shouldBe invalidResultBodyForAmend(ninoWithoutSuffix, UNAUTHORIZED, "error message", 1)
      }

      "submit the correct json to nps" in {
        testMocks()
        result
        verify(putRequestedFor(urlEqualTo(s"/pensions-lifetime-allowance/individual/$ninoWithoutSuffix/protections/$id"))
          .withRequestBody(equalToJson(validAmendSubmissionIPBody(ninoWithoutSuffix, 3), false, true))
        )
      }

      "submit the correct auditing data" in {
        testMocks()
        result
        verify(postRequestedFor(urlEqualTo("/write/audit/merged"))
          .withRequestBody(equalToJson(failureAuditResponse(nino, ninoWithoutSuffix, 3, UNAUTHORIZED, "error message", 1), false, true))
        )
      }

    }

    "submitting an amend IP2016 application which results in a generic 400 response" in {


      val ninoWithoutSuffix = "AA100001"
      val nino = s"${ninoWithoutSuffix}A"
      val id = 1

      def testMocks(): Unit = {
        mockAmend(ninoWithoutSuffix, FORBIDDEN, "error message", id)
        mockAuth(OK)
        mockAudit(NO_CONTENT)
        mockCitizenDetails(nino, OK)

      }

      def client(path: String): WSRequest = ws.url(s"http://localhost:$port/protect-your-lifetime-allowance/$path")
        .withFollowRedirects(false)
        .withHeaders(("X-Session-ID", "session-12345"))

      def result: WSResponse = client(s"individuals/$nino/protections/$id").put(validAmendIPBody(ninoWithoutSuffix, "IP2016")).futureValue

      "return a service_unavailable response" in {
        testMocks()
        result.status shouldBe INTERNAL_SERVER_ERROR
      }

      "includes a body with the error message" in {
        testMocks()
        result.body shouldBe invalidResultBodyForAmend(ninoWithoutSuffix, FORBIDDEN, "error message", 1)
      }

      "submit the correct json to nps" in {
        testMocks()
        result
        verify(putRequestedFor(urlEqualTo(s"/pensions-lifetime-allowance/individual/$ninoWithoutSuffix/protections/$id"))
          .withRequestBody(equalToJson(validAmendSubmissionIPBody(ninoWithoutSuffix, 3), false, true))
        )
      }

      "submit the correct auditing data" in {
        testMocks()
        result
        verify(postRequestedFor(urlEqualTo("/write/audit/merged"))
          .withRequestBody(equalToJson(failureAuditResponse(nino, ninoWithoutSuffix, 3, FORBIDDEN, "error message", 1), false, true))
        )
      }

    }

    "submitting an amend IP2016 application which results in a BadRequest response" in {


      val ninoWithoutSuffix = "AA100001"
      val nino = s"${ninoWithoutSuffix}A"
      val id = 1

      def testMocks(): Unit = {
        mockAmend(ninoWithoutSuffix, BAD_REQUEST, "error message", id)
        mockAuth(OK)
        mockAudit(NO_CONTENT)
        mockCitizenDetails(nino, OK)

      }

      def client(path: String): WSRequest = ws.url(s"http://localhost:$port/protect-your-lifetime-allowance/$path")
        .withFollowRedirects(false)
        .withHeaders(("X-Session-ID", "session-12345"))

      def result: WSResponse = client(s"individuals/$nino/protections/$id").put(validAmendIPBody(ninoWithoutSuffix, "IP2016")).futureValue

      "return a service_unavailable response" in {
        testMocks()
        result.status shouldBe BAD_REQUEST
      }

      "includes a body with the error message" in {
        testMocks()
        result.body shouldBe badRequestResultBody(ninoWithoutSuffix, "error message", 1)
      }

      "submit the correct json to nps" in {
        testMocks()
        result
        verify(putRequestedFor(urlEqualTo(s"/pensions-lifetime-allowance/individual/$ninoWithoutSuffix/protections/$id"))
          .withRequestBody(equalToJson(validAmendSubmissionIPBody(ninoWithoutSuffix, 3), false, true))
        )
      }

      "submit the correct auditing data" in {
        testMocks()
        result
        verify(postRequestedFor(urlEqualTo("/write/audit/merged"))
          .withRequestBody(equalToJson(failureAuditResponse(nino, ninoWithoutSuffix, 3, BAD_REQUEST, "error message", 1), false, true))
        )
      }

    }

    "amending an IP2014 application" in {
      val ninoWithoutSuffix = "AA100001"
      val nino = s"${ninoWithoutSuffix}A"
      val id = 1

      def testMocks(): Unit = {
        mockAmend(ninoWithoutSuffix, OK, validResponseIPBody(ninoWithoutSuffix, 2), id)
        mockAuth(OK)
        mockAudit(NO_CONTENT)
        mockCitizenDetails(nino, OK)
      }

      def client(path: String): WSRequest = ws.url(s"http://localhost:$port/protect-your-lifetime-allowance/$path")
        .withFollowRedirects(false)
        .withHeaders(("X-Session-ID", "session-12345"))

      def result: WSResponse = client(s"individuals/$nino/protections/$id").put(validAmendIPBody(ninoWithoutSuffix, "IP2014")).futureValue


      "return a success response" in {
        testMocks()
        result.status shouldBe OK
      }

      "include the response in the body" in {
        testMocks()
        result.body shouldBe validResultIPBody(nino, "IP2014")
      }

      "submit the correct json to nps" in {
        testMocks()
        result
        verify(putRequestedFor(urlEqualTo(s"/pensions-lifetime-allowance/individual/$ninoWithoutSuffix/protections/$id"))
          .withRequestBody(equalToJson(validAmendSubmissionIPBody(ninoWithoutSuffix, 2), false, true))
        )
      }

      "submit the correct auditing data" in {
        testMocks()
        result
        verify(postRequestedFor(urlEqualTo("/write/audit"))
          .withRequestBody(equalToJson(sucessfulAuditResult(nino, ninoWithoutSuffix, id, 2), false, true))
        )
      }
    }

    "submitting an amend IP2014 application which results in a Service Unavailable response" in {
      val ninoWithoutSuffix = "AA100001"
      val nino = s"${ninoWithoutSuffix}A"
      val id = 1

      def testMocks(): Unit = {
        mockAmend(ninoWithoutSuffix, SERVICE_UNAVAILABLE, "error message", id)
        mockAuth(OK)
        mockAudit(NO_CONTENT)
        mockCitizenDetails(nino, OK)

      }

      def client(path: String): WSRequest = ws.url(s"http://localhost:$port/protect-your-lifetime-allowance/$path")
        .withFollowRedirects(false)
        .withHeaders(("X-Session-ID", "session-12345"))

      def result: WSResponse = client(s"individuals/$nino/protections/$id").put(validAmendIPBody(ninoWithoutSuffix, "IP2014")).futureValue

      "return a service_unavailable response" in {
        testMocks()
        result.status shouldBe SERVICE_UNAVAILABLE
      }

      "includes a body with the error message" in {
        testMocks()
        result.body shouldBe invalidResultBodyForAmend(ninoWithoutSuffix, SERVICE_UNAVAILABLE, "error message", 1)
      }

      "submit the correct json to nps" in {
        testMocks()
        result
        verify(putRequestedFor(urlEqualTo(s"/pensions-lifetime-allowance/individual/$ninoWithoutSuffix/protections/$id"))
          .withRequestBody(equalToJson(validAmendSubmissionIPBody(ninoWithoutSuffix, 2), false, true))
        )
      }

      "submit the correct auditing data" in {
        testMocks()
        result
        verify(postRequestedFor(urlEqualTo("/write/audit/merged"))
          .withRequestBody(equalToJson(failureAuditResponse(nino, ninoWithoutSuffix, 2, SERVICE_UNAVAILABLE, "error message", 1), false, true))
        )
      }
    }

    "submitting an amend IP2014 application which results in a generic 500 response" in {


      val ninoWithoutSuffix = "AA100001"
      val nino = s"${ninoWithoutSuffix}A"
      val id = 1

      def testMocks(): Unit = {
        mockAmend(ninoWithoutSuffix, BAD_GATEWAY, "error message", id)
        mockAuth(OK)
        mockAudit(NO_CONTENT)
        mockCitizenDetails(nino, OK)

      }

      def client(path: String): WSRequest = ws.url(s"http://localhost:$port/protect-your-lifetime-allowance/$path")
        .withFollowRedirects(false)
        .withHeaders(("X-Session-ID", "session-12345"))

      def result: WSResponse = client(s"individuals/$nino/protections/$id").put(validAmendIPBody(ninoWithoutSuffix, "IP2014")).futureValue

      "return a service_unavailable response" in {
        testMocks()
        result.status shouldBe INTERNAL_SERVER_ERROR
      }

      "includes a body with the error message" in {
        testMocks()
        result.body shouldBe invalidResultBodyForAmend(ninoWithoutSuffix, BAD_GATEWAY, "error message", 1)
      }

      "submit the correct json to nps" in {
        testMocks()
        result
        verify(putRequestedFor(urlEqualTo(s"/pensions-lifetime-allowance/individual/$ninoWithoutSuffix/protections/$id"))
          .withRequestBody(equalToJson(validAmendSubmissionIPBody(ninoWithoutSuffix, 2), false, true))
        )
      }

      "submit the correct auditing data" in {
        testMocks()
        result
        verify(postRequestedFor(urlEqualTo("/write/audit/merged"))
          .withRequestBody(equalToJson(failureAuditResponse(nino, ninoWithoutSuffix, 2, BAD_GATEWAY, "error message", 1), false, true))
        )
      }
    }

    "submitting an amend IP2014 application which results in a Unauthorised response" in {


      val ninoWithoutSuffix = "AA100001"
      val nino = s"${ninoWithoutSuffix}A"
      val id = 1

      def testMocks(): Unit = {
        mockAmend(ninoWithoutSuffix, UNAUTHORIZED, "error message", id)
        mockAuth(OK)
        mockAudit(NO_CONTENT)
        mockCitizenDetails(nino, OK)

      }

      def client(path: String): WSRequest = ws.url(s"http://localhost:$port/protect-your-lifetime-allowance/$path")
        .withFollowRedirects(false)
        .withHeaders(("X-Session-ID", "session-12345"))

      def result: WSResponse = client(s"individuals/$nino/protections/$id").put(validAmendIPBody(ninoWithoutSuffix, "IP2014")).futureValue

      "return a service_unavailable response" in {
        testMocks()
        result.status shouldBe UNAUTHORIZED
      }

      "includes a body with the error message" in {
        testMocks()
        result.body shouldBe invalidResultBodyForAmend(ninoWithoutSuffix, UNAUTHORIZED, "error message", 1)
      }

      "submit the correct json to nps" in {
        testMocks()
        result
        verify(putRequestedFor(urlEqualTo(s"/pensions-lifetime-allowance/individual/$ninoWithoutSuffix/protections/$id"))
          .withRequestBody(equalToJson(validAmendSubmissionIPBody(ninoWithoutSuffix, 2), false, true))
        )
      }

      "submit the correct auditing data" in {
        testMocks()
        result
        verify(postRequestedFor(urlEqualTo("/write/audit/merged"))
          .withRequestBody(equalToJson(failureAuditResponse(nino, ninoWithoutSuffix, 2, UNAUTHORIZED, "error message", 1), false, true))
        )
      }

    }

    "submitting an amend IP2014 application which results in a generic 400 response" in {


      val ninoWithoutSuffix = "AA100001"
      val nino = s"${ninoWithoutSuffix}A"
      val id = 1

      def testMocks(): Unit = {
        mockAmend(ninoWithoutSuffix, FORBIDDEN, "error message", id)
        mockAuth(OK)
        mockAudit(NO_CONTENT)
        mockCitizenDetails(nino, OK)

      }

      def client(path: String): WSRequest = ws.url(s"http://localhost:$port/protect-your-lifetime-allowance/$path")
        .withFollowRedirects(false)
        .withHeaders(("X-Session-ID", "session-12345"))

      def result: WSResponse = client(s"individuals/$nino/protections/$id").put(validAmendIPBody(ninoWithoutSuffix, "IP2014")).futureValue

      "return a service_unavailable response" in {
        testMocks()
        result.status shouldBe INTERNAL_SERVER_ERROR
      }

      "includes a body with the error message" in {
        testMocks()
        result.body shouldBe invalidResultBodyForAmend(ninoWithoutSuffix, FORBIDDEN, "error message", 1)
      }

      "submit the correct json to nps" in {
        testMocks()
        result
        verify(putRequestedFor(urlEqualTo(s"/pensions-lifetime-allowance/individual/$ninoWithoutSuffix/protections/$id"))
          .withRequestBody(equalToJson(validAmendSubmissionIPBody(ninoWithoutSuffix, 2), false, true))
        )
      }

      "submit the correct auditing data" in {
        testMocks()
        result
        verify(postRequestedFor(urlEqualTo("/write/audit/merged"))
          .withRequestBody(equalToJson(failureAuditResponse(nino, ninoWithoutSuffix, 2, FORBIDDEN, "error message", 1), false, true))
        )
      }

    }

    "submitting an amend IP2014 application which results in a BadRequest response" in {


      val ninoWithoutSuffix = "AA100001"
      val nino = s"${ninoWithoutSuffix}A"
      val id = 1

      def testMocks(): Unit = {
        mockAmend(ninoWithoutSuffix, BAD_REQUEST, "error message", id)
        mockAuth(OK)
        mockAudit(NO_CONTENT)
        mockCitizenDetails(nino, OK)

      }

      def client(path: String): WSRequest = ws.url(s"http://localhost:$port/protect-your-lifetime-allowance/$path")
        .withFollowRedirects(false)
        .withHeaders(("X-Session-ID", "session-12345"))

      def result: WSResponse = client(s"individuals/$nino/protections/$id").put(validAmendIPBody(ninoWithoutSuffix, "IP2014")).futureValue

      "return a service_unavailable response" in {
        testMocks()
        result.status shouldBe BAD_REQUEST
      }

      "includes a body with the error message" in {
        testMocks()
        result.body shouldBe badRequestResultBody(ninoWithoutSuffix, "error message", 1)
      }

      "submit the correct json to nps" in {
        testMocks()
        result
        verify(putRequestedFor(urlEqualTo(s"/pensions-lifetime-allowance/individual/$ninoWithoutSuffix/protections/$id"))
          .withRequestBody(equalToJson(validAmendSubmissionIPBody(ninoWithoutSuffix, 2), false, true))
        )
      }

      "submit the correct auditing data" in {
        testMocks()
        result
        verify(postRequestedFor(urlEqualTo("/write/audit/merged"))
          .withRequestBody(equalToJson(failureAuditResponse(nino, ninoWithoutSuffix, 2, BAD_REQUEST, "error message", 1), false, true))
        )
      }

    }
  }


}
