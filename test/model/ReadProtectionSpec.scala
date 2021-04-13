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

class ReadProtectionSpec extends PlaySpec {

  "ReadProtection" when {

    "correctly read from json" when {

      "supplied with no optional values" in {
        val json = Json.parse(
          """
            |{
            | "id": 1,
            | "version": 1,
            | "type": 1,
            | "status": 1
            |}
          """.stripMargin)

        Json.fromJson[ReadProtection](json).get shouldBe ReadProtection(protectionID = 1, version = 1, protectionType = "FP2016", status = "Open")
      }

      "supplied with all optional values" in {
        val json = Json.parse(
          """
            |{
            | "id": 1,
            | "certificateDate": "2015-05-22",
            | "certificateTime": "12:22:59",
            | "version": 1,
            | "type": 3,
            | "status": 3,
            | "protectionReference": "FP161234567890C",
            | "relevantAmount": 1250000.00,
            | "protectedAmount": 1250000.00,
            | "notificationID": 12,
            | "postADayBCE": 15000.00,
            | "preADayPensionInPayment": 12000.00,
            | "uncrystallisedRights": 11000.00,
            | "withdrawnDate": "2016-05-05",
            | "nonUKRights": 1000.00,
            | "pensionDebitAmount": 5000.00,
            | "pensionDebitEnteredAmount": 4000.00,
            | "pensionDebitStartDate": "2016-04-04",
            | "pensionDebitTotalAmount": 3000.00
            |}
          """.stripMargin)

        Json.fromJson[ReadProtection](json).get shouldBe ReadProtection(1, Some("2015-05-22T12:22:59"), 1, "IP2016", "Withdrawn", Some(1250000.0),
          Some(1250000.0), Some(15000.0), Some(12000.0), Some(11000.0), Some(1000.0), Some(5000.0), Some(4000.0), Some("2016-04-04"),
          Some(3000.0), Some(12), Some("FP161234567890C"), Some("2016-05-05"))
      }
    }
  }
}
