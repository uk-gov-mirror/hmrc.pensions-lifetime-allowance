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
import org.mockito.Mockito._
import _root_.mock.AuthMock
import org.mockito.Matchers
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.OneServerPerSuite
import play.api.http.Status._
import play.api.mvc.Results._
import play.api.mvc._
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.{InsufficientConfidenceLevel, MissingBearerToken}
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.play.test.UnitSpec
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.{HeaderCarrier, Upstream4xxResponse, Upstream5xxResponse}

class AuthorisedActionsSpec extends UnitSpec with MockitoSugar with OneServerPerSuite with AuthMock {

  val ninoGenerator = new Generator(new Random())
  val testNino = ninoGenerator.nextNino.nino.replaceFirst("MA", "AA")
  lazy val fakeRequest = FakeRequest()

  implicit val hc = HeaderCarrier()
  implicit lazy val materializer: Materializer = app.materializer
  implicit val system = ActorSystem()


  def setupActions(authResponse: Future[Unit], citizenCheckResponse: Future[CitizenRecordCheckResult]): AuthorisedActions = {
    val testAuthConnector = mock[AuthClientConnectorTrait]
    val testCitizenConnector = mock[CitizenDetailsConnector]

    when(testAuthConnector.authorise[Unit](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(authResponse)

    when(testCitizenConnector.checkCitizenRecord(Matchers.any())(Matchers.any(), Matchers.any()))
      .thenReturn(citizenCheckResponse)

    new AuthorisedActions {

      override val citizenDetailsConnector: CitizenDetailsConnector = testCitizenConnector

      override def authConnector: AuthClientConnectorTrait = testAuthConnector
    }
  }


  "test Citizen Record search found" in {
    val action = setupActions(Future.successful({}), Future.successful(CitizenRecordOK))
    val result = action.Authorised(testNino).invokeBlock(fakeRequest, (r: Request[Any]) => Future.successful(Ok))
    val resultStatus = await(result)
    resultStatus shouldBe Ok
  }

  "test Citizen Record Not Found" in {
    val action = setupActions(Future.successful({}), Future.successful(CitizenRecordNotFound))
    val result: Future[Result] = action.Authorised(testNino).invokeBlock(fakeRequest, (r: Request[Any]) => Future.successful(NotModified))
    val resultStatus: Result = await(result)
    val expectedResult = NotFound(s"Citizen Record Check: Not Found for '$testNino'")

    bodyOf(resultStatus) shouldBe bodyOf(expectedResult)
    resultStatus.header.status shouldBe expectedResult.header.status

  }

  "test Citizen Record Locked" in {
    val action = setupActions(Future.successful({}), Future.successful(CitizenRecordLocked))
    val result: Future[Result] = action.Authorised(testNino).invokeBlock(fakeRequest, (r: Request[Any]) => Future.successful(NotModified))
    val resultStatus = await(result)
    val expectedResult = Locked(s"Citizen Record Check: Locked for '$testNino'")

    bodyOf(resultStatus) shouldBe bodyOf(expectedResult)
    resultStatus.header.status shouldBe expectedResult.header.status
  }

  "test Citizen Record search resulted in a 400 response" in {
    val errorString = "Mock 400 Error"
    val action = setupActions(Future.successful({}), Future.successful(CitizenRecordOther4xxResponse(new Upstream4xxResponse(errorString, 400, 400))))
    val result: Future[Result] = action.Authorised(testNino).invokeBlock(fakeRequest, (r: Request[Any]) => Future.successful(NotModified))
    val resultStatus = await(result)
    val expectedResult = BadRequest(s"Citizen Record Check: 400 response for '$testNino'\nResponse: $errorString")

    bodyOf(resultStatus) shouldBe bodyOf(expectedResult)
    resultStatus.header.status shouldBe expectedResult.header.status
  }

  "test Citizen Record search resulted in a 500 response" in {
    val errorString = "Mock 500 Error"
    val action = setupActions(Future.successful({}), Future.successful(Future.successful(CitizenRecord5xxResponse(new Upstream5xxResponse(errorString, 500, 500)))))
    val result: Future[Result] = action.Authorised(testNino).invokeBlock(fakeRequest, (r: Request[Any]) => Future.successful(NotModified))
    val resultStatus = await(result)
    val expectedResult = InternalServerError(s"Citizen Record Check: Upstream 500 response for '$testNino'\nResponse: $errorString")

    bodyOf(resultStatus) shouldBe bodyOf(expectedResult)
    resultStatus.header.status shouldBe expectedResult.header.status
  }

  "test Citizen Record search resulted in a 503 response" in {
    val errorString = "Mock 503 Error"
    val action = setupActions(Future.successful({}), Future.successful(Future.successful(CitizenRecord5xxResponse(new Upstream5xxResponse(errorString, 503, 503)))))
    val result: Future[Result] = action.Authorised(testNino).invokeBlock(fakeRequest, (r: Request[Any]) => Future.successful(NotModified))
    val resultStatus = await(result)
    val expectedResult = GatewayTimeout(s"Citizen Record Check: Upstream 503 response for '$testNino'\nResponse: $errorString")

    bodyOf(resultStatus) shouldBe bodyOf(expectedResult)
    resultStatus.header.status shouldBe expectedResult.header.status
  }

  "test Citizen Record search resulted in a 5xx response" in {
    val errorString = "Mock 501 Error"
    val action = setupActions(Future.successful({}), Future.successful(Future.successful(CitizenRecord5xxResponse(new Upstream5xxResponse(errorString, 501, 501)))))
    val result: Future[Result] = action.Authorised(testNino).invokeBlock(fakeRequest, (r: Request[Any]) => Future.successful(NotModified))
    val resultStatus = await(result)
    val expectedResult = InternalServerError(s"Citizen Record Check: Upstream 501 response for '$testNino'\nResponse: $errorString")

    bodyOf(resultStatus) shouldBe bodyOf(expectedResult)
    resultStatus.header.status shouldBe expectedResult.header.status
  }

    "get Unauthorized response for Auth" in {
      val action = setupActions(Future.failed(new MissingBearerToken), Future.successful(Future.successful(CitizenRecordOK)))
      val result: Future[Result] = action.Authorised(testNino).invokeBlock(fakeRequest, (r: Request[Any]) => Future.successful(Ok))
      status(result) shouldBe UNAUTHORIZED
    }

  "get Forbidden response for Auth" in {
    val action = setupActions(Future.failed(new InsufficientConfidenceLevel), Future.successful(Future.successful(CitizenRecordOK)))
    val result: Future[Result] = action.Authorised(testNino).invokeBlock(fakeRequest, (r: Request[Any]) => Future.successful(Ok))
    status(result) shouldBe FORBIDDEN
  }

  "get Internal Server Error response for Auth" in {
    val action = setupActions(Future.failed(new Exception), Future.successful(Future.successful(CitizenRecordOK)))
    val result: Future[Result] = action.Authorised(testNino).invokeBlock(fakeRequest, (r: Request[Any]) => Future.successful(Ok))
    status(result) shouldBe INTERNAL_SERVER_ERROR
  }

}
