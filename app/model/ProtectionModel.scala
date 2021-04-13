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

case class ProtectionModel(
                            nino: String,
                            psaCheckReference: Option[String] = None,
                            protectionID: Option[Long] = None,
                            certificateDate: Option[String] = None,
                            version: Option[Int] = None,
                            protectionType: String,
                            status: Option[String] = None,
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

object ProtectionModel {

  implicit val format: Format[ProtectionModel] = {

    val jsonWrites: Writes[ProtectionModel] = Json.writes[ProtectionModel]

    val jsonReads: Reads[ProtectionModel] = new Reads[ProtectionModel] {
      override def reads(json: JsValue): JsResult[ProtectionModel] = (
        (JsPath \ "nino").read[String] and
          (JsPath \ "pensionSchemeAdministratorCheckReference").readNullable[String] and
          (JsPath \ "protection" \ "id").readNullable[Long] and
          (JsPath \ "protection").read(Transformers.dateReads) and
          (JsPath \ "protection" \ "version").readNullable[Int] and
          (JsPath \ "protection" \ "type").read[Int].map(Transformers.intToType) and
          (JsPath \ "protection" \ "status").readNullable[Int].map(_.map(Transformers.intToStatus)) and
          (JsPath \ "protection" \ "protectedAmount").readNullable[Double] and
          (JsPath \ "protection" \ "relevantAmount").readNullable[Double] and
          (JsPath \ "protection" \ "postADayBCE").readNullable[Double] and
          (JsPath \ "protection" \ "preADayPensionInPayment").readNullable[Double] and
          (JsPath \ "protection" \ "uncrystallisedRights").readNullable[Double] and
          (JsPath \ "protection" \ "nonUKRights").readNullable[Double] and
          (JsPath \ "protection" \ "pensionDebitAmount").readNullable[Double] and
          (JsPath \ "protection" \ "pensionDebitEnteredAmount").readNullable[Double] and
          (JsPath \ "protection" \ "pensionDebitStartDate").readNullable[String] and
          (JsPath \ "protection" \ "pensionDebitTotalAmount").readNullable[Double] and
          (JsPath \ "protection" \ "notificationID").readNullable[Int] and
          (JsPath \ "protection" \ "protectionReference").readNullable[String] and
          (JsPath \ "protection" \ "withdrawnDate").readNullable[String]
        ) (ProtectionModel.apply _).reads(json)
    }

    Format(jsonReads, jsonWrites)
  }
}