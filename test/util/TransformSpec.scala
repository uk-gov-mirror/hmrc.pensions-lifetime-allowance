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

  val fp2016ApplicationRequestBody = Json.parse(
      """
        | {
        |   "protectionType": "FP2016"
        | }
      """.stripMargin
    ).as[JsObject]

  val testNino = randomNino
  val (testNinoWithoutSuffix, testNinoSuffixChar) = dropNinoSuffix(testNino)

  val testProtectionId = 1
  val testProtectionVersion = 1

  val successfulCreateFP2016NPSResponseBody = Json.parse(
    s"""
      |  {
      |      "nino": "${testNinoWithoutSuffix}",
      |      "psaCheckReference" : "PSA123456789",
      |      "protection": {
      |        "id": ${testProtectionId},
      |        "version": ${testProtectionVersion},
      |        "type": 1,
      |        "certificateDate": "2015-05-22",
      |        "certificateTime": "12:22:59",
      |        "status": 1,
      |        "protectionReference": "FP161234567890C",
      |        "relevantAmount": 1250000.00,
      |        "protectedAmount": 1250000.00,
      |        "notificationID": 12
      |      }
      |    }
      |
    """.stripMargin).as[JsObject]

  val unsuccessfulCreateIP2014NPSResponseBody= Json.parse(
    s"""
      |  {
      |      "nino": "${testNinoWithoutSuffix}",
      |      "protection": {
      |        "id": ${testProtectionId},
      |        "version": ${testProtectionVersion},
      |        "type": 2,
      |        "status": 5,
      |        "notificationID": 10
      |      }
      |  }
    """.stripMargin).as[JsObject]

  val emptyReadProtectionsNPSResponseBody=Json.parse(
    s"""
       |{
       |  "nino" : "${testNinoWithoutSuffix}",
       |  "pensionSchemeAdministratorCheckReference": "PSA123456789",
       |  "protections": []
       |}
     """.stripMargin).as[JsObject]

  val readProtectionsNPSResponseBody =Json.parse(
    s"""
       |{
       |  "nino" : "${testNinoWithoutSuffix}",
       |  "pensionSchemeAdministratorCheckReference": "PSA123456789",
       |  "protections": [
       |    {
       |      "id": ${testProtectionId},
       |      "version": ${testProtectionVersion},
       |      "type": 1,
       |      "certificateDate": "2015-05-22",
       |      "certificateTime": "12:22:59",
       |      "status": 1,
       |      "protectionReference": "FP161234567890C",
       |      "relevantAmount": 1250000.00,
       |      "protectedAmount": 1250000.00,
       |      "notificationID": 12
       |    },
       |    {
       |      "id": ${testProtectionId + 1},
       |      "version": ${testProtectionVersion},
       |      "type": 2,
       |      "status": 5,
       |      "notificationID": 10
       |    }
       |  ]
       |}
     """.stripMargin).as[JsObject]

  val ip2016ApplicationRequestBody=Json.parse(
    s"""
       | {
       |   "protectionType": "IP2016",
       |   "postADayBenefitCrystallisationEvents": 100000.00,
       |   "preADayPensionInPayment": 100000.00,
       |   "uncrystallisedRights": 200000.00,
       |   "nonUKRights": 800000.00,
       |   "relevantAmount": 1200000.00
       | }
     """.stripMargin).as[JsObject]


  val ip2016AmendmentRequestBody= Json.parse(
    s"""
       |{
       |  "protectionType": "IP2016",
       |  "version": 1,
       |  "status": "Open",
       |  "postADayBenefitCrystallisationEvents": 100000.00,
       |  "preADayPensionInPayment": 100000.00,
       |  "uncrystallisedRights": 200000.00,
       |  "nonUKRights": 800000.00,
       |  "relevantAmount": 1200000.00,
       |  "withdrawnDate":"2015-12-01"
       |  }
     """.stripMargin).as[JsObject]

  val ip2016ApplicationRequestWithPensionDebitsBody = Json.parse(
    s"""
       | {
       |  "protectionType": "IP2016",
       |  "postADayBenefitCrystallisationEvents": 100000.00,
       |  "preADayPensionInPayment": 100000.00,
       |  "uncrystallisedRights": 200000.00,
       |  "nonUKRights": 800000.00,
       |  "relevantAmount": 1200000.00,
       |  "pensionDebits": [
       |    {
       |      "startDate": "2016-6-29",
       |      "amount": 4000.00
       |    },
       |    {
       |      "startDate": "2016-4-1",
       |      "amount": 623000.00
       |    }
       |  ]
       | }
     """.stripMargin
  ).as[JsObject]
}

