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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, urlPathMatching}
import com.kenshoo.play.metrics.PlayModule
import controllers.Assets.INTERNAL_SERVER_ERROR
import org.scalatest.BeforeAndAfter
import org.scalatest.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status.{BAD_REQUEST, LOCKED, NOT_FOUND, OK}
import play.api.inject.guice.GuiceableModule
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import util.{TestUtils, WireMockHelper}

import scala.concurrent.ExecutionContext.Implicits.global


class CitizenDetailsConnectorSpec extends PlaySpec with MockitoSugar with BeforeAndAfter with TestUtils
  with GuiceOneAppPerSuite with WireMockHelper {

  private val DefaultTestNino = "KA191435A"
  private val DesignatoryDetailsUrl = s"/citizen-details/$DefaultTestNino/designatory-details"
  private val DefaultLocalUrl = "http://localhost:8083"

  implicit val hc: HeaderCarrier = HeaderCarrier()

  def bindModules: Seq[GuiceableModule] = Seq(new PlayModule)

  object testCitizenDetailsConnector extends CitizenDetailsConnector {
    override val serviceUrl = DefaultLocalUrl
    override def http: DefaultHttpClient = app.injector.instanceOf[DefaultHttpClient]
    override val checkRequired = true
  }

  object NoCheckRequiredCitizenDetailsConnector extends CitizenDetailsConnector {
    override val serviceUrl = DefaultLocalUrl
    override def http: DefaultHttpClient = app.injector.instanceOf[DefaultHttpClient]
    override val checkRequired = false
  }

  "The CitizenDetails Connector getCitizenRecordCheckUrl method" when {
    "return a  URL that contains the nino passed to it" in {
      testCitizenDetailsConnector.getCitizenRecordCheckUrl(DefaultTestNino).contains(DefaultTestNino) shouldBe true
    }
  }

  "The CitizenDetails Connector checkCitizenRecord method" when {
    "return a CitizenRecordOK response when no check is needed" in {
      val f = NoCheckRequiredCitizenDetailsConnector.checkCitizenRecord(DefaultTestNino)

      val res = await(f)
      res shouldBe CitizenRecordOK
    }
  }

  "The CitizenDetails Connector checkCitizenRecord method" when {

    "return a valid HTTPResponse for successful retrieval" in {
      server.stubFor(
        get(urlPathMatching(DesignatoryDetailsUrl))
          .willReturn(
            aResponse()
              .withStatus(OK)
          )
      )

      val f = testCitizenDetailsConnector.checkCitizenRecord(DefaultTestNino)

      val res = await(f)
      res shouldBe CitizenRecordOK
    }

    "return an error if NotFoundException received" in {

      server.stubFor(
        get(urlPathMatching(DesignatoryDetailsUrl))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
          )
      )

      val f = testCitizenDetailsConnector.checkCitizenRecord(DefaultTestNino)

      val res = await(f)
      res shouldBe CitizenRecordNotFound
    }

    "return an error if Upstream4xxResponse received" in {

      server.stubFor(
        get(urlPathMatching(DesignatoryDetailsUrl))
          .willReturn(
            aResponse()
              .withStatus(BAD_REQUEST)
          )
      )

      val f = testCitizenDetailsConnector.checkCitizenRecord(DefaultTestNino)

      val res = await(f)
      res.isInstanceOf[CitizenRecordOther4xxResponse] shouldBe true
    }


    "return an error if Upstream5xxResponse received" in {

      server.stubFor(
        get(urlPathMatching(DesignatoryDetailsUrl))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      val f = testCitizenDetailsConnector.checkCitizenRecord(DefaultTestNino)

      val res = await(f)
      res.isInstanceOf[CitizenRecord5xxResponse] shouldBe true
    }


    "return an error if CitizenRecordLocked received" in {

      server.stubFor(
        get(urlPathMatching(DesignatoryDetailsUrl))
          .willReturn(
            aResponse()
              .withStatus(LOCKED)
          )
      )
      val f = testCitizenDetailsConnector.checkCitizenRecord(DefaultTestNino)

      val res = await(f)
      res shouldBe CitizenRecordLocked
    }
  }
}
