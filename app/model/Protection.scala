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
                       id: Long,
                       version: Int,
                       `type`: Int,
                       status: Int,
                       notificationID: Option[Short],
                       notificationMsg: Option[String], // this field is stored in the DB but excluded from API responses
                       protectionReference: Option[String],
                       certificateDate: Option[String] = None,
                       certificateTime: Option[String] = None,
                       relevantAmount: Option[Double] = None,
                       preADayPensionInPayment: Option[Double] = None,
                       postADayBCE: Option[Double] = None,
                       uncrystallisedRights: Option[Double] = None,
                       pensionDebitAmount: Option[List[Double]] = None,
                       nonUKRights: Option[Double] = None,
                       self: Option[String] = None, // dynamically added when protections are retrieved and returned to clients
                       previousVersions: Option[List[String]] = None) /* dynamically added as above */ {

  import Protection.Type._
  def requestedType: Option[Protection.Type.Value] = `type` match {
    case 1 => Some(FP2016)
    case 2 => Some(IP2014)
    case 3 => Some(IP2016)
    case 4 => Some(Primary)
    case 5 => Some(Enhanced)
    case 6 => Some(Fixed)
    case 7 => Some(FP2014)
    case _ => None
  }

  import Protection.Status._
  def requestedStatus: Option[Protection.Status.Value] = status match {
    case 1 => Some(Open)
    case 2 => Some(Dormant)
    case 3 => Some(Withdrawn)
    case 4 => Some(Expired)
    case 5 => Some(Unsuccessful)
    case 6 => Some(Rejected)
    case _ => None
  }
}

object Protection {

  object Status extends Enumeration {
    val Unknown, Open,Dormant,Withdrawn,Expired,Unsuccessful,Rejected =Value
  }

  def extractedStatus(pStatus: Status.Value): Int = {
    pStatus match {
      case Status.Open          => 1
      case Status.Dormant       => 2
      case Status.Withdrawn     => 3
      case Status.Expired       => 4
      case Status.Unsuccessful  => 5
      case Status.Rejected      => 6
    }
  }

  object Type extends Enumeration {
    val Primary, Enhanced, Fixed, FP2014, FP2016, IP2014, IP2016 = Value
  }

  def extractedType(pType: Type.Value): Int = {
    pType match {
      case Type.FP2016   => 1
      case Type.IP2014   => 2
      case Type.IP2016   => 3
      case Type.Primary  => 4
      case Type.Enhanced => 5
      case Type.Fixed    => 6
      case Type.FP2014   => 7
    }
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