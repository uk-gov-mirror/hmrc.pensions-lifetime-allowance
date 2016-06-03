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

import play.api.libs.json._

import uk.gov.hmrc.play.test.UnitSpec

import NinoHelper.dropNinoSuffix

object TransformSpec {

  import uk.gov.hmrc.domain.Generator
  import java.util.Random

  val rand = new Random()
  val ninoGenerator = new Generator(rand)
  def randomNino: String = ninoGenerator.nextNino.nino.replaceFirst("MA", "AA")

  import model.ProtectionApplication

  val fp2016ApplicationRequestBody=Json.toJson(ProtectionApplication(
    protectionType = "FP2016"
  )).as[JsObject]

  val testNino=randomNino
  val (testNinoWithoutSuffix, testNinoSuffixChar) = dropNinoSuffix(testNino)

  val successfulCreateFP2016NPSResponseBody=Json.parse(
    s"""
      |  {
      |      "nino": "${testNinoWithoutSuffix}",
      |      "psaCheckReference" : "PSA123456789",
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

  val unsuccessfulCreateFP2016NPSResponseBody=Json.parse(
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

  val ip2016ApplicationRequestBody=Json.toJson(ProtectionApplication(
    protectionType = "IP2016",
    postADayBenefitCrystallisationEvents = Some(100000.00),
    preADayPensionInPayment = Some(100000.00),
    uncrystallisedRights = Some(200000.00),
    nonUKRights = Some(800000.00),
    relevantAmount = Some(1200000.00)
  )).as[JsObject]
}

class TransformSpec extends UnitSpec{

  import Transformers._
  import TransformSpec._

  "A valid received FP2016 protection application request" should {
    "transform to a valid NPS Create Lifetime Allowance request body" in {
      val npsRequestBody = transformApplyRequestBody(testNinoWithoutSuffix, fp2016ApplicationRequestBody)
      val topLevelFields=npsRequestBody.get.value
      topLevelFields.size shouldBe 2
      topLevelFields.get("nino").get.as[JsString].value shouldEqual testNinoWithoutSuffix
      val protection=topLevelFields.get("protection")
      protection.isDefined shouldBe true
      val protectionFields = protection.get.as[JsObject].value
      protectionFields.get("type").get.as[JsNumber].value.toInt shouldBe 1
      protectionFields.size shouldBe 1
    }
  }

  "A valid NPS response to a successful FP2016 Create Lifetime Allowance request" should {
    "transform to a successful and valid FP2016 application response body for the original MDTP client request" in {
      val responseBody = transformApplyResponseBody(testNinoSuffixChar.get, successfulCreateFP2016NPSResponseBody)
      val topLevelFields = responseBody.get.value
      topLevelFields.get("nino").get.as[JsString].value shouldEqual testNino
      topLevelFields.get("psaCheckReference").get.as[JsString].value shouldBe "PSA123456789"
      topLevelFields.get("protectionID").get.as[JsNumber].value.toInt shouldBe 1
      topLevelFields.get("version").get.as[JsNumber].value.toInt shouldBe 1
      topLevelFields.get("protectionType").get.as[JsString].value shouldBe "FP2016"
      topLevelFields.get("certificateDate").get.as[JsString].value shouldBe "2015-05-22T12:22:59"
      topLevelFields.get("status").get.as[JsString].value shouldBe "Open"
      topLevelFields.get("protectionReference").get.as[JsString].value shouldBe "FP161234567890C"
      topLevelFields.get("relevantAmount").get.as[JsNumber].value.toFloat shouldBe 1250000.00
      topLevelFields.get("notificationId").get.as[JsNumber].value.toInt shouldBe 12
    }
  }

  "A valid NPS response to an unsuccessful IP2016 Create Lifetime Allowance request" should {
    "transform to a unsuccessful but valid FP2016 application response body for the original MDTP client request" in {
      val responseBody = transformApplyResponseBody(testNinoSuffixChar.get, unsuccessfulCreateFP2016NPSResponseBody)
      println(responseBody)
      val topLevelFields = responseBody.get.value
      topLevelFields.get("nino").get.as[JsString].value shouldEqual testNino
      topLevelFields.get("protectionType").get.as[JsString].value shouldBe "IP2014"
      topLevelFields.get("protectionID").get.as[JsNumber].value.toInt shouldBe 1
      topLevelFields.get("version").get.as[JsNumber].value.toInt shouldBe 1
      topLevelFields.get("status").get.as[JsString].value shouldBe "Unsuccessful"
      topLevelFields.get("notificationId").get.as[JsNumber].value.toInt shouldBe 10
    }
  }

  "A valid received IP2016 protection application request" should {
    "transform to a valid NPS Create Lifetime Allowance request body" in {
      val npsRequestBody = transformApplyRequestBody(testNinoWithoutSuffix, ip2016ApplicationRequestBody)
      println("==> " + npsRequestBody)
      val npsTopLevelFields=npsRequestBody.get.value
      npsTopLevelFields.size shouldBe 2
      npsTopLevelFields.get("nino").get.as[JsString].value shouldEqual testNinoWithoutSuffix
      val protection=npsTopLevelFields.get("protection")
      protection.isDefined shouldBe true
      val protectionFields = protection.get.as[JsObject].value
      protectionFields.size shouldBe 6
      protectionFields.get("type").get.as[JsNumber].value.toInt shouldBe 3
      protectionFields.get("postADayBCE").get.as[JsNumber].value.toFloat shouldBe 100000.00
      protectionFields.get("preADayPensionInPayment").get.as[JsNumber].value.toFloat shouldBe 100000.00
      protectionFields.get("uncrystallisedRights").get.as[JsNumber].value.toFloat shouldBe 200000.00
      protectionFields.get("nonUKRights").get.as[JsNumber].value.toFloat shouldBe 800000.00
      protectionFields.get("relevantAmount").get.as[JsNumber].value.toFloat shouldBe 1200000.00
    }
  }
}