class TransformSpec extends UnitSpec{

  import Transformers._
  import TransformSpec._

  "A valid received FP2016 protection application request" should {
    "transform to a valid NPS Create Lifetime Allowance request body" in {
      val npsRequestBody = transformApplyOrAmendRequestBody(testNinoWithoutSuffix, None, fp2016ApplicationRequestBody)
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
      val responseBody = transformApplyOrAmendResponseBody(testNinoSuffixChar.get, successfulCreateFP2016NPSResponseBody)
      val topLevelFields = responseBody.get.value
      topLevelFields.get("nino").get.as[JsString].value shouldEqual testNino
      topLevelFields.get("psaCheckReference").get.as[JsString].value shouldBe "PSA123456789"
      topLevelFields.get("protectionID").get.as[JsNumber].value.toInt shouldBe testProtectionId
      topLevelFields.get("version").get.as[JsNumber].value.toInt shouldBe testProtectionVersion
      topLevelFields.get("protectionType").get.as[JsString].value shouldBe "FP2016"
      topLevelFields.get("certificateDate").get.as[JsString].value shouldBe "2015-05-22T12:22:59"
      topLevelFields.get("status").get.as[JsString].value shouldBe "Open"
      topLevelFields.get("protectionReference").get.as[JsString].value shouldBe "FP161234567890C"
      topLevelFields.get("relevantAmount").get.as[JsNumber].value.toFloat shouldBe 1250000.00
      topLevelFields.get("notificationId").get.as[JsNumber].value.toInt shouldBe 12
    }
  }

  "A valid NPS response to an unsuccessful IP2014 Create Lifetime Allowance request" should {
    "transform to a unsuccessful but valid IP2014 application response body for the original MDTP client request" in {
      val responseBody = transformApplyOrAmendResponseBody(testNinoSuffixChar.get, unsuccessfulCreateIP2014NPSResponseBody)

      val topLevelFields = responseBody.get.value
      topLevelFields.get("nino").get.as[JsString].value shouldEqual testNino
      topLevelFields.get("protectionType").get.as[JsString].value shouldBe "IP2014"
      topLevelFields.get("protectionID").get.as[JsNumber].value.toInt shouldBe testProtectionId
      topLevelFields.get("version").get.as[JsNumber].value.toInt shouldBe testProtectionVersion
      topLevelFields.get("status").get.as[JsString].value shouldBe "Unsuccessful"
      topLevelFields.get("notificationId").get.as[JsNumber].value.toInt shouldBe 10
    }
  }

