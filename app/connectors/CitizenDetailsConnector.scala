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

package connectors

import javax.inject.Inject
import play.api.Mode
import play.api.http.Status._
import play.api.{Configuration, Environment}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient

import scala.concurrent.{ExecutionContext, Future}

class DefaultCitizenDetailsConnector @Inject()(val http: DefaultHttpClient,
                                               environment: Environment,
                                               val runModeConfiguration: Configuration,
                                               servicesConfig: ServicesConfig)
  extends CitizenDetailsConnector {

  override lazy val serviceUrl: String = servicesConfig.baseUrl("citizen-details")
  override lazy val checkRequired: Boolean = servicesConfig.getConfBool("citizen-details.checkRequired", defBool = true)

  val mode: Mode = environment.mode
}

sealed trait CitizenRecordCheckResult
case object CitizenRecordOK extends CitizenRecordCheckResult
case object CitizenRecordLocked extends CitizenRecordCheckResult
case object CitizenRecordNotFound extends CitizenRecordCheckResult
case class CitizenRecordOther4xxResponse(e: UpstreamErrorResponse) extends CitizenRecordCheckResult
case class CitizenRecord5xxResponse(e: UpstreamErrorResponse) extends CitizenRecordCheckResult

trait CitizenDetailsConnector {
  def http: DefaultHttpClient
  val serviceUrl: String
  val checkRequired: Boolean
  
  def getCitizenRecordCheckUrl(nino: String): String = {
    serviceUrl + s"/citizen-details/$nino/designatory-details"
  }

  def checkCitizenRecord(nino: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[CitizenRecordCheckResult] = {
    if(!checkRequired) {
      Future.successful(CitizenRecordOK)
    } else {
      val requestUrl = getCitizenRecordCheckUrl(nino)
      http.GET[HttpResponse](requestUrl) map {
        _ => CitizenRecordOK
      } recover {
        case e: UpstreamErrorResponse if e.statusCode == LOCKED => CitizenRecordLocked
        case e: NotFoundException => CitizenRecordNotFound
        case e: UpstreamErrorResponse
          if e.statusCode >= BAD_REQUEST && e.statusCode < INTERNAL_SERVER_ERROR  => {
          CitizenRecordOther4xxResponse(e)
        }
        case e: BadRequestException => CitizenRecordOther4xxResponse(UpstreamErrorResponse(e.message, e.responseCode))
        case e: UpstreamErrorResponse
          if e.statusCode >= INTERNAL_SERVER_ERROR  => {
          CitizenRecord5xxResponse(e)
        }
      }
    }
  }
}
