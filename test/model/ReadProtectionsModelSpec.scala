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

class ReadProtectionsModelSpec extends PlaySpec {

  "ReadProtectionsModel" when {

    "read correctly from json" when {

      "provided with empty protections" in {
        val json = Json.parse(
          """
            |{
            | "nino": "A000001",
            | "pensionSchemeAdministratorCheckReference" : "PSA123456789",
            | "protections": []
            |}
          """.stripMargin)

        Json.fromJson[ReadProtectionsModel](json).get shouldBe ReadProtectionsModel("A000001", "PSA123456789", Some(List.empty))
      }

      "provided with no protections" in {
        val json = Json.parse(
          """
            |{
            | "nino": "A000001",
            | "pensionSchemeAdministratorCheckReference" : "PSA123456789"
            |}
          """.stripMargin)

        Json.fromJson[ReadProtectionsModel](json).get shouldBe ReadProtectionsModel("A000001", "PSA123456789", None)
      }

      "provided with one protection" in {
        val model = ReadProtection(protectionID = 1, version = 1, protectionType = "FP2016", status = "Open")
        val json = Json.parse(
          """
            |{
            | "nino": "A000001",
            | "pensionSchemeAdministratorCheckReference" : "PSA123456789",
            | "protections" :  [
            |   {
            |     "id": 1,
            |     "version": 1,
            |     "type": 1,
            |     "status": 1
            |   }
            | ]
            |}
          """.stripMargin)

        Json.fromJson[ReadProtectionsModel](json).get shouldBe ReadProtectionsModel("A000001", "PSA123456789", Some(List(model)))
      }

      "provided with multiple protections" in {
        val model = ReadProtection(protectionID = 1, version = 1, protectionType = "FP2016", status = "Open")
        val json = Json.parse(
          """
            |{
            | "nino": "A000001",
            | "pensionSchemeAdministratorCheckReference" : "PSA123456789",
            | "protections" :  [
            |   {
            |     "id": 1,
            |     "version": 1,
            |     "type": 1,
            |     "status": 1
            |   },
            |   {
            |     "id": 1,
            |     "version": 1,
            |     "type": 1,
            |     "status": 1
            |   }
            | ]
            |}
          """.stripMargin)

        Json.fromJson[ReadProtectionsModel](json).get shouldBe ReadProtectionsModel("A000001", "PSA123456789", Some(List(model, model)))
      }
    }
  }
}
