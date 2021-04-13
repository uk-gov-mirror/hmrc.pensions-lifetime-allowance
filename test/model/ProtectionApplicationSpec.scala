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
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

class ProtectionApplicationSpec extends PlaySpec {

  "ProtectionApplication" when {

    "write correctly to Json" when {

      "missing all optional elements" in {
        val application = ProtectionApplication("FP2016")

        Json.toJson(application) shouldBe Json.parse(
          """
            |{
            | "protection": {
            |   "type": 1
            | }
            |}
          """.stripMargin)
      }

      "including all optional elements" in {
        val application = ProtectionApplication("FP2016", Some(1000.0), Some(2000.0), Some(3000.0), Some(4000.0), Some(5000.0),
          Some(List(PensionDebit("2016-04-04", 1001.0), PensionDebit("2016-05-05", 1002.0))))

        Json.toJson(application) shouldBe Json.parse(
          """
            |{
            |   "pensionDebits": [
            |     {
            |       "pensionDebitStartDate": "2016-04-04",
            |       "pensionDebitEnteredAmount": 1001.00
            |     },
            |     {
            |       "pensionDebitStartDate": "2016-05-05",
            |       "pensionDebitEnteredAmount": 1002.00
            |     }
            |   ],
            | "protection": {
            |   "type": 1,
            |   "relevantAmount": 1000.00,
            |   "postADayBCE": 3000.00,
            |   "preADayPensionInPayment": 2000.00,
            |   "uncrystallisedRights": 4000.00,
            |   "nonUKRights": 5000.00
            | }
            |}
          """.stripMargin
        )
      }
    }

    "read correctly from Json using the default formatter" in {
      Json.parse(
        """
          |{
          | "protectionType": "FP2016",
          | "relevantAmount": 1000.00,
          | "preADayPensionInPayment": 2000.00,
          | "postADayBenefitCrystallisationEvents": 3000.00,
          | "uncrystallisedRights": 4000.00,
          | "nonUKRights": 5000.00,
          | "pensionDebits": [
          |     {
          |       "startDate": "2016-04-04",
          |       "amount": 1001.00
          |     },
          |     {
          |       "startDate": "2016-05-05",
          |       "amount": 1002.00
          |     }
          |   ]
          |}
        """.stripMargin
      ).as[ProtectionApplication] shouldBe ProtectionApplication("FP2016", Some(1000.0), Some(2000.0), Some(3000.0), Some(4000.0), Some(5000.0),
        Some(List(PensionDebit("2016-04-04", 1001.0), PensionDebit("2016-05-05", 1002.0))))
    }
  }
}
