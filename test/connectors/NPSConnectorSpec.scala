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

package connectors

import org.scalatest.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.mockito.MockitoSugar

import java.util.Random
import util._
import play.api.libs.json._
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import org.scalatestplus.play.PlaySpec

import scala.concurrent.ExecutionContext.Implicits.global

class NPSConnectorSpec extends PlaySpec with MockitoSugar {

  private val mockHttp = mock[DefaultHttpClient]

  object testNPSConnector extends NpsConnector {
    override val serviceUrl = "http://localhost:80"
    override val http: DefaultHttpClient = mockHttp
    override val serviceAccessToken = "token"
    override val serviceEnvironment = "environment"

    override val audit : AuditConnector = mock[AuditConnector]
  }

  val rand = new Random()
  val ninoGenerator = new Generator(rand)
  def randomNino: String = ninoGenerator.nextNino.nino.replaceFirst("MA", "AA")

  val testNino: String = randomNino
  val (testNinoWithoutSuffix,_) = NinoHelper.dropNinoSuffix(testNino)

  implicit val hc: HeaderCarrier = HeaderCarrier()

  "The NPS connector implicit header carrier  " when {
    "should have the environment and authorisation headers set" in {
      testNPSConnector.addExtraHeaders.extraHeaders.exists(_._1 == "Environment") shouldBe true
      testNPSConnector.addExtraHeaders.authorization.isDefined shouldBe true
    }
  }

  "The  NPS Connector response handler" when {
    "handle 409 responses as successes and pass the status back unmodifed" in {
      val handledHttpResponse =  NpsResponseHandler.handleNpsResponse("POST", "", HttpResponse(409, ""))
      handledHttpResponse.status shouldBe 409
    }
  }

  "The NPS Connector response handler" when {
    "handle non-OK responses other than 409 as failures and throw an exception" in {
      try {
        NpsResponseHandler.handleNpsResponse("POST", "", HttpResponse(400, ""))
        fail("Exception not thrown")
      } catch {
        case ex: Throwable =>
      }
    }
  }

  "The NPS Connector getApplyUrl method" when {
    "return a  URL that contains the nino passed to it" in {
      testNPSConnector.getApplyUrl(testNinoWithoutSuffix).contains(testNinoWithoutSuffix) shouldBe true
    }
  }

  "The NPS Connector getAmendUrl method" when {
    "return a  URL that contains the nino passed to it" in {
      testNPSConnector.getAmendUrl(testNinoWithoutSuffix,1).contains(testNinoWithoutSuffix) shouldBe true
    }
  }

  "The NPS Connector getReadUrl method" when {
    "return a  URL that contains the nino passed to it" in {
      testNPSConnector.getReadUrl(testNinoWithoutSuffix).contains(testNinoWithoutSuffix) shouldBe true
    }
  }

  "The NPS Connector handleAuditableResponse" when {
    "return a HTTPResponseDetails object with valid fields" in {
      val requestStr =
        s"""
          |{
          | "nino": "$testNinoWithoutSuffix",
          | "protection": {
          |   "type": 1
          |   }
          | }
        """.stripMargin
      val responseBody = Json.parse(requestStr).as[JsObject]
      val responseDetails = testNPSConnector.handleAuditableResponse(testNino, HttpResponse(200, responseBody.toString()), None)
      responseDetails.status shouldBe 200
      responseDetails.body.isSuccess shouldBe true
    }
  }

  "The NPS Connector handleAuditableResponse" when {
    "return a HTTPResponseDetails object with a 400 status if the nino returned differs from that sent" in {
      val (t1NinoWithoutSuffix,_) = NinoHelper.dropNinoSuffix(randomNino)
      val (t2NinoWithoutSuffix,_) = NinoHelper.dropNinoSuffix(randomNino)

      val requestStr =
        s"""
           |{
           | "nino": "$t1NinoWithoutSuffix",
           | "protection": {
           |   "type": 1
           |   }
           | }
        """.stripMargin
      val responseBody = Json.parse(requestStr).as[JsObject]
      val responseDetails = testNPSConnector.handleAuditableResponse(testNino, HttpResponse(200, responseBody.toString()), None)
      responseDetails.status shouldBe 400
      responseDetails.body.isSuccess shouldBe true
    }
  }

  "The NPS Connector handleEResponse" when {
    "return a HTTPResponseDetails object with valid fields" in {
      val responseStr =
        s"""
          {
           |"nino": "$testNinoWithoutSuffix",
           | "protection": {
           |   "type": 1
           |  }
           |}
        """.stripMargin
      val responseDetails = testNPSConnector.handleExpectedReadResponse(
        testNino,
        HttpResponse(200, responseStr))
      responseDetails.status shouldBe 200
      responseDetails.body.isSuccess shouldBe true
    }
  }
  "The NPS Connector handleExpectedReadResponse" when {
    "return a HTTPResponseDetails object with a 400 status if the nino returned differs from that sent" in {
      val (t1NinoWithoutSuffix,_) = NinoHelper.dropNinoSuffix(randomNino)

      val requestStr =
        s"""
           |{
           | "nino": "$t1NinoWithoutSuffix"
           | }
        """.stripMargin

      val responseDetails = testNPSConnector.handleExpectedReadResponse(
        testNino, HttpResponse(200, requestStr))
      responseDetails.status shouldBe 400
      responseDetails.body.isSuccess shouldBe true
    }
  }
}
