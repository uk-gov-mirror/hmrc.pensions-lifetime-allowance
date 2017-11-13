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

import java.util.Random

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import play.api.mvc.{ActionBuilder, Request, Result}
import util.NinoHelper
import play.api.http.Status
import play.api.libs.json._
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.test.{FakeHeaders, FakeRequest}
import play.api.test.Helpers._
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import model.ProtectionApplication
import uk.gov.hmrc.domain.Generator
import connectors.NpsConnector
import services.ProtectionService
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.{ BadRequestException, HeaderCarrier }

class CreateProtectionsControllerSpec extends UnitSpec with MockitoSugar with BeforeAndAfter {

  private implicit val system: ActorSystem = ActorSystem("test-sys")
  private implicit val mat: ActorMaterializer = ActorMaterializer()

  val rand = new Random()
  val ninoGenerator = new Generator(rand)

  def randomNino: String = ninoGenerator.nextNino.nino.replaceFirst("MA", "AA")

  val testNino = randomNino
  val (testNinoWithoutSuffix, _) = NinoHelper.dropNinoSuffix(testNino)

  implicit val hc = HeaderCarrier()
  val mockNpsConnector = mock[NpsConnector]

  val validApplicationBody = Json.parse(
    """
      |{
      |  "protectionType" : "FP2016"
      |}
    """.stripMargin
  )

  val invalidApplicationBody = Json.parse(
    """
      |{
      |  "type" : 100000
      |}
    """.stripMargin)

  val successfulCreateFP2016NPSResponseBody = Json.parse(
    s"""
       |  {
       |      "nino": "${testNinoWithoutSuffix}",
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

  val unsuccessfulCreateFP2016NPSResponseBody = Json.parse(
    s"""
       |  {
       |      "nino": "${testNinoWithoutSuffix}",
       |      "protection": {
       |        "id": 1,
       |        "version": 1,
       |        "type": 2,
       |        "status": 5,
       |        "notificationID": 10
       |      }
       |  }
    """.stripMargin).as[JsObject]

  val standardHeaders = Seq("Content-type" -> Seq("application/json"))
  val validExtraHOutboundHeaders = Seq("Environment" -> Seq("local"), "Authorisation" -> Seq("Bearer abcdef12345678901234567890"))

  object testProtectionService extends services.ProtectionService {
    override val nps = mockNpsConnector
  }

  case class AlwaysExecuteAction(nino: String) extends ActionBuilder[Request] {

    def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = {
      block(request)
    }
  }

  object testCreateController extends CreateProtectionsController {
    override val protectionService = testProtectionService

    override def WithCitizenRecordCheck(nino: String) = AlwaysExecuteAction(nino)
  }

  "CreateProtectionController" should {
    "respond to a valid Create Protection request with OK" in {
      when(mockNpsConnector.applyForProtection(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(model.HttpResponseDetails(200, JsSuccess(successfulCreateFP2016NPSResponseBody))))

      val fakeRequest: FakeRequest[JsValue] = FakeRequest(
        method = "POST",
        uri = "",
        headers = FakeHeaders(Seq("content-type" -> "application.json")),
        body = validApplicationBody)

      val result: Future[Result] = testCreateController.applyForProtection(testNino).apply(fakeRequest)
      status(result) shouldBe OK
    }
  }

  "CreateProtectionController" should {
    "handle a 400 (BAD_REQUEST) response from NPS service by passing it back to the caller" in {
      when(mockNpsConnector.applyForProtection(Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.failed(new BadRequestException("bad request")))

      val fakeRequest = FakeRequest(
        method = "POST",
        uri = "",
        headers = FakeHeaders(Seq("content-type" -> "application.json")),
        body = validApplicationBody)

      val result = testCreateController.applyForProtection(testNino).apply(fakeRequest)
      status(result) shouldBe BAD_REQUEST
    }

    "CreateProtectionController" should {
      "handle an invalid Json submission" in {
        val fakeRequest = FakeRequest(
          method = "POST",
          uri = "",
          headers = FakeHeaders(Seq("content-type" -> "application.json")),
          body = invalidApplicationBody)

        val result = testCreateController.applyForProtection(testNino).apply(fakeRequest)
        status(result) shouldBe BAD_REQUEST
        await(jsonBodyOf(result)) shouldBe Json.obj("message" -> JsString(
          "body failed validation with errors: List((/protectionType,List(ValidationError(List(error.path.missing),WrappedArray()))))"
        ))
      }
    }
  }
}
