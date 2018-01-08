/*
 * Copyright 2018 HM Revenue & Customs
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

package auth

import java.util.Random

import akka.actor.ActorSystem
import akka.stream.Materializer
import connectors._
import controllers.ProtectionsActions
import org.mockito.Matchers
import org.mockito.Mockito._
import _root_.mock.AuthMock
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneServerPerSuite
import play.api.http.Status._
import play.api.mvc.Results._
import play.api.mvc._
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.PlayAuthConnector
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.{HeaderCarrier, Upstream4xxResponse, Upstream5xxResponse}

class ProtectionsActionsSpec extends UnitSpec with MockitoSugar with OneServerPerSuite with AuthMock {
  val ninoGenerator = new Generator(new Random())
  val testNino = ninoGenerator.nextNino.nino.replaceFirst("MA", "AA")
  lazy val fakeRequest = FakeRequest()

  implicit val hc = HeaderCarrier()
  implicit lazy val materializer: Materializer = app.materializer
  val mockPlayAuthConnector = mock[PlayAuthConnector]
  implicit val system = ActorSystem()
  val mockCitizenDetailsConnector = mock[CitizenDetailsConnector]

  object testProtectionsActions extends ProtectionsActions with AuthorisedActions with AuthMock  {

    override lazy val authConnector: AuthClientConnectorTrait = mockAuthConnector
    override lazy val citizenDetailsConnector = mockCitizenDetailsConnector
  }

  def testCitizenRecordCheck(nino: String): testProtectionsActions.WithCitizenRecordCheckAction = testProtectionsActions.WithCitizenRecordCheckAction(nino)
  def testAuth(nino: String): testProtectionsActions.Authorised = testProtectionsActions.Authorised(nino)

  "test Citizen Record search found" in {
    when(mockCitizenDetailsConnector.checkCitizenRecord(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(CitizenRecordOK))

    val result = testCitizenRecordCheck(testNino).invokeBlock(FakeRequest(), (r: Request[Any]) => Future.successful(Ok))
    val resultStatus = await(result)
    resultStatus shouldBe Ok
  }

  "test Citizen Record Not Found" in {
    when(mockCitizenDetailsConnector.checkCitizenRecord(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(CitizenRecordNotFound))

    val result: Future[Result] = testCitizenRecordCheck(testNino).invokeBlock(FakeRequest(), (r: Request[Any]) => Future.successful(NotModified))
    val resultStatus: Result = await(result)
    val expectedResult = NotFound(s"Citizen Record Check: Not Found for '$testNino'")

    bodyOf(resultStatus) shouldBe bodyOf(expectedResult)
    resultStatus.header.status shouldBe expectedResult.header.status

  }

  "test Citizen Record Locked" in {
    when(mockCitizenDetailsConnector.checkCitizenRecord(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(CitizenRecordLocked))

    val result = testCitizenRecordCheck(testNino).invokeBlock(FakeRequest(), (r: Request[Any]) => Future.successful(NotModified))
    val resultStatus = await(result)
    val expectedResult = Locked(s"Citizen Record Check: Locked for '$testNino'")

    bodyOf(resultStatus) shouldBe bodyOf(expectedResult)
    resultStatus.header.status shouldBe expectedResult.header.status
  }

  "test Citizen Record search resulted in a 400 response" in {
    val errorString = "Mock 400 Error"
    when(mockCitizenDetailsConnector.checkCitizenRecord(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(CitizenRecordOther4xxResponse(new Upstream4xxResponse(errorString, 400, 400))))

    val result = testCitizenRecordCheck(testNino).invokeBlock(FakeRequest(), (r: Request[Any]) => Future.successful(NotModified))
    val resultStatus = await(result)
    val expectedResult = BadRequest(s"Citizen Record Check: 400 response for '$testNino'\nResponse: $errorString")

    bodyOf(resultStatus) shouldBe bodyOf(expectedResult)
    resultStatus.header.status shouldBe expectedResult.header.status
  }

  "test Citizen Record search resulted in a 500 response" in {
    val errorString = "Mock 500 Error"
    when(mockCitizenDetailsConnector.checkCitizenRecord(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(CitizenRecord5xxResponse(new Upstream5xxResponse(errorString, 500, 500))))

    val result = testCitizenRecordCheck(testNino).invokeBlock(FakeRequest(), (r: Request[Any]) => Future.successful(NotModified))
    val resultStatus = await(result)
    val expectedResult = InternalServerError(s"Citizen Record Check: Upstream 500 response for '$testNino'\nResponse: $errorString")

    bodyOf(resultStatus) shouldBe bodyOf(expectedResult)
    resultStatus.header.status shouldBe expectedResult.header.status
  }

  "test Citizen Record search resulted in a 503 response" in {
    val errorString = "Mock 503 Error"
    when(mockCitizenDetailsConnector.checkCitizenRecord(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(CitizenRecord5xxResponse(new Upstream5xxResponse(errorString, 503, 503))))

    val result = testCitizenRecordCheck(testNino).invokeBlock(FakeRequest(), (r: Request[Any]) => Future.successful(NotModified))
    val resultStatus = await(result)
    val expectedResult = GatewayTimeout(s"Citizen Record Check: Upstream 503 response for '$testNino'\nResponse: $errorString")

    bodyOf(resultStatus) shouldBe bodyOf(expectedResult)
    resultStatus.header.status shouldBe expectedResult.header.status
  }

  "test Citizen Record search resulted in a 5xx response" in {
    val errorString = "Mock 501 Error"
    when(mockCitizenDetailsConnector.checkCitizenRecord(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(CitizenRecord5xxResponse(new Upstream5xxResponse(errorString, 501, 501))))

    val result = testCitizenRecordCheck(testNino).invokeBlock(FakeRequest(), (r: Request[Any]) => Future.successful(NotModified))
    val resultStatus = await(result)
    val expectedResult = InternalServerError(s"Citizen Record Check: Upstream 501 response for '$testNino'\nResponse: $errorString")

    bodyOf(resultStatus) shouldBe bodyOf(expectedResult)
    resultStatus.header.status shouldBe expectedResult.header.status
  }

    "get Unauthorized response for Auth" in {
      when(mockCitizenDetailsConnector.checkCitizenRecord(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(CitizenRecordOK))

      val result = testCitizenRecordCheck(testNino).invokeBlock(FakeRequest(), (r: Request[Any]) => Future.successful(Unauthorized))
      status(result) shouldBe UNAUTHORIZED
    }

  "get Forbidden response for Auth" in {
    when(mockCitizenDetailsConnector.checkCitizenRecord(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(CitizenRecordOK))

    val result = testCitizenRecordCheck(testNino).invokeBlock(FakeRequest(), (r: Request[Any]) => Future.successful(Forbidden))
    status(result) shouldBe FORBIDDEN
  }

  "get Internal Server Error response for Auth" in {
    when(mockCitizenDetailsConnector.checkCitizenRecord(Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(CitizenRecordOK))

    val result = testCitizenRecordCheck(testNino).invokeBlock(FakeRequest(), (r: Request[Any]) => Future.successful(InternalServerError))
    status(result) shouldBe INTERNAL_SERVER_ERROR
  }

}
