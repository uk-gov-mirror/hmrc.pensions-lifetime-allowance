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

package events

import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.model.DataEvent
import play.api.libs.json.{JsNumber, JsObject}
import uk.gov.hmrc.http.HeaderCarrier

abstract class NPSBaseLTAEvent(
  ltaAuditType: String,
  transactionName: String,
  nino: String,
  npsRequestBodyJs: JsObject,
  npsResponseBodyJs: JsObject,
  statusCode: Int,
  path: String,
  extraDetail: Map[String, String])(implicit hc: HeaderCarrier)
extends DataEvent(
  auditSource = "pensions-lifetime-allowance",
  auditType = ltaAuditType,
  detail = Map[String,String](
    "nino" -> nino,
    "protectionType" ->  (npsRequestBodyJs \ "protection" \ "type").as[JsNumber].value.toInt.toString,
    "statusCode" -> statusCode.toString,
    "protectionStatus" -> (npsResponseBodyJs \ "protection").as[JsObject].fields.find(_._1 == "status").map(_._2.as[JsNumber].value.toInt.toString).getOrElse("n/a")
  ) ++ extraDetail,
  tags = hc.toAuditTags(transactionName, path)
)
