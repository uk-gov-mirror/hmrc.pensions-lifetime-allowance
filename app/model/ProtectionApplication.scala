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

import play.api.libs.json.Json

case class ProtectionApplication (
  protectionType: String,
  relevantAmount: Option[Double] = None,
  preADayPensionInPayment: Option[Double] = None,
  postADayBenefitCrystallisationEvents: Option[Double] = None,
  uncrystallisedRights: Option[Double] = None,
  nonUKRights: Option[Double] = None,
  pensionDebitAmount: Option[Double] = None) {

  import Protection.Type._

  def requestedType: Option[Protection.Type.Value] = protectionType match {
    case "FP2016" => Some(FP2016)
    case "IP2014" => Some(IP2014)
    case "IP2016" => Some(IP2016)
    case _ => None
  }
}
  object ProtectionApplication {
    implicit val protectionApplicationFormat = Json.format[ProtectionApplication]
}
