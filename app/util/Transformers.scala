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

import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

object Transformers {
  val protectionTypes = Vector(
    "Unknown", "FP2016", "IP2014", "IP2016", "Primary", "Enhanced", "Fixed", "FP2014"
  )

  val protectionStatuses = Vector(
    "Unknown", "Open", "Dormant", "Withdrawn", "Expired", "Unsuccessful", "Rejected"
  )

  val typeToInt: String => Int = protectionType => protectionTypes.indexOf(protectionType)

  val statusToInt: String => Int = status => protectionStatuses.indexOf(status)

  val intToType: Int => String = id => protectionTypes.apply(id)

  val intToStatus: Int => String = id => protectionStatuses.apply(id)

  val dateReads: Reads[Option[String]] = {
    new Reads[Option[String]] {
      override def reads(json: JsValue): JsResult[Option[String]] = (
        (JsPath \ "certificateDate").readNullable[String] and
          (JsPath \ "certificateTime").readNullable[String]
        ) ((dateOpt, timeOpt) => (dateOpt, timeOpt) match {
        case (Some(date), Some(time)) => Some(date + "T" + time)
        case _ => None
      }).reads(json)
    }
  }
}
