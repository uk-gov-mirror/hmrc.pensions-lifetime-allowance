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

import java.time.LocalDate
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class ProtectionAmendment (
  protectionType: ProtectionType.Value,  // must be either "IP2014" or "IP2016"
  version: Int, // version of protection to update
  status: ProtectionStatus.Value, // status of protection to update
  relevantAmount: Money,
  preADayPensionInPayment: Money,
  postADayBenefitCrystallisationEvents: Money,
  uncrystallisedRights: Money,
  nonUKRights: Money,
  pensionDebitTotalAmount: Option[Money] = None,
  pensionDebits: List[(LocalDate, Money)] = Nil) {

  {
    import ProtectionType._
    val permissibleTypes = List(Individual2014,Individual2016)
    require(
      permissibleTypes contains protectionType,
      s"${this.getClass.getSimpleName} must be " ++
        s"${permissibleTypes.map(_.toString).mkString(" or ")}"
    )
  }
}

object ProtectionAmendment {
  implicit val protectionAmendmentFormat: Format[ProtectionAmendment] = (
    (__ \ "protectionType").format[ProtectionType.Value] and
    (__ \ "version").format[Int] and
    (__ \ "status").format[ProtectionStatus.Value] and
    (__ \ "relevantAmount").format[Money] and
    (__ \ "preADayPensionInPayment").format[Money] and
    (__ \ "postADayBenefitCrystallisationEvents").format[Money] and
    (__ \ "uncrystallisedRights").format[Money] and
    (__ \ "nonUKRights").format[Money] and
    (__ \ "pensionDebitsTotalAmount").formatNullable[Money] and      
    (__ \ "pensionDebits").formatNullable[List[(LocalDate,Money)]].inmap(_.getOrElse(Nil),Some(_: List[(LocalDate,Money)]))
  )(ProtectionAmendment.apply, unlift(ProtectionAmendment.unapply))

}
