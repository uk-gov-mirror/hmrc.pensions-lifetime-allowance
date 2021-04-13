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

import _root_.util.Transformers
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class ReadProtection(
                           protectionID: Long,
                           certificateDate: Option[String] = None,
                           version: Int,
                           protectionType: String,
                           status: String,
                           protectedAmount: Option[Double] = None,
                           relevantAmount: Option[Double] = None,
                           postADayBenefitCrystallisationEvents: Option[Double] = None,
                           preADayPensionInPayment: Option[Double] = None,
                           uncrystallisedRights: Option[Double] = None,
                           nonUKRights: Option[Double] = None,
                           pensionDebitAmount: Option[Double] = None,
                           pensionDebitEnteredAmount: Option[Double] = None,
                           pensionDebitStartDate: Option[String] = None,
                           pensionDebitTotalAmount: Option[Double] = None,
                           notificationId: Option[Int] = None,
                           protectionReference: Option[String] = None,
                           withdrawnDate: Option[String] = None
                         )

object ReadProtection {

  implicit val formats: Format[ReadProtection] = {

    val jsonReads: Reads[ReadProtection] = new Reads[ReadProtection] {
      override def reads(json: JsValue): JsResult[ReadProtection] = ((JsPath \ "id").read[Long] and
        JsPath.read(Transformers.dateReads) and
        (JsPath \ "version").read[Int] and
        (JsPath \ "type").read[Int].map(Transformers.intToType) and
        (JsPath \ "status").read[Int].map(Transformers.intToStatus) and
        (JsPath \ "protectedAmount").readNullable[Double] and
        (JsPath \ "relevantAmount").readNullable[Double] and
        (JsPath \ "postADayBCE").readNullable[Double] and
        (JsPath \ "preADayPensionInPayment").readNullable[Double] and
        (JsPath \ "uncrystallisedRights").readNullable[Double] and
        (JsPath \ "nonUKRights").readNullable[Double] and
        (JsPath \ "pensionDebitAmount").readNullable[Double] and
        (JsPath \ "pensionDebitEnteredAmount").readNullable[Double] and
        (JsPath \ "pensionDebitStartDate").readNullable[String] and
        (JsPath \ "pensionDebitTotalAmount").readNullable[Double] and
        (JsPath \ "notificationID").readNullable[Int] and
        (JsPath \ "protectionReference").readNullable[String] and
        (JsPath \ "withdrawnDate").readNullable[String]
        ) (ReadProtection.apply _).reads(json)
    }

    val jsonWrites: Writes[ReadProtection] = Json.writes[ReadProtection]

    Format(jsonReads, jsonWrites)
  }
}
