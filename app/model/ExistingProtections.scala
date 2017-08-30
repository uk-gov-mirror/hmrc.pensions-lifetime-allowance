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

case class ExistingProtections(nino: String,
                               pensionSchemeAdministratorCheckReference: String,
                               protections: Seq[Protection])

case class Protection(id: Int, version: Int, `type`: Int,
                      certificateDate: Option[LocalDate], certificateTime: Option[LocalTime],
                      status: Int, protectionReference: Option[String], relevantAmount: Option[BigDecimal],
                      preADayPensionInPayment: Option[BigDecimal], postADayBCE: Option[BigDecimal],
                      uncrystallisedRights: Option[BigDecimal], nonUKRights: Option[BigDecimal],
                      pensionDebitAmount: Option[BigDecimal], notificationID: Option[Int],
                      protectedAmount: Option[BigDecimal], pensionDebitEnteredAmount: Option[BigDecimal],
                      pensionDebitStartDate: Option[LocalDate], pensionDebitTotalAmount: Option[BigDecimal],
                      withdrawnDate: Option[LocalDate], previousVersions: List[ProtectionVersion]
                     )

case class ProtectionVersion(version: Int, link: String)

