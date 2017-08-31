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

import play.api.libs.json._
import play.api.libs.functional.syntax._
import java.time.LocalDate

package object model {

  type Money = BigDecimal

  implicit class RichStr(i: String) {
    def asErrorJson: JsObject = JsObject(Seq("message" -> JsString(i)))
  }

  implicit val dateMoneyTupleFormatter: Format[(LocalDate,Money)] = (
    (__ \ "startDate").format[LocalDate] and
    (__ \ "amount").format[Money]
  ).tupled

  implicit val protectionTypeFormatter: Format[ProtectionType.Value] =
    __.format[Int].inmap(ProtectionType(_),{x: ProtectionType.Value => x.id})

  implicit val protectionStatusFormatter: Format[ProtectionStatus.Value] =
    __.format[Int].inmap(ProtectionStatus(_),{x: ProtectionStatus.Value => x.id})

}
