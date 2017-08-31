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

import java.time.{LocalDate, LocalTime}

case class ExistingProtections(
  nino: String,
  pensionSchemeAdministratorCheckReference: String,
  protections: Seq[Protection]
)

case class Protection(
  id: Int,
  version: Int,
  protectionType: ProtectionType.Value,
  certificateDate: Option[LocalDate],
  certificateTime: Option[LocalTime],
  status: ProtectionStatus.Value,
  protectionReference: Option[String],
  relevantAmount: Option[Money],
  preADayPensionInPayment: Option[Money],
  postADayBCE: Option[Money],
  uncrystallisedRights: Option[Money],
  nonUKRights: Option[Money],
  pensionDebitAmount: Option[Money],
  notificationID: Option[Int],
  protectedAmount: Option[Money],
  pensionDebitEnteredAmount: Option[Money],
  pensionDebitStartDate: Option[LocalDate],
  pensionDebitTotalAmount: Option[Money],
  withdrawnDate: Option[LocalDate],
  previousVersions: List[(Int, String)]
)

object ProtectionType extends Enumeration {
  val Unknown, Fixed2016, Individual2014, Individual2016, Primary, Enhanced, Fixed, Fixed2014 = Value
}

object ProtectionStatus extends Enumeration {
  val Unknown, Open, Dormant, Withdrawn, Expired, Unsuccessful, Rejected = Value
}
