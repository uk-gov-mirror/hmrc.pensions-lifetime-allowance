/*
 * Copyright 2016 HM Revenue & Customs
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

package util

import config.{MicroserviceAuditConnector, WSHttp}
import connectors.{CitizenDetailsConnector, CitizenRecordOK, CitizenRecordOther4xxResponse}
import config.WSHttp
import connectors.{CitizenDetailsConnector, CitizenRecordOK, CitizenRecordOther4xxResponse, NpsConnector}
import connectors.NpsConnector._
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.{HttpErrorFunctions, HttpResponse, Upstream4xxResponse}
import uk.gov.hmrc.play.audit.model.DataEvent
import com.github.tomakehurst.wiremock.client.WireMock._
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

class TestConnectorSpec extends IntegrationSpec with HttpErrorFunctions{

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(fakeConfig())
    .build()

  "Test connector" should {

    "return a wiremock response" in {

      stubGet("/test", OK,
        """{
          |"result":"success"
          |}""".stripMargin)


      lazy val httpGet = await(WSHttp.GET[HttpResponse](baseUrl("nps") + "/test"))

      httpGet.status shouldBe 200
      httpGet.body shouldBe """{
                              |"result":"success"
                              |}""".stripMargin
    }

    "mock the Citizen Details call" when {

      "the details are mocked as valid" in {
        mockCitizenDetails("AA10000A", OK)

        await(CitizenDetailsConnector.checkCitizenRecord("AA10000A")) shouldBe CitizenRecordOK
      }

      "the details throw an invalid call with an exception" in {
        mockCitizenDetails("AA10000A", UNAUTHORIZED)

        Try(await(CitizenDetailsConnector.checkCitizenRecord("AA10000A")).asInstanceOf[CitizenRecordOther4xxResponse]) match {
          case Success(CitizenRecordOther4xxResponse(result)) =>
            result.copy(headers = Map.empty) shouldBe
              Upstream4xxResponse(
                upstreamResponseMessage("GET", url + "/citizen-details/AA10000A/designatory-details", UNAUTHORIZED, ""),
                UNAUTHORIZED,
                INTERNAL_SERVER_ERROR)
          case Failure(_) => fail("Incorrect Citizen Record response type")
        }
      }
    }

    "mock the auditing call" which {

      "should have the correct json body" in {
        mockAudit(OK)
        val result = await(MicroserviceAuditConnector.sendEvent(DataEvent(auditSource = "testCall", auditType = "type", eventId = "id", generatedAt = DateTime.parse("06-10-1990", DateTimeFormat.forPattern("dd-MM-yyyy")))))

        verify(postRequestedFor(urlEqualTo("/write/audit"))
          .withRequestBody(equalToJson(Json.parse(
            """
              |{
              | "auditSource" : "testCall",
              | "auditType" : "type",
              | "eventId" : "id",
              | "tags" : { },
              | "detail": { },
              | "generatedAt": "1990-10-05T23:00:00.000+0000"
              |}
            """.stripMargin).toString(), false, true)))
      }
    }

    "mock the NPS call" which {

      "should have the correct json body" in {
        mockNPSConnector("AA100001", OK)

        val validBody = Json.parse(
       """
        |{
        |
        |    "nino": "AA100001A",
        |    "protection": {
        |        "type": 1,
        |        "relevantAmount": 1250000,
        |        "preADayPensionInPayment": 250000,
        |        "postADayBCE": 250000,
        |        "uncrystallisedRights": 500000,
        |        "nonUKRights": 250000,
        |        "pensionDebitAmount": 0,
        |        "protectedAmount": 200,
        |        "pensionDebitEnteredAmount": 150,
        |        "pensionDebitStartDate": "2015-05-25",
        |        "pensionDebitTotalAmount": 15000
        |    },
        |    "pensionDebits": [
        |        {
        |            "pensionDebitEnteredAmount": 400.00,
        |            "pensionDebitStartDate": "2015-05-25"
        |        },
        |        {
        |            "pensionDebitEnteredAmount": 200.00,
        |            "pensionDebitStartDate": "2015-05-24"
        |        },
        |        {
        |            "pensionDebitEnteredAmount": 100.00,
        |            "pensionDebitStartDate": "2015-05-23"
        |        }
        |    ]
        |
        |}
      """.stripMargin)

        val validBodyAsObject:JsObject = validBody.as[JsObject]
        val result = await(NpsConnector.applyForProtection("AA100001A", validBodyAsObject))

        verify(postRequestedFor(urlEqualTo("/pensions-lifetime-allowance/individual/AA100001/protection")))

        result.status shouldBe OK
      }
    }
  }
}
