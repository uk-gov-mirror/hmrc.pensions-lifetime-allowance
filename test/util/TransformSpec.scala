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

package util

import org.scalatest.Matchers.convertToAnyShouldWrapper
import play.api.libs.json.Json
import org.scalatestplus.play.PlaySpec

class TransformSpec extends PlaySpec {

  "Transformers" when {

    "convert a protection type to its' corresponding index" when {

      "supplied with an Unknown protection" in {
        Transformers.typeToInt("Unknown") shouldBe 0
      }

      "supplied with an FP2016 protection" in {
        Transformers.typeToInt("FP2016") shouldBe 1
      }

      "supplied with an IP2014 protection" in {
        Transformers.typeToInt("IP2014") shouldBe 2
      }

      "supplied with an IP2016 protection" in {
        Transformers.typeToInt("IP2016") shouldBe 3
      }

      "supplied with a Primary protection" in {
        Transformers.typeToInt("Primary") shouldBe 4
      }

      "supplied with a Enhanced protection" in {
        Transformers.typeToInt("Enhanced") shouldBe 5
      }

      "supplied with a Fixed protection" in {
        Transformers.typeToInt("Fixed") shouldBe 6
      }

      "supplied with a FP2014 protection" in {
        Transformers.typeToInt("FP2014") shouldBe 7
      }

      "supplied with a non-recognised protection" in {
        Transformers.typeToInt("") shouldBe -1
      }
    }

    "convert an index to its' corresponding protection type" when {

      "supplied with a code of 0" in {
        Transformers.intToType(0) shouldBe "Unknown"
      }

      "supplied with a code of 1" in {
        Transformers.intToType(1) shouldBe "FP2016"
      }

      "supplied with a code of 2" in {
        Transformers.intToType(2) shouldBe "IP2014"
      }

      "supplied with a code of 3" in {
        Transformers.intToType(3) shouldBe "IP2016"
      }

      "supplied with a code of 4" in {
        Transformers.intToType(4) shouldBe "Primary"
      }

      "supplied with a code of 5" in {
        Transformers.intToType(5) shouldBe "Enhanced"
      }

      "supplied with a code of 6" in {
        Transformers.intToType(6) shouldBe "Fixed"
      }

      "supplied with a code of 7" in {
        Transformers.intToType(7) shouldBe "FP2014"
      }
    }

    "convert a status to its' corresponding index" when {

      "supplied with a status of Unknown" in {
        Transformers.statusToInt("Unknown") shouldBe 0
      }

      "supplied with a status of Open" in {
        Transformers.statusToInt("Open") shouldBe 1
      }

      "supplied with a status of Dormant" in {
        Transformers.statusToInt("Dormant") shouldBe 2
      }

      "supplied with a status of Withdrawn" in {
        Transformers.statusToInt("Withdrawn") shouldBe 3
      }

      "supplied with a status of Expired" in {
        Transformers.statusToInt("Expired") shouldBe 4
      }

      "supplied with a status of Unsuccessful" in {
        Transformers.statusToInt("Unsuccessful") shouldBe 5
      }

      "supplied with a status of Rejected" in {
        Transformers.statusToInt("Rejected") shouldBe 6
      }

      "supplied with a non-recognised status" in {
        Transformers.statusToInt("") shouldBe -1
      }
    }

    "convert an index to its' corresponding status" when {

      "supplied with a code of 0" in {
        Transformers.intToStatus(0) shouldBe "Unknown"
      }

      "supplied with a code of 1" in {
        Transformers.intToStatus(1) shouldBe "Open"
      }

      "supplied with a code of 2" in {
        Transformers.intToStatus(2) shouldBe "Dormant"
      }

      "supplied with a code of 3" in {
        Transformers.intToStatus(3) shouldBe "Withdrawn"
      }

      "supplied with a code of 4" in {
        Transformers.intToStatus(4) shouldBe "Expired"
      }

      "supplied with a code of 5" in {
        Transformers.intToStatus(5) shouldBe "Unsuccessful"
      }

      "supplied with a code of 6" in {
        Transformers.intToStatus(6) shouldBe "Rejected"
      }
    }

    "read a datetime from json correctly using dateReads" when {

      "provided with both a date and time" in {
        val json = Json.parse(
          """
            |{
            | "certificateDate": "2015-05-05",
            | "certificateTime": "12:22:59"
            |}
          """.stripMargin)

        Json.fromJson[Option[String]](json)(Transformers.dateReads).get shouldBe Some("2015-05-05T12:22:59")
      }

      "provided with only a date" in {
        val json = Json.parse(
          """
            |{
            | "certificateDate": "2015-05-05"
            |}
          """.stripMargin)

        Json.fromJson[Option[String]](json)(Transformers.dateReads).get shouldBe None
      }

      "provided with only a time" in {
        val json = Json.parse(
          """
            |{
            | "certificateTime": "12:22:59"
            |}
          """.stripMargin)

        Json.fromJson[Option[String]](json)(Transformers.dateReads).get shouldBe None
      }

      "provided with no date or time" in {
        val json = Json.parse(
          """
            |{
            |}
          """.stripMargin)

        Json.fromJson[Option[String]](json)(Transformers.dateReads).get shouldBe None
      }
    }
  }
}
