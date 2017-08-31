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
import java.time.LocalDate
import play.api.libs.functional.syntax._

case class ProtectionApplication (
  protectionType: ProtectionType.Value,
  relevantAmount: Option[Money] = None,
  preADayPensionInPayment: Option[Money] = None,
  postADayBenefitCrystallisationEvents: Option[Money] = None,
  uncrystallisedRights: Option[Money] = None,
  nonUKRights: Option[Money] = None,
  pensionDebits: List[(LocalDate, Money)] = Nil)

object ProtectionApplication {

  implicit val protectionApplicationFormat: Format[ProtectionApplication] = (
    (__ \ "protectionType").format[ProtectionType.Value] and
    (__ \ "relevantAmount").formatNullable[Money] and
    (__ \ "preADayPensionInPayment").formatNullable[Money] and
    (__ \ "postADayBenefitCrystallisationEvents").formatNullable[Money] and
    (__ \ "uncrystallisedRights").formatNullable[Money] and
    (__ \ "nonUKRights").formatNullable[Money] and
    (__ \ "pensionDebits").formatNullable[List[(LocalDate,Money)]].inmap(_.getOrElse(Nil),Some(_: List[(LocalDate,Money)]))
  )(ProtectionApplication.apply, unlift(ProtectionApplication.unapply))
}
