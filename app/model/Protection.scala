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

package model

import java.time.LocalDateTime
import play.api.libs.json.{Json,JsValue,JsPath}
import play.api.libs.json.{Reads,Writes,Format}
import play.api.libs.functional.syntax._

import scala.util.Random

case class Protection(
                       nino: String,
                       protectionID: Long,
                       version: Int,
                       protectionType: String,
                       status: String,
                       notificationId: Option[Short],
                       notificationMsg: Option[String], // this field is stored in the DB but excluded from API responses
                       protectionReference: Option[String],
                       certificateDate: Option[LocalDateTime] = None,
                       relevantAmount: Option[Double] = None,
                       preADayPensionsInPayment: Option[Double] = None,
                       postADayBenefitCrystallisationEvents: Option[Double] = None,
                       uncrystallisedRights: Option[Double] = None,
                       pensionDebitAmount: Option[Double] = None,
                       nonUKRights: Option[Double] = None,
                       self: Option[String] = None, // dynamically added when protections are retrieved and returned to clients
                       previousVersions: Option[List[String]] = None) // dynamically added as above

object Protection {

  object Status extends Enumeration {
    val Unknown, Open,Dormant,Withdrawn,Unsuccessful,Rejected =Value
  }

  object Type extends Enumeration {
    val Primary, Enhanced, Fixed, FP2014, FP2016, IP2014, IP2016 = Value
  }

  implicit val localDateTimeReads = Reads[LocalDateTime](js =>
    js.validate[String].map[LocalDateTime](dtString =>
      LocalDateTime.parse(dtString)
    )
  )
  implicit val localDateTimeWrites = new Writes[LocalDateTime] {
    val formatter = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME

    def writes(ldt: LocalDateTime): JsValue = Json.toJson(ldt.format(formatter))
  }

  implicit val localDateTimeFormat = Format(localDateTimeReads, localDateTimeWrites)

  implicit lazy val protectionFormat = Json.format[Protection]
}
