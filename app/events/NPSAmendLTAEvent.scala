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

import play.api.libs.json.JsObject
import uk.gov.hmrc.http.HeaderCarrier

class NPSAmendLTAEvent(
    nino: String,
    id: Long,
    npsRequestBodyJs: JsObject,
    npsResponseBodyJs: JsObject,
    statusCode: Int,
    path: String)(implicit hc: HeaderCarrier)
  extends NPSBaseLTAEvent(
    ltaAuditType = "AmendAllowance",
    transactionName="amend-pensions-lifetime-allowance",
    nino = nino,
    npsRequestBodyJs = npsRequestBodyJs,
    npsResponseBodyJs = npsResponseBodyJs,
    statusCode=statusCode,
    path = path,
    extraDetail = Map("protectionId" -> id.toString))
