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

package controllers

import auth.{AuthClientConnector, AuthorisedActions}
import connectors.CitizenDetailsConnector
import javax.inject.Inject
import model.ProtectionApplication
import play.api.mvc._
import services.ProtectionService
import play.api.libs.json._
import model.Error

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class DefaultCreateProtectionsController @Inject()(val authConnector: AuthClientConnector,
                                                   val citizenDetailsConnector: CitizenDetailsConnector,
                                                   val protectionService: ProtectionService) extends CreateProtectionsController

trait CreateProtectionsController extends NPSResponseHandler with AuthorisedActions {
  def protectionService: ProtectionService

  def applyForProtection(nino: String): Action[JsValue] = Authorised(nino).async(BodyParsers.parse.json) { implicit request =>
    val protectionApplicationJs = request.body.validate[ProtectionApplication]

    protectionApplicationJs.fold(
      errors => Future.successful(BadRequest(Json.toJson(Error(message = "body failed validation with errors: " + errors)))),
      p => protectionService.applyForProtection(nino, Json.toJson(p).as[JsObject]).map { response =>
        handleNPSSuccess(response)
      }.recover {
        case downstreamError => handleNPSError(downstreamError, "[AmendProtectionsController.amendProtection]")
      }
    )
  }
}
