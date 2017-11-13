/*
 * Copyright 2017 HM Revenue & Customs
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

import play.api.libs.json._
import _root_.util.Transformers

case class ProtectionApplication(
                                  protectionType: String,
                                  relevantAmount: Option[Double] = None,
                                  preADayPensionInPayment: Option[Double] = None,
                                  postADayBenefitCrystallisationEvents: Option[Double] = None,
                                  uncrystallisedRights: Option[Double] = None,
                                  nonUKRights: Option[Double] = None,
                                  pensionDebits: Option[List[PensionDebit]] = None)

object ProtectionApplication {

  val typeToInt: String => Int = protectionType => Transformers.protectionTypes.indexOf(protectionType)

  val jsonReads = Json.reads[ProtectionApplication]

  val jsonWrites: Writes[ProtectionApplication] = new Writes[ProtectionApplication] {
    override def writes(o: ProtectionApplication): JsValue = {
      JsObject(Json.obj(
        "pensionDebits" -> o.pensionDebits,
        "protection" -> JsObject(Json.obj(
          "type" -> typeToInt(o.protectionType),
          "relevantAmount" -> o.relevantAmount,
          "postADayBCE" -> o.postADayBenefitCrystallisationEvents,
          "preADayPensionInPayment" -> o.preADayPensionInPayment,
          "uncrystallisedRights" -> o.uncrystallisedRights,
          "nonUKRights" -> o.nonUKRights
        ).fields.filterNot(_._2 == JsNull))
      ).fields.filterNot(_._2 == JsNull))
    }
  }

  implicit val protectionApplicationFormat = Format(jsonReads, jsonWrites)
}
