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

package controllers

import scala.concurrent.{ExecutionContext, Future}
import play.api.mvc._
import play.api.mvc.Results._
import uk.gov.hmrc.play.microservice.controller.BaseController
import connectors.CitizenDetailsConnector
import connectors.{CitizenRecord5xxResponse, CitizenRecordLocked, CitizenRecordNotFound, CitizenRecordOK, CitizenRecordOther4xxResponse}
import uk.gov.hmrc.play.http.HeaderCarrier



object ProtectionsActions{
  lazy val citizenDetailsConnector = CitizenDetailsConnector
}

case class WithCitizenRecordCheckAction(nino: String)(implicit ec: ExecutionContext) extends ActionBuilder[Request] {

  def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = {
    implicit val hc: HeaderCarrier = HeaderCarrier.fromHeadersAndSession(request.headers, Some(request.session))

    ProtectionsActions.citizenDetailsConnector.checkCitizenRecord(nino) flatMap { citizenCheckResult =>
      citizenCheckResult match {
        case CitizenRecordOK => block(request)
        case CitizenRecordNotFound => Future.successful(NotFound)
        case CitizenRecordLocked => Future.successful(Locked)
        case CitizenRecordOther4xxResponse(e) => Future.successful(BadRequest)
        case CitizenRecord5xxResponse(e) if e.upstreamResponseCode == 500 => Future.successful(InternalServerError)
        case CitizenRecord5xxResponse(e) if e.upstreamResponseCode == 503 => Future.successful(GatewayTimeout)
      }
    }
  }
}
