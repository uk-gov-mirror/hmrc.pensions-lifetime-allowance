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

package services

import connectors.NpsConnector
import play.api.libs.json.{JsObject,JsResult}
import uk.gov.hmrc.play.http.{HttpResponse, HeaderCarrier}

import scala.concurrent.{ExecutionContext, Future}

import util.{NinoHelper, Transformers}

object ProtectionService extends ProtectionService {
  override val nps: NpsConnector = NpsConnector
}

trait ProtectionService {

  val nps: NpsConnector

  def applyForProtection(nino: String, applicationRequestBody: JsObject)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[JsResult[JsObject]] = {

    val (ninoWithoutSuffix, lastNinoCharOpt) = NinoHelper.dropNinoSuffix(nino)
    val npsRequestBody: JsResult[JsObject] = Transformers.transformApplyRequestBody(ninoWithoutSuffix, applicationRequestBody)
    npsRequestBody.fold(
      errors => Future.successful(npsRequestBody), // caller will handle any transformation error(s)
      req => nps.applyForProtection(nino, req) map { npsResponse =>
        Transformers.transformApplyResponseBody(lastNinoCharOpt.get, npsResponse)
      }     
    )
  }
}
