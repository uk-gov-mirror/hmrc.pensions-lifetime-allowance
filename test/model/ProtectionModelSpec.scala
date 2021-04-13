/*
 * Copyright 2021 HM Revenue & Customs
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

package model

import org.scalatest.Matchers.convertToAnyShouldWrapper
import play.api.libs.json.Json
import org.scalatestplus.play.PlaySpec

class ProtectionModelSpec extends PlaySpec {

  "ProtectionModel" when {

    "read correctly from Json" when {

      "supplied with no optional values" in {
        val json = Json.parse(
          """
            |{
            | "nino": "AA000001",
            | "protection": {
            |   "type": 1
            | }
            |}
          """.stripMargin)

        Json.fromJson[ProtectionModel](json).get shouldBe ProtectionModel(nino = "AA000001", protectionType = "FP2016")
      }

      "supplied with all optional values" in {
        val json = Json.parse(
          s"""
             |  {
             |      "nino": "AA000001",
             |      "pensionSchemeAdministratorCheckReference" : "PSA123456789",
             |      "protection": {
             |        "id": 5,
             |        "version": 1,
             |        "type": 3,
             |        "certificateDate": "2015-05-22",
             |        "certificateTime": "12:22:59",
             |        "status": 1,
             |        "protectionReference": "FP161234567890C",
             |        "relevantAmount": 1250000.00,
             |        "protectedAmount": 1250000.00,
             |        "notificationID": 12,
             |        "postADayBCE": 15000.00,
             |        "preADayPensionInPayment": 12000.00,
             |        "uncrystallisedRights": 11000.00,
             |        "withdrawnDate": "2016-05-05",
             |        "nonUKRights": 1000.00,
             |        "pensionDebitAmount": 5000.00,
             |        "pensionDebitEnteredAmount": 4000.00,
             |        "pensionDebitStartDate": "2016-04-04",
             |        "pensionDebitTotalAmount": 3000.00
             |      }
             |    }
             |
          """.stripMargin)

        Json.fromJson[ProtectionModel](json).get shouldBe ProtectionModel(
          "AA000001", Some("PSA123456789"), Some(5), Some("2015-05-22T12:22:59"), Some(1), "IP2016", Some("Open"), Some(1250000.0),
          Some(1250000.0), Some(15000.0), Some(12000.0), Some(11000.0), Some(1000.0), Some(5000.0), Some(4000.0), Some("2016-04-04"),
          Some(3000.0), Some(12), Some("FP161234567890C"), Some("2016-05-05")
        )
      }
    }
  }

}
