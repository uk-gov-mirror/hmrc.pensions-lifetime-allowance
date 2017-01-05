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

package controllers

import model.Error
import play.api.mvc._
import play.api.libs.json.{JsArray, JsNumber, JsObject, JsUndefined, JsValue, Json}
import play.api.mvc.Action
import play.mvc.Http.Response
import services.ProtectionService
import model.HttpResponseDetails
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object ReadProtectionsController extends ReadProtectionsController {
  override val protectionService = ProtectionService
  override def WithCitizenRecordCheck(nino: String) = ProtectionsActions.WithCitizenRecordCheckAction(nino)
}

trait ReadProtectionsController extends BaseController {

  def protectionService: ProtectionService

  def WithCitizenRecordCheck(nino: String): ActionBuilder[Request]

  /**
    * Return the full details of current versions of all protections held by the individual
    *
    * @param nino national insurance number of the individual
    * @return json object full details of the existing protections held fby the individual
    */
  def readExistingProtections(nino: String) : Action[AnyContent] = WithCitizenRecordCheck(nino).async { implicit request =>
    protectionService.readExistingProtections(nino) map { response =>
      response.status match {
        case OK if response.body.isSuccess => Ok(response.body.get)
        case _ => handleErrorResponse(response, includeResponseDetails = true)
      }
    }
  }

  /*
   * Returns a count of the existing protections for the individual
   * @param nino
   * @return a json object with a single field 'count' set to the number of existing protections
   */
  def readExistingProtectionsCount(nino: String) : Action[AnyContent] = Action.async { implicit request =>
    protectionService.readExistingProtections(nino) map { response =>
      response.status match {
        case OK if response.body.isSuccess => {
          val protectionsArrayJsValue = response.body.get \ "lifetimeAllowanceProtections"
          val count = protectionsArrayJsValue match {
            case protectionsJsArray: JsArray => protectionsJsArray.value.size
            case _ => 0
          }
          Ok(JsObject(Seq("count" -> JsNumber(count))))
        }
        case _ => handleErrorResponse(response)
      }
    }
  }

  private def handleErrorResponse(response: HttpResponseDetails, includeResponseDetails: Boolean = false) : Result = {
    //  unpexected error response handling
    val responseBodyDetails = if (response.body.isSuccess && includeResponseDetails) {
      ", body=" + Json.asciiStringify(response.body.get)
    } else {
      ""
    }
    val error = Json.toJson(Error("NPS request resulted in a response with: HTTP status=" + response.status + responseBodyDetails))
    response.status match {
      case OK => InternalServerError(error)
      case BAD_REQUEST => BadRequest(error)
      case INTERNAL_SERVER_ERROR => InternalServerError(error)
      case SERVICE_UNAVAILABLE => ServiceUnavailable(error)
      case UNAUTHORIZED => Unauthorized(error)
      case NOT_FOUND => NotFound(error)
      case _ => InternalServerError(error)
    }
  }
}
