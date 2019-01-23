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
import model.Error
import play.api.Logger
import play.api.libs.json._
import play.api.mvc.{Action, _}
import services.ProtectionService
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global

class DefaultReadProtectionsController @Inject()(val authConnector: AuthClientConnector,
                                                 val citizenDetailsConnector: CitizenDetailsConnector,
                                                 val protectionService: ProtectionService) extends ReadProtectionsController

trait ReadProtectionsController extends BaseController with AuthorisedActions with NPSResponseHandler {
  def protectionService: ProtectionService

  /**
    * Return the full details of current versions of all protections held by the individual
    *
    * @param nino national insurance number of the individual
    * @return json object full details of the existing protections held fby the individual
    */
  def readExistingProtections(nino: String): Action[AnyContent] = Authorised(nino).async { implicit request =>
    protectionService.readExistingProtections(nino).map { response =>
      response.status match {
        case OK if response.body.isSuccess => Ok(response.body.get)
        case _ =>
          val error = Json.toJson(Error(s"NPS request resulted in a response with: HTTP status = ${response.status} body = ${response.body}"))
          Logger.error(error.toString)
          InternalServerError(error)
      }
    }.recover {
      case error => handleNPSError(error, "[ReadProtectionsController.readExistingProtections]")
    }
  }

  /*
   * Returns a count of the existing protections for the individual
   * @param nino
   * @return a json object with a single field 'count' set to the number of existing protections
   */
  def readExistingProtectionsCount(nino: String): Action[AnyContent] = Action.async { implicit request =>
    protectionService.readExistingProtections(nino).map { response =>
      response.status match {
        case OK if response.body.isSuccess => {
          val protectionsArrayJsValue = response.body.get \ "lifetimeAllowanceProtections" getOrElse JsNumber(0)
          val count = protectionsArrayJsValue match {
            case protectionsJsArray: JsArray => protectionsJsArray.value.size
            case _ => 0
          }
          Ok(JsObject(Seq("count" -> JsNumber(count))))
        }
        case _ =>
          val error = Json.toJson(Error(s"NPS request resulted in a response with: HTTP status = ${response.status} body = ${response.body}"))
          Logger.error(error.toString)
          InternalServerError(error)
      }
    }.recover {
      case error => handleNPSError(error, "[ReadProtectionsController.readExistingProtectionsCount]")
    }
  }
}
