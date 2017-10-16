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
import connectors.NpsConnector._
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.play.http.HttpResponse

class TestConnectorSpec extends IntegrationSpec {

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(fakeConfig())
    .build()

  "Test connector" should {

    "return a wiremock response" in {

      stubGet("/test", 200,
        """{
          |"result":"success"
          |}""".stripMargin)


      lazy val httpGet = await(WSHttp.GET[HttpResponse](baseUrl("nps") + "/test"))

      httpGet.status shouldBe 200
      httpGet.body shouldBe """{
                              |"result":"success"
                              |}""".stripMargin
    }
  }
}
