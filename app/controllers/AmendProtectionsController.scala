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

import model.ProtectionAmendment
import play.api.mvc._
import play.api.http.Status
import services.ProtectionService
import uk.gov.hmrc.play.http.{BadRequestException, HttpResponse, Upstream4xxResponse, Upstream5xxResponse}
import uk.gov.hmrc.play.microservice.controller.BaseController
import play.api.libs.json._
import model.Error
import play.api.Logger

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object AmendProtectionsController extends AmendProtectionsController {
  override val protectionService = ProtectionService
  override def WithCitizenRecordCheck(nino: String)= ProtectionsActions.WithCitizenRecordCheckAction(nino)
}

trait AmendProtectionsController extends BaseController {

  def protectionService: ProtectionService
  def WithCitizenRecordCheck(nino:String): ActionBuilder[Request]

  def amendProtection(nino: String, id: String) = WithCitizenRecordCheck(nino).async(BodyParsers.parse.json) { implicit request =>

    val protectionIdOpt: Option[Long] = try {
      Some(id.toLong)
    } catch {
      case ex: NumberFormatException => None
    }

    protectionIdOpt map { protectionId =>

      val protectionAmendmentJs = request.body.validate[ProtectionAmendment]

      protectionAmendmentJs.fold(
        errors => Future.successful(BadRequest(Json.toJson(Error(message = "body failed validation with errors: " + errors)))),
        p => protectionService.amendProtection(nino, protectionId, request.body.as[JsObject]).map { response =>
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
              InternalServerError(error)
            }
          }
        }.recover {
          case Upstream5xxResponse(errorDetails, SERVICE_UNAVAILABLE, _) =>
            Logger.error(s"[AmendProtectionsController.amendProtection] $errorDetails")
            ServiceUnavailable(errorDetails)
          case Upstream5xxResponse(errorDetails, _, _) =>
            Logger.error(s"[AmendProtectionsController.amendProtection] $errorDetails")
            InternalServerError(errorDetails)
          case Upstream4xxResponse(errorDetails, UNAUTHORIZED, _, _) =>
            Logger.error(s"[AmendProtectionsController.amendProtection] $errorDetails")
            Unauthorized(errorDetails)
          case Upstream4xxResponse(errorDetails,_,_,_) =>
            Logger.error(s"[AmendProtectionsController.amendProtection] $errorDetails")
            InternalServerError(errorDetails)
          case badRequest: BadRequestException =>
            Logger.error(s"[AmendProtectionsController.amendProtection] ${badRequest.getMessage}", badRequest)
            BadRequest(badRequest.getMessage)

        }
      )
    } getOrElse {
      Future.successful(BadRequest(Json.toJson(Error(message = "path parameter 'id' is not an integer: " + id))))
    }
  }
}
