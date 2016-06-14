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

package events

import play.api.libs.json.JsValue
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.audit.EventTypes
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.http.HeaderCarrier

class NPSCreateLTAEvent(nino: String,
                        path: String,
                        requestBody: String,
                        responseBody: String)(implicit hc: HeaderCarrier)
  extends DataEvent(
    auditSource = "pla",
    auditType = EventTypes.OutboundCall,
    tags = hc.toAuditTags("CreateLTAProtection",path),
    detail = hc.toAuditDetails()  ++ Map(
      "nino" -> nino,
      "requestBody" -> requestBody.toString,
      "responseBody" -> responseBody.toString
    )
  )

object NPSCreateLTAEvent {
  def apply(nino: String,
            path: String,
            requestBody: String,
            responseBody: String)(implicit hc: HeaderCarrier) =
  {
      new NPSCreateLTAEvent(nino,path,requestBody,responseBody)
  }
}