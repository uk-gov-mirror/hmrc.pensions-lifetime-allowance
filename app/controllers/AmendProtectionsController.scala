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

import model._
import play.api.mvc._
import play.api.http.Status
import services.ProtectionService
import uk.gov.hmrc.play.http.HttpResponse
import uk.gov.hmrc.play.microservice.controller.BaseController
import play.api.libs.json._

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
        errors => Future.successful(BadRequest(s"body failed validation with errors: $errors".asErrorJson)),
        p => protectionService.amendProtection(nino, protectionId, request.body.as[JsObject]) map { response =>
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
              val error = {"NPS request resulted in a response with: HTTP status=" + response.status + responseErrorDetails}.asErrorJson
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
    } getOrElse {
      Future.successful(BadRequest(s"path parameter 'id' is not an integer: $id".asErrorJson))
    }
  }
}
