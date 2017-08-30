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

import javax.inject.Inject

import connectors.NpsConnector
import model.{ExistingProtections, HttpResponseDetails}
import play.api.http.Status
import play.api.libs.json.{JsObject, JsResult, JsValue, Json}
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}
import util.{NinoHelper, Transformers}
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

import scala.concurrent.{ExecutionContext, Future}

object ProtectionService extends ProtectionService {
  override val nps: NpsConnector = NpsConnector
}

trait ProtectionService {

  val nps: NpsConnector

  def applyForProtection(nino: String, applicationRequestBody: JsObject)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponseDetails] = {
    val (ninoWithoutSuffix, lastNinoCharOpt) = NinoHelper.dropNinoSuffix(nino)
    val npsRequestBody: JsResult[JsObject] = Transformers.transformApplyOrAmendRequestBody(
      ninoWithoutSuffix,
      None,
      applicationRequestBody)
    npsRequestBody.fold(
      errors => Future.successful(HttpResponseDetails(Status.BAD_REQUEST, npsRequestBody)),
      req => nps.applyForProtection(nino, req) map { npsResponse =>
        val transformedResponseJs = npsResponse.body.flatMap {
          Transformers.transformApplyOrAmendResponseBody(lastNinoCharOpt.get, _)
        }
        HttpResponseDetails(npsResponse.status, transformedResponseJs)
      }
    )
  }


  def amendProtection(nino: String, protectionId: Long, amendmentRequestBody: JsObject)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponseDetails] = {
    val (ninoWithoutSuffix, lastNinoCharOpt) = NinoHelper.dropNinoSuffix(nino)
    val npsRequestBody: JsResult[JsObject] = Transformers.transformApplyOrAmendRequestBody(
      ninoWithoutSuffix,
      Some(protectionId),
      amendmentRequestBody)

    npsRequestBody.fold(
      errors => Future.successful(HttpResponseDetails(Status.BAD_REQUEST, npsRequestBody)),
      req => nps.amendProtection(nino, protectionId, req) map { npsResponse =>
        val transformedResponseJs = npsResponse.body.flatMap {
          Transformers.transformApplyOrAmendResponseBody(lastNinoCharOpt.get, _)
        }
        HttpResponseDetails(npsResponse.status, transformedResponseJs)
      }
    )
  }

  def readExistingProtections(nino: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponseDetails] = {
    val (_, lastNinoCharOpt) = NinoHelper.dropNinoSuffix(nino)

    nps.readExistingProtections(nino) map { npsResponse =>
      val transformedResponseJs = npsResponse.body.flatMap {
        Transformers.transformReadResponseBody(lastNinoCharOpt.get, _)
      }
      HttpResponseDetails(npsResponse.status, transformedResponseJs)
    }
  }

}

class NewProtectionService @Inject()(npsConnector: NpsConnector) {

  def getCurrentProtections(nino: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val ninoWithoutSuffix = nino.dropRight(1)
    npsConnector.getProtections(ninoWithoutSuffix)
  }

  def transformCurrentProtectionJson(json: JsValue): Unit = {
    //Rename type to protection type
    val jsonModifiers = (__ \ 'protectionType).json.copyFrom((__ \ 'type).json.pick)
      //Change status number to corresponding text
      //Change type number to corresponding text

    //Transform the json
    //json.transform(jsonModifiers)
  }


}

object ProtectionTypes extends Enumeration {
  val Unknown, FP2016, IP2014, IP2016, Primary, Enhanced, Fixed, FP2014 = Value
}