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

package controllers

import auth.{AuthClientConnector, AuthorisedActions}
import connectors.CitizenDetailsConnector

import javax.inject.Inject
import model.ProtectionAmendment
import play.api.mvc._
import services.ProtectionService
import play.api.libs.json._
import model.Error
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

class AmendProtectionsController @Inject()(val authConnector: AuthClientConnector,
                                           val citizenDetailsConnector: CitizenDetailsConnector,
                                           val protectionService: ProtectionService,
                                           cc: ControllerComponents) extends BackendController(cc) with NPSResponseHandler with AuthorisedActions {

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  def amendProtection(nino: String, id: String): Action[JsValue] = Action.async(cc.parsers.json) { implicit request =>
    userAuthorised(nino) {

      Try {
        id.toLong
      }.map { protectionId =>

        request.body.validate[ProtectionAmendment].fold(
          errors =>
            Future.successful(BadRequest(Json.toJson(Error(message = "body failed validation with errors: " + errors)))),
          amendment =>
            protectionService.amendProtection(nino, protectionId, Json.toJson(amendment).as[JsObject]).map { response =>
              handleNPSSuccess(response)
            }.recover {
              case downstreamError => handleNPSError(downstreamError, "[AmendProtectionsController.amendProtection]")
            }
        )
      }.getOrElse {
        Future.successful(BadRequest(Json.toJson(Error(message = "path parameter 'id' is not an integer: " + id))))
      }
    }
  }
}