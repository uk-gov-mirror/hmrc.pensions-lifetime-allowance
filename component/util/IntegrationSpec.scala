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

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.http.HeaderCarrier
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite


trait IntegrationSpec extends PlaySpec
  with GuiceOneServerPerSuite with ScalaFutures with IntegrationPatience with Matchers
  with WiremockHelper with BeforeAndAfterEach with BeforeAndAfterAll {

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(fakeConfig())
    .build()

  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val mat: Materializer = ActorMaterializer()
  implicit val hc: HeaderCarrier = new HeaderCarrier()

  override def beforeEach(): Unit = {
    resetWiremock()
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    startWiremock()
  }

  override def afterAll(): Unit = {
    stopWiremock()
    super.afterAll()
  }

  def mockAuth(status: Int): Unit = {

    stubPost("/auth/authorise", status, "{}")


    stubGet("/auth/ids", status, """{"internalId":"Int-xxx","externalId":"Ext-xxx"}""")

    stubGet("/auth/userDetails", status,
      """
        |{
        |   "name":"xxx",
        |   "email":"xxx",
        |   "affinityGroup":"xxx",
        |   "authProviderId":"123456789",
        |   "authProviderType":"xxx"
        |}
      """.stripMargin
    )
  }

  def mockCitizenDetails(nino: String, status: Int): Unit = {
    val url = s"/citizen-details/$nino/designatory-details"
    stubGet(url, status, "")
  }

  def mockAudit(status: Int): Unit = {
    val url = s"/write/audit"
    stubPost(url, status, "audit-response")
    stubPost(url + "/merged", status, "audit-response")
  }

  def mockNPSConnector(nino: String, status: Int, body: String): Unit = {
    val url = s"/pensions-lifetime-allowance/individual/$nino/protection"
    stubPost(url, status, body)
  }

  def mockAmend(nino: String, status: Int, body: String, id: Long): Unit = {
    val url = s"/pensions-lifetime-allowance/individual/$nino/protections/$id"
    stubPut(url, status, body)
  }

}