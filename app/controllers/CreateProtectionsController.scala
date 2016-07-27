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

import model.ProtectionApplication
import play.api.mvc._
import play.api.http.Status
import services.ProtectionService
import uk.gov.hmrc.play.http.HttpResponse
import uk.gov.hmrc.play.microservice.controller.BaseController
import play.api.libs.json._
import model.Error
import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global

object CreateProtectionsController extends CreateProtectionsController {
  override val protectionService = ProtectionService
  override def WithCitizenRecordCheck(nino: String)= WithCitizenRecordCheckAction(nino)
}

trait CreateProtectionsController extends BaseController {

  def protectionService: ProtectionService
  def WithCitizenRecordCheck(nino:String): ActionBuilder[Request]

  def applyForProtection(nino: String) = WithCitizenRecordCheck(nino).async(BodyParsers.parse.json) { implicit request =>
    val protectionApplicationJs = request.body.validate[ProtectionApplication]

    protectionApplicationJs.fold(
      errors => Future.successful(BadRequest(Json.toJson(Error(message = "body failed validation with errors: " + errors)))),
      p => protectionService.applyForProtection(nino, request.body.as[JsObject]) map { response =>
        response.status match {
          case OK if response.body.isSuccess => Ok(response.body.get)
          case CONFLICT if response.body.isSuccess => Conflict(response.body.get) // this is a normal/expected response
          case _ => {
            //  error response handling
            val responseErrorDetails = if (!response.body.isSuccess) {
              ", but unable to parse the NPS response body"
            } else {
              ", body=" + Json.asciiStringify(response.body.get)
            }
            val error = Json.toJson(Error("NPS request resulted in a response with: HTTP status=" + response.status + responseErrorDetails))
            response.status match {
              case OK => InternalServerError(error)
              case BAD_REQUEST => BadRequest(error)
              case INTERNAL_SERVER_ERROR => InternalServerError(error)
              case SERVICE_UNAVAILABLE => ServiceUnavailable(error)
              case UNAUTHORIZED => Unauthorized(error)
              case _ => InternalServerError(error)
            }
          }
        }
      }
    )
  }
}
