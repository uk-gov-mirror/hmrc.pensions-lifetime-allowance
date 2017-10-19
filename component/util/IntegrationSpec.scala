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

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers}
import org.scalatestplus.play.OneServerPerSuite
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

trait IntegrationSpec extends UnitSpec
  with OneServerPerSuite with ScalaFutures with IntegrationPatience with Matchers
  with WiremockHelper with BeforeAndAfterEach with BeforeAndAfterAll {

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

  def mockCitizenDetails(nino: String, status: Int): Unit = {
    val url = s"/citizen-details/$nino/designatory-details"
    stubGet(url, status, "")
  }

  def mockAudit(status: Int): Unit = {
    val url = s"/write/audit"
    stubPost(url, status, "audit-response")
  }

  def mockNPSConnector(nino: String, status: Int): Unit = {
    val url = s"/pensions-lifetime-allowance/individual/$nino/protection"
    stubPost(url, status, "")
  }
}