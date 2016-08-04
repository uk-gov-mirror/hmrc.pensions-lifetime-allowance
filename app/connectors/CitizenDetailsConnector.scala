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

package connectors

import util.NinoHelper
import config.WSHttp
import play.api.http.Status._

import play.api.{LoggerLike, Logger}

import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HttpResponse, _}

import scala.concurrent.{ExecutionContext, Future}

object CitizenDetailsConnector extends CitizenDetailsConnector with ServicesConfig {

  override val serviceUrl = baseUrl("citizen-details")
  override val checkRequired = getConfBool("citizen-details.checkRequired", true)
  override def http = WSHttp

}

sealed trait CitizenRecordCheckResult
case object CitizenRecordOK extends CitizenRecordCheckResult
case object CitizenRecordLocked extends CitizenRecordCheckResult
case object CitizenRecordNotFound extends CitizenRecordCheckResult
case class CitizenRecordOther4xxResponse(e: Upstream4xxResponse) extends CitizenRecordCheckResult
case class CitizenRecord5xxResponse(e: Upstream5xxResponse) extends CitizenRecordCheckResult

trait CitizenDetailsConnector {

  def http: HttpGet with HttpPost with HttpPut
  val serviceUrl: String
  val checkRequired: Boolean
  
  def getCitizenRecordCheckUrl(nino: String): String = {
    val (ninoWithoutSuffix, _) = NinoHelper.dropNinoSuffix(nino)
    serviceUrl + s"/citizen-details/${ninoWithoutSuffix}/designatory-details"
  }

  def checkCitizenRecord(nino: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[CitizenRecordCheckResult] = {
    if(!checkRequired) {
      Future.successful(CitizenRecordOK)
    } else {
      val requestUrl = getCitizenRecordCheckUrl(nino)
      http.GET[HttpResponse](requestUrl) map {
        _ => CitizenRecordOK
      } recover {
        case e: Upstream4xxResponse if (e.upstreamResponseCode == LOCKED) => CitizenRecordLocked
        case e: NotFoundException => CitizenRecordNotFound
        case e: Upstream4xxResponse => CitizenRecordOther4xxResponse(e)
        case e: Upstream5xxResponse => CitizenRecord5xxResponse(e)
      }
    }
  }
}