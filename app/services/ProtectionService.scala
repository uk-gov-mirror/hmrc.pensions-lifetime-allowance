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

package services

import _root_.util.{NinoHelper, Transformers}
import connectors.NpsConnector
import model.{HttpResponseDetails, ProtectionModel, ReadProtectionsModel}
import play.api.libs.json._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

object ProtectionService extends ProtectionService {
  override val nps: NpsConnector = NpsConnector
}

trait ProtectionService {

  val nps: NpsConnector

  def applyForProtection(nino: String, applicationRequestBody: JsObject)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponseDetails] = {
    val (ninoWithoutSuffix, lastNinoCharOpt) = NinoHelper.dropNinoSuffix(nino)
    val npsRequestBody = applicationRequestBody.deepMerge(Json.obj("nino" -> ninoWithoutSuffix))
    nps.applyForProtection(nino, npsRequestBody) map { npsResponse =>
      val transformedResponseJs = npsResponse.body.flatMap{
        json => Json.fromJson[ProtectionModel](json).map(base => base.copy(nino = base.nino + lastNinoCharOpt.getOrElse("")))
      }
      HttpResponseDetails(npsResponse.status, transformedResponseJs.map(model => Json.toJson[ProtectionModel](model).as[JsObject]))
    }
  }


  def amendProtection(nino: String,
                      protectionId: Long,
                      amendmentRequestBody: JsObject)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponseDetails] = {
    val (ninoWithoutSuffix, lastNinoCharOpt) = NinoHelper.dropNinoSuffix(nino)
    val npsRequestBody: JsObject = {
      amendmentRequestBody.deepMerge(Json.obj(
        "nino" -> ninoWithoutSuffix,
        "protection" -> Json.obj("id" -> protectionId)
      ))
    }

    nps.amendProtection(nino, protectionId, npsRequestBody) map { npsResponse =>
      val transformedResponseJs = npsResponse.body.flatMap{
        json => Json.fromJson[ProtectionModel](json).map(base => base.copy(nino = base.nino + lastNinoCharOpt.getOrElse("")))
      }
      HttpResponseDetails(npsResponse.status, transformedResponseJs.map(model => Json.toJson[ProtectionModel](model).as[JsObject]))
    }
  }

  def readExistingProtections(nino: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponseDetails] = {
    val (_, lastNinoCharOpt) = NinoHelper.dropNinoSuffix(nino)

    nps.readExistingProtections(nino) map { npsResponse =>
      val transformedResponseJs = npsResponse.body.flatMap{
        json => Json.fromJson[ReadProtectionsModel](json).map(base => base.copy(nino = base.nino + lastNinoCharOpt.getOrElse("")))
      }
      HttpResponseDetails(npsResponse.status, transformedResponseJs.map(model => Json.toJson[ReadProtectionsModel](model).as[JsObject]))
    }
  }

}
