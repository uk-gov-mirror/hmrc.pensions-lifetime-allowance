/*
 * Copyright 2019 HM Revenue & Customs
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

import java.util.Random

import com.kenshoo.play.metrics.PlayModule
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.domain.Generator
import org.scalatest.mockito.MockitoSugar
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, HttpGet, HttpResponse, NotFoundException, Upstream4xxResponse, Upstream5xxResponse }


class CitizenDetailsConnectorSpec extends UnitSpec with MockitoSugar with WithFakeApplication with BeforeAndAfter {

  override def bindModules = Seq(new PlayModule)
  val mockHttp = mock[HttpGet]

  object testCitizenDetailsConnector extends CitizenDetailsConnector {
    override val serviceUrl = "http://localhost:80"
    override def http = mockHttp
    override val checkRequired = true
  }

  object NoCheckRequiredCitizenDetailsConnector extends CitizenDetailsConnector {
    override val serviceUrl = "http://localhost:80"
    override def http = mockHttp
    override val checkRequired = false
  }

  val rand = new Random()
  val ninoGenerator = new Generator(rand)
  def randomNino: String = ninoGenerator.nextNino.nino.replaceFirst("MA", "AA")

  val testNino = randomNino

  implicit val hc: HeaderCarrier = HeaderCarrier()

  "The CitizenDetails Connector getCitizenRecordCheckUrl method" should {
    "return a  URL that contains the nino passed to it" in {
      testCitizenDetailsConnector.getCitizenRecordCheckUrl(testNino).contains(testNino) shouldBe true
    }
  }

  "The CitizenDetails Connector checkCitizenRecord method" should {
    "return a CitizenRecordOK response when no check is needed" in {
      val f = NoCheckRequiredCitizenDetailsConnector.checkCitizenRecord(testNino)

      val res = await(f)
      res shouldBe CitizenRecordOK
    }
  }

  before {
    reset(mockHttp)
  }

  "The CitizenDetails Connector checkCitizenRecord method" should {
    "return a valid HTTPResponse for successful retrieval" in {

      when(mockHttp.GET[HttpResponse](ArgumentMatchers.any())
       (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(200)))

      val f = testCitizenDetailsConnector.checkCitizenRecord(testNino)

      val res = await(f)
      res shouldBe CitizenRecordOK
    }
  }

  "The CitizenDetails Connector checkCitizenRecord method" should {
    "return an error if NotFoundException received" in {

      val response = new NotFoundException("")

      when(mockHttp.GET[HttpResponse](ArgumentMatchers.any())
       (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.failed(response))

      val f = testCitizenDetailsConnector.checkCitizenRecord(testNino)

      val res = await(f)
      res shouldBe CitizenRecordNotFound
    }
  }

  "The CitizenDetails Connector checkCitizenRecord method" should {
    "return an error if Upstream4xxResponse received" in {

      val response = new Upstream4xxResponse("",400,400)

      when(mockHttp.GET[HttpResponse](ArgumentMatchers.any())
       (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.failed(response))

      val f = testCitizenDetailsConnector.checkCitizenRecord(testNino)

      val res = await(f)
      res shouldBe CitizenRecordOther4xxResponse(response)
    }
  }

  "The CitizenDetails Connector checkCitizenRecord method" should {
    "return an error if Upstream5xxResponse received" in {

      val response = new Upstream5xxResponse("",500,500)

      when(mockHttp.GET[HttpResponse](ArgumentMatchers.any())
       (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.failed(response))

      val f = testCitizenDetailsConnector.checkCitizenRecord(testNino)

      val res = await(f)
      res shouldBe CitizenRecord5xxResponse(response)
    }
  }

  "The CitizenDetails Connector checkCitizenRecord method" should {
    "return an error if CitizenRecordLocked received" in {

      val response = new Upstream4xxResponse("",423,423)

      when(mockHttp.GET[HttpResponse](ArgumentMatchers.any())
       (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.failed(response))

      val f = testCitizenDetailsConnector.checkCitizenRecord(testNino)

      val res = await(f)
      res shouldBe CitizenRecordLocked
    }
  }
}
