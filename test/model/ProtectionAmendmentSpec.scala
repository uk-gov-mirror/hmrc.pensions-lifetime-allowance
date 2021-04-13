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

class ProtectionAmendmentSpec extends PlaySpec {

  "ProtectionApplication" when {

    "write correctly to Json" when {

      "missing all optional elements" in {
        val application = ProtectionAmendment("IP2016", 1, "Open", 1000.0, 2000.0, 3000.0, 4000.0, 5000.0)

        Json.toJson(application) shouldBe Json.parse(
          """
            |{
            | "protection": {
            |   "type":3,
            |   "status": 1,
            |   "version": 1,
            |   "relevantAmount": 1000.00,
            |   "postADayBCE":3000.00,
            |   "preADayPensionInPayment": 2000.00,
            |   "uncrystallisedRights": 4000.00,
            |   "nonUKRights": 5000.00
            | }
            |}
          """.stripMargin
        )
      }

      "including all optional elements" in {
        val application = ProtectionAmendment("IP2016", 1, "Open", 1000.0, 2000.0, 3000.0, 4000.0, 5000.0, Some(6000.0),
          Some(List(PensionDebit("2016-04-04", 1001.0), PensionDebit("2016-05-05", 1002.0))), Some("2017-04-04"))

        Json.toJson(application) shouldBe Json.parse(
          """
            |{
            | "pensionDebits": [
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
            |   "type":3,
            |   "status": 1,
            |   "version": 1,
            |   "relevantAmount": 1000.00,
            |   "postADayBCE":3000.00,
            |   "preADayPensionInPayment": 2000.00,
            |   "withdrawnDate": "2017-04-04",
            |   "uncrystallisedRights": 4000.00,
            |   "nonUKRights": 5000.00,
            |   "pensionDebitTotalAmount": 6000.00
            | }
            |}
          """.stripMargin
        )
      }
    }

    "read correctly from Json using the default formatter" in {
      val json = Json.parse(
        """
          |{
          | "protectionType": "IP2016",
          | "version": 1,
          | "status": "Open",
          | "relevantAmount": 1000.00,
          | "preADayPensionInPayment": 2000.00,
          | "postADayBenefitCrystallisationEvents": 3000.00,
          | "uncrystallisedRights": 4000.00,
          | "nonUKRights": 5000.00,
          | "pensionDebitTotalAmount": 6000.00,
          | "pensionDebits": [
          |   {
          |     "startDate": "2016-04-04",
          |     "amount": 1001.00
          |   },
          |   {
          |     "startDate": "2016-05-05",
          |     "amount": 1002.00
          |   }
          | ],
          | "withdrawnDate": "2017-04-04"
          |}
        """.stripMargin
      )

      json.as[ProtectionAmendment] shouldBe ProtectionAmendment("IP2016", 1, "Open", 1000.0, 2000.0, 3000.0, 4000.0, 5000.0, Some(6000.0),
        Some(List(PensionDebit("2016-04-04", 1001.0), PensionDebit("2016-05-05", 1002.0))), Some("2017-04-04"))
    }
  }

}
