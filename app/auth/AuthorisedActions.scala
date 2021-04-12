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

package auth

import connectors._
import play.api.Logging
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.auth.core.{AuthorisationException, _}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

trait AuthorisedActions extends AuthProvider with AuthorisedFunctions with Logging {

  def userAuthorised(nino: String)(body: => Future[Result])(implicit request: Request[_], ec: ExecutionContext): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)
    //implicit val hc: HeaderCarrier =  HeaderCarrierConverter.fromRequest(request.headers)
    authorised(Nino(hasNino = true, nino = Some(nino)) and ConfidenceLevel.L200) {
      citizenDetailsConnector.checkCitizenRecord(nino) flatMap {
        case CitizenRecordOK => body
        case CitizenRecordNotFound => logErrorAndRespond(s"Citizen Record Check: Not Found for '$nino'", NotFound)
        case CitizenRecordLocked => logErrorAndRespond(s"Citizen Record Check: Locked for '$nino'", Locked)
        case CitizenRecordOther4xxResponse(e) =>
          logErrorAndRespondFromUpstreamResponse(s"Citizen Record Check: ${e.statusCode} response for '$nino'", BadRequest, e)
        case CitizenRecord5xxResponse(e) if e.statusCode == 503 =>
          logErrorAndRespondFromUpstreamResponse(s"Citizen Record Check: Upstream 503 response for '$nino'", GatewayTimeout, e)
        case CitizenRecord5xxResponse(e) =>
          logErrorAndRespondFromUpstreamResponse(s"Citizen Record Check: Upstream ${e.statusCode} response for '$nino'", InternalServerError, e)
        case _ => logErrorAndRespond("err", InternalServerError)
      }
    }.recover(authErrorHandling)
  }

  def logErrorAndRespond(err: String, status: Status): Future[Result] = {
    logger.warn(err)
    Future.successful(status(err))
  }

  def logErrorAndRespondFromUpstreamResponse(err: String, status: Status, upstreamError: Throwable): Future[Result] = {
    logger.warn("Error from Citizen Details", upstreamError)
    Future.successful(status(s"$err\nResponse: ${upstreamError.getMessage}"))
  }

  def authErrorHandling: PartialFunction[Throwable, Result] = {
    case e: NoActiveSession => {
      logger.error("User has no active session", e)
      Unauthorized("User has no active session")
    }
    case e: AuthorisationException => {
      logger.error("User forbidden", e)
      Forbidden("User forbidden")
    }
  }

  val citizenDetailsConnector: CitizenDetailsConnector

}
