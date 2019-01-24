/*
 * Copyright 2019 HM Revenue & Customs
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
import play.api.Logger
import play.api.mvc._
import uk.gov.hmrc.auth.core.{AuthorisationException, _}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import scala.concurrent.{ExecutionContext, Future}

trait AuthorisedActions extends AuthProvider with Controller with AuthorisedFunctions {
  val citizenDetailsConnector: CitizenDetailsConnector
  case class Authorised(nino: String)(implicit ec: ExecutionContext) extends ActionBuilder[Request] {

    def logErrorAndRespond(err: String, status: Status): Future[Result] = {
      Logger.warn(err)
      Future.successful(status(err))
    }

    def logErrorAndRespondFromUpstreamResponse(err: String, status: Status, upstreamError: Throwable): Future[Result] = {
      Logger.warn("Error from Citizen Details", upstreamError)
      Future.successful(status(s"$err\nResponse: ${upstreamError.getMessage}"))
    }

    def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]): Future[Result] = {
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers)
      authorised(Nino(hasNino = true, nino = Some(nino)) and ConfidenceLevel.L200) {
        citizenDetailsConnector.checkCitizenRecord(nino) flatMap {
          case CitizenRecordOK => block(request)
          case CitizenRecordNotFound => logErrorAndRespond(s"Citizen Record Check: Not Found for '$nino'", NotFound)
          case CitizenRecordLocked => logErrorAndRespond(s"Citizen Record Check: Locked for '$nino'", Locked)
          case CitizenRecordOther4xxResponse(e) =>
            logErrorAndRespondFromUpstreamResponse(s"Citizen Record Check: ${e.upstreamResponseCode} response for '$nino'", BadRequest, e)
          case CitizenRecord5xxResponse(e) if e.upstreamResponseCode == 503 =>
            logErrorAndRespondFromUpstreamResponse(s"Citizen Record Check: Upstream 503 response for '$nino'", GatewayTimeout, e)
          case CitizenRecord5xxResponse(e) =>
            logErrorAndRespondFromUpstreamResponse(s"Citizen Record Check: Upstream ${e.upstreamResponseCode} response for '$nino'", InternalServerError, e)
          case _ => logErrorAndRespond("err", InternalServerError)
        }
      }.recover(authErrorHandling[A](request))
    }

    def authErrorHandling[A](implicit request: Request[A]): PartialFunction[Throwable, Result] = {
      case e: NoActiveSession => {
        Logger.error("User has no active session", e)
        Unauthorized("User has no active session")
      }
      case e: AuthorisationException => {
        Logger.error("User forbidden", e)
        Forbidden("User forbidden")
      }
    }
  }
}
