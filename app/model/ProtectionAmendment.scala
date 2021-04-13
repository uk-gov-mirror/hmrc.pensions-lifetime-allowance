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
import play.api.libs.json._

case class ProtectionAmendment(
                                protectionType: String, // must be either "IP2014" or "IP2016"
                                version: Int, // version of protection to update
                                status: String, // status of protection to update
                                relevantAmount: Double,
                                preADayPensionInPayment: Double,
                                postADayBenefitCrystallisationEvents: Double,
                                uncrystallisedRights: Double,
                                nonUKRights: Double,
                                pensionDebitTotalAmount: Option[Double] = None,
                                pensionDebits: Option[List[PensionDebit]] = None,
                                withdrawnDate: Option[String] = None)

object ProtectionAmendment {

  implicit val protectionAmendmentFormat = {
    val jsonReads = Json.reads[ProtectionAmendment]

    val jsonWrites: Writes[ProtectionAmendment] = new Writes[ProtectionAmendment] {
      override def writes(o: ProtectionAmendment): JsValue = JsObject(Json.obj(
        "pensionDebits" -> o.pensionDebits,
        "protection" -> JsObject(Json.obj(
          "type" -> Transformers.typeToInt(o.protectionType),
          "status" -> Transformers.statusToInt(o.status),
          "version" -> o.version,
          "relevantAmount" -> o.relevantAmount,
          "postADayBCE" -> o.postADayBenefitCrystallisationEvents,
          "preADayPensionInPayment" -> o.preADayPensionInPayment,
          "withdrawnDate" -> o.withdrawnDate,
          "uncrystallisedRights" -> o.uncrystallisedRights,
          "nonUKRights" -> o.nonUKRights,
          "pensionDebitTotalAmount" -> o.pensionDebitTotalAmount
        ).fields.filterNot(_._2 == JsNull))
      ).fields.filterNot(_._2 == JsNull))
    }

    Format(jsonReads, jsonWrites)
  }
}
