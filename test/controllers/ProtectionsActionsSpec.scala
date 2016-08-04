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

package controllers

import java.util.Random

import connectors._
import org.scalatest.mock.MockitoSugar
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.mvc.Results._
import play.api.mvc.{Result, Request, ActionBuilder}
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.play.http.{Upstream5xxResponse, Upstream4xxResponse, HeaderCarrier}
import uk.gov.hmrc.play.test.{WithFakeApplication, UnitSpec}

import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global

class ProtectionsActionsSpec extends UnitSpec with MockitoSugar with WithFakeApplication {

  val ninoGenerator = new Generator(new Random())
  val testNino = ninoGenerator.nextNino.nino.replaceFirst("MA", "AA")

  implicit val hc = HeaderCarrier()

  val mockCitizenDetailsConnector = mock[CitizenDetailsConnector]

  object testProtectionsActions extends ProtectionsActions {
    override lazy val citizenDetailsConnector = mockCitizenDetailsConnector
  }

  def testCitizenRecordCheck(nino: String): testProtectionsActions.WithCitizenRecordCheckAction = testProtectionsActions.WithCitizenRecordCheckAction(nino)

  "test Citizen Record search found" in {
    when(mockCitizenDetailsConnector.checkCitizenRecord(Matchers.any())(Matchers.any(),Matchers.any())).thenReturn(Future.successful(CitizenRecordOK))

    val result= testCitizenRecordCheck(testNino).invokeBlock(FakeRequest(), (r: Request[Any]) => Future.successful(Ok))
    val x = await(result)
    x shouldBe Ok
  }

  "test Citizen Record Not Found" in {
    when(mockCitizenDetailsConnector.checkCitizenRecord(Matchers.any())(Matchers.any(),Matchers.any())).thenReturn(Future.successful(CitizenRecordNotFound))

    val result= testCitizenRecordCheck(testNino).invokeBlock(FakeRequest(), (r: Request[Any]) => Future.successful(NotModified))
    val x = await(result)
    x shouldBe NotFound
  }

  "test Citizen Record Locked" in {
    when(mockCitizenDetailsConnector.checkCitizenRecord(Matchers.any())(Matchers.any(),Matchers.any())).thenReturn(Future.successful(CitizenRecordLocked))

    val result= testCitizenRecordCheck(testNino).invokeBlock(FakeRequest(), (r: Request[Any]) => Future.successful(NotModified))
    val x = await(result)
    x shouldBe Locked
  }

  "test Citizen Record search resulted in a 4xx response" in {
    when(mockCitizenDetailsConnector.checkCitizenRecord(Matchers.any())(Matchers.any(),Matchers.any())).thenReturn(Future.successful(CitizenRecordOther4xxResponse(new Upstream4xxResponse("",400,400))))

    val result= testCitizenRecordCheck(testNino).invokeBlock(FakeRequest(), (r: Request[Any]) => Future.successful(NotModified))
    val x = await(result)
    x shouldBe BadRequest
  }

  "test Citizen Record search resulted in a 500 response" in {
    when(mockCitizenDetailsConnector.checkCitizenRecord(Matchers.any())(Matchers.any(),Matchers.any())).thenReturn(Future.successful(CitizenRecord5xxResponse(new Upstream5xxResponse("",500,500))))

    val result= testCitizenRecordCheck(testNino).invokeBlock(FakeRequest(), (r: Request[Any]) => Future.successful(NotModified))
    val x = await(result)
    x shouldBe InternalServerError
  }

  "test Citizen Record search resulted in a 503 response" in {
    when(mockCitizenDetailsConnector.checkCitizenRecord(Matchers.any())(Matchers.any(),Matchers.any())).thenReturn(Future.successful(CitizenRecord5xxResponse(new Upstream5xxResponse("",503,503))))

    val result= testCitizenRecordCheck(testNino).invokeBlock(FakeRequest(), (r: Request[Any]) => Future.successful(NotModified))
    val x = await(result)
    x shouldBe GatewayTimeout
  }

  "test Citizen Record search resulted in a 5xx response" in {
    when(mockCitizenDetailsConnector.checkCitizenRecord(Matchers.any())(Matchers.any(),Matchers.any())).thenReturn(Future.successful(CitizenRecord5xxResponse(new Upstream5xxResponse("",501,501))))

    val result= testCitizenRecordCheck(testNino).invokeBlock(FakeRequest(), (r: Request[Any]) => Future.successful(NotModified))
    val x = await(result)
    x shouldBe InternalServerError
  }
}
