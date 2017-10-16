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

import config.WSHttp
import connectors.{CitizenDetailsConnector, CitizenRecordOK, CitizenRecordOther4xxResponse}
import connectors.NpsConnector._
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.play.http.{HttpResponse, Upstream4xxResponse}
import play.api.http.Status._
import uk.gov.hmrc.play.http.HttpErrorFunctions

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
  }
}