  "A valid received IP2016 protection application request" should {
    "transform to a valid NPS Create Lifetime Allowance request body" in {
      val npsRequestBody = transformApplyOrAmendRequestBody(testNinoWithoutSuffix, None, ip2016ApplicationRequestBody)

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

  "A valid received IP2016 protection amendment request" should {
    "transform to a valid NPS Amend Lifetime Allowance request body" in {
      val npsRequestBody = transformApplyOrAmendRequestBody(testNinoWithoutSuffix, Some(testProtectionId), ip2016AmendmentRequestBody)

      val npsTopLevelFields=npsRequestBody.get.value
      npsTopLevelFields.size shouldBe 2
      npsTopLevelFields.get("nino").get.as[JsString].value shouldEqual testNinoWithoutSuffix
      val protection=npsTopLevelFields.get("protection")
      protection.isDefined shouldBe true
      val protectionFields = protection.get.as[JsObject].value
      protectionFields.size shouldBe 10
      protectionFields.get("type").get.as[JsNumber].value.toInt shouldBe 3
      protectionFields.get("id").get.as[JsNumber].value.toInt shouldBe testProtectionId
      protectionFields.get("version").get.as[JsNumber].value.toInt shouldBe testProtectionVersion
      protectionFields.get("status").get.as[JsNumber].value.toInt shouldBe 1
      protectionFields.get("postADayBCE").get.as[JsNumber].value.toFloat shouldBe 100000.00
      protectionFields.get("preADayPensionInPayment").get.as[JsNumber].value.toFloat shouldBe 100000.00
      protectionFields.get("uncrystallisedRights").get.as[JsNumber].value.toFloat shouldBe 200000.00
      protectionFields.get("nonUKRights").get.as[JsNumber].value.toFloat shouldBe 800000.00
      protectionFields.get("relevantAmount").get.as[JsNumber].value.toFloat shouldBe 1200000.00
    }
  }

  "A valid received IP2016 protection application request with pension debits" should {
    "transform to a valid NPS Create Lifetime Allowance request body" in {
      val npsRequestBody = transformApplyOrAmendRequestBody(testNinoWithoutSuffix, None, ip2016ApplicationRequestWithPensionDebitsBody)

      val npsTopLevelFields=npsRequestBody.get.value
      npsTopLevelFields.size shouldBe 3
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

      // check penssion debits
      val pd=npsTopLevelFields.get("pensionDebits")
      val pdItems = pd.get.as[JsArray].value
      pdItems.size shouldBe 2
      val pd1 = pdItems(0).as[JsObject]
      pd1.value.get("pensionDebitEnteredAmount").get.as[JsNumber].value.toFloat shouldEqual 4000.00
      pd1.value.get("pensionDebitStartDate").get.as[JsString].value shouldBe "2016-6-29"
      val pd2 = pdItems(1).as[JsObject]
      pd2.value.get("pensionDebitEnteredAmount").get.as[JsNumber].value.toFloat shouldEqual 623000.00
      pd2.value.get("pensionDebitStartDate").get.as[JsString].value shouldBe "2016-4-1"
    }
  }

  "A valid received Read LTA Protections response body with an empty protections array" should {
    "tranform to a valid NPS Read protections" in {
      val responseBody = transformReadResponseBody(testNinoSuffixChar.get, emptyReadProtectionsNPSResponseBody)

      val topLevelFields = responseBody.get.value
      topLevelFields.get("nino").get.as[JsString].value shouldEqual testNino
      topLevelFields.get("psaCheckReference").get.as[JsString].value shouldBe "PSA123456789"
      topLevelFields.get("protections").get.as[JsArray].value.size shouldBe 0
    }
  }

  "A valid received Read LTA Protections response body with a non-empty protections array" should {
    "tranform to a valid NPS Read protections" in {
      val responseBody = transformReadResponseBody(testNinoSuffixChar.get, readProtectionsNPSResponseBody)

      val topLevelFields = responseBody.get.value
      topLevelFields.get("nino").get.as[JsString].value shouldEqual testNino
      topLevelFields.get("psaCheckReference").get.as[JsString].value shouldBe "PSA123456789"

      val protections=topLevelFields.get("lifetimeAllowanceProtections").get.as[JsArray].value
      protections.size shouldBe 2

      val p1Fields=protections(0).as[JsObject].value
      p1Fields.get("protectionID").get.as[JsNumber].value.toInt shouldBe testProtectionId
      p1Fields.get("version").get.as[JsNumber].value.toInt shouldBe testProtectionVersion
      p1Fields.get("protectionType").get.as[JsString].value shouldBe "FP2016"
      p1Fields.get("certificateDate").get.as[JsString].value shouldBe "2015-05-22T12:22:59"
      p1Fields.get("status").get.as[JsString].value shouldBe "Open"
      p1Fields.get("protectionReference").get.as[JsString].value shouldBe "FP161234567890C"
      p1Fields.get("relevantAmount").get.as[JsNumber].value.toFloat shouldBe 1250000.00
      p1Fields.get("notificationId").get.as[JsNumber].value.toInt shouldBe 12

      val p2Fields = protections(1).as[JsObject].value
      p2Fields.get("protectionType").get.as[JsString].value shouldBe "IP2014"
      p2Fields.get("protectionID").get.as[JsNumber].value.toInt shouldBe testProtectionId + 1
      p2Fields.get("version").get.as[JsNumber].value.toInt shouldBe testProtectionVersion
      p2Fields.get("status").get.as[JsString].value shouldBe "Unsuccessful"
      p2Fields.get("notificationId").get.as[JsNumber].value.toInt shouldBe 10
    }
  }
}
